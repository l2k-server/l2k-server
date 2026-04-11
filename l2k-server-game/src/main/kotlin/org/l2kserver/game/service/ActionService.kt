package org.l2kserver.game.service

import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.extensions.model.actor.isAttacking
import org.l2kserver.game.extensions.model.actor.isInteractableBy
import org.l2kserver.game.handler.dto.request.ActionRequest
import org.l2kserver.game.handler.dto.request.AttackRequest
import org.l2kserver.game.handler.dto.request.BasicAction
import org.l2kserver.game.handler.dto.request.BasicActionRequest
import org.l2kserver.game.handler.dto.request.SocialActionRequest
import org.l2kserver.game.handler.dto.response.ActionFailedResponse
import org.l2kserver.game.handler.dto.response.CancelCastingResponse
import org.l2kserver.game.handler.dto.response.CancelTargetResponse
import org.l2kserver.game.handler.dto.response.ChangeMoveTypeResponse
import org.l2kserver.game.handler.dto.response.SetTargetResponse
import org.l2kserver.game.handler.dto.response.ShowMapResponse
import org.l2kserver.game.handler.dto.response.SocialActionResponse
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.model.actor.Intention
import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.NpcInstanceImpl
import org.l2kserver.game.model.actor.PlayerCharacterInstanceImpl
import org.l2kserver.game.model.actor.ScatteredItem
import org.l2kserver.game.model.actor.MoveType
import org.l2kserver.game.model.actor.MutableActorInstance
import org.l2kserver.game.model.actor.Posture
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sessionContext
import org.l2kserver.game.repository.GameObjectRepository
import org.springframework.stereotype.Service
import kotlin.math.roundToInt

const val INTERACTION_DISTANCE = 40

/** Handles player's actions, like attacking, setting target, switching sit and stand... */
@Service
class ActionService(
    private val npcService: NpcService,
    private val tradeService: TradeService,
    private val asyncTaskService: AsyncTaskService,

    override val gameObjectRepository: GameObjectRepository
) : AbstractService() {
    override val log = logger()

    /** Handles request to attack */
    suspend fun attackTarget(attackRequest: AttackRequest) {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        val target = gameObjectRepository.findActorById(attackRequest.targetId)

        log.debug("Player {} requested to attack {}", character, target)

        if (character.targetId != target.id) character.setTarget(target)
        else enqueueAttack(character, target)
    }

    /** Handles left-click on some game object */
    suspend fun performAction(request: ActionRequest) {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        log.debug("Player '{}' left-clicked target with id='{}'", character, request.targetId)

        //Second click to the player's own character
        if (character.targetId == request.targetId && character.targetId == character.id)
            return

        when (val target = gameObjectRepository.findByIdOrNull(request.targetId)) {
            null -> {
                log.warn("'{}' tries to interact with non-existent object '{}'", character, request.targetId)
                send { ActionFailedResponse }
            }
            is ScatteredItem -> character.intentionQueue.enqueue(
                Intention.Move(target),
                Intention.PickUp(target)
            )
            is MutableActorInstance -> when {
                target.id != character.targetId -> character.setTarget(target)
                target.isEnemyOf(character) -> enqueueAttack(character, target)
                target.isInteractableBy(character) -> character.intentionQueue.enqueue(
                    Intention.Move(target, requiredDistance = INTERACTION_DISTANCE),
                    Intention.Interact(target)
                )
                target is PlayerCharacterInstanceImpl -> send { ActionFailedResponse } //TODO https://github.com/l2k-server/l2k-server/issues/21
            }
        }
    }

    /** Cancels casting if character is casting or cancels target if character targets something */
    suspend fun cancelAction() {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        log.debug("Player '{}' cancelled current action", character)

        // Cancel casting if character casts
        if (character.intentionQueue.current is Intention.Cast) {
            character.intentionQueue.cancel()
            broadcastAround(character.position) { CancelCastingResponse(character.id) }
            send { ActionFailedResponse }
        }
        // Otherwise cancel his target
        else character.targetId?.let {
            gameObjectRepository.findActorByIdOrNull(it)?.targetedBy?.remove(character)
            character.targetId = null
            broadcastAround(character.position) { CancelTargetResponse(character) }
        }
    }

    /** Handles request to perform some basic action - switching sit/stand, walk/run, summon actions... */
    suspend fun performBasicAction(request: BasicActionRequest) {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        asyncTaskService.cancelActionByActorId(character.id)

        when (request.action) {
            BasicAction.TOGGLE_SIT_STAND -> {
                log.debug("Got request to toggle sit/stand")
                if (character.privateStore != null) return
                if (character.posture == Posture.STANDING) character.sitDown()
                else character.standUp()
            }

            BasicAction.TOGGLE_WALK_RUN -> suspendTransaction {
                log.debug("Got request to toggle walk/run")

                if (character.moveType == MoveType.RUN) character.moveType = MoveType.WALK
                else character.moveType = MoveType.RUN

                broadcastAround(character.position) {
                    ChangeMoveTypeResponse(character.id, character.moveType)
                }
            }

            BasicAction.GENERAL_MANUFACTURE -> tradeService.startGeneralPrivateManufacture()
        }
    }

    /** Handles request to perform some social action - laugh, greetings, dancing, etc. */
    suspend fun performSocialAction(request: SocialActionRequest) {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())

        if (character.isParalyzed) {
            send { ActionFailedResponse }
            return
        }
        asyncTaskService.cancelActionByActorId(character.id)

        broadcastAround(character.position) { SocialActionResponse(character.id, request.socialAction) }
    }

    /** Handles request to show map */
    suspend fun showMap() = send { ShowMapResponse }

    /** Moves PlayerCharacter enough close to [target] and starts interaction with it */
    suspend fun interact(character: PlayerCharacterInstanceImpl, target: ActorInstance) {
        val enoughCloseToInteract = character.position.isCloseTo(
            other = target.position,
            distance = (character.collisionBox.radius + target.collisionBox.radius).roundToInt() + INTERACTION_DISTANCE
        )

        if (!enoughCloseToInteract) character.intentionQueue.enqueue(
            Intention.Move(target, requiredDistance = INTERACTION_DISTANCE),
            Intention.Interact(target)
        )

        when (target) {
            is NpcInstanceImpl -> npcService.talkTo(target)
            is PlayerCharacterInstanceImpl -> tradeService.showPrivateStoreOf(target)
        }
    }

    private suspend fun enqueueAttack(attacker: MutableActorInstance, target: MutableActorInstance) {
        if (attacker.isAttacking(target)) {
            log.debug("{} is already attacking {}", attacker, target)
            return
        }
        attacker.intentionQueue.enqueue(
            Intention.Move(target, requiredDistance = attacker.stats.attackRange),
            Intention.Attack(target)
        )
    }

    /** Set character's target to [targeted] and sends information about it */
    private suspend fun PlayerCharacterInstanceImpl.setTarget(targeted: MutableActorInstance) {
        this.targetId = targeted.id
        targeted.targetedBy.add(this)

        when (targeted) {
            is PlayerCharacterInstanceImpl -> send { SetTargetResponse(targeted.id) }
            is NpcInstanceImpl -> {
                send { SetTargetResponse(targeted.id, this.level - targeted.level) }
                send { UpdateStatusResponse.hpOf(targeted) }
            }
        }
    }

}
