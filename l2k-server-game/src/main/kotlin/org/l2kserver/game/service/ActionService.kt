package org.l2kserver.game.service

import kotlinx.coroutines.isActive
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.handler.dto.request.ActionRequest
import org.l2kserver.game.handler.dto.request.AttackRequest
import org.l2kserver.game.handler.dto.request.BasicAction
import org.l2kserver.game.handler.dto.request.BasicActionRequest
import org.l2kserver.game.handler.dto.request.SocialActionRequest
import org.l2kserver.game.handler.dto.response.ActionFailedResponse
import org.l2kserver.game.handler.dto.response.CancelActionResponse
import org.l2kserver.game.handler.dto.response.ChangeMoveTypeResponse
import org.l2kserver.game.handler.dto.response.SetTargetResponse
import org.l2kserver.game.handler.dto.response.ShowMapResponse
import org.l2kserver.game.handler.dto.response.SocialActionResponse
import org.l2kserver.game.handler.dto.response.StatusAttribute
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.model.actor.Actor
import org.l2kserver.game.model.actor.Npc
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.actor.ScatteredItem
import org.l2kserver.game.model.actor.MoveType
import org.l2kserver.game.model.actor.Posture
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sessionContext
import org.l2kserver.game.repository.GameObjectRepository
import org.springframework.stereotype.Service
import kotlin.math.roundToInt

const val INTERACTION_DISTANCE = 40

/**
 * Handles player's actions, like attacking, setting target, switching sit and stand...
 */
@Service
class ActionService(
    private val combatService: CombatService,
    private val npcService: NpcService,
    private val itemService: ItemService,
    private val tradeService: TradeService,
    private val asyncTaskService: AsyncTaskService,
    private val moveService: MoveService,

    override val gameObjectRepository: GameObjectRepository
): AbstractService() {
    override val log = logger()

    /**
     * Handles request to attack
     */
    suspend fun attackTarget(attackRequest: AttackRequest) {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        val target = gameObjectRepository.findActorById(attackRequest.targetId)

        if (character.targetId != target.id) character.setTarget(target)
        else combatService.launchAttack(character, target)
    }

    /**
     * Handles left-click on some game object
     */
    suspend fun performAction(request: ActionRequest) {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        log.debug("Player '{}' left-clicked target with id='{}'", character, request.targetId)

        //Second click to the player's own character
        if (character.targetId == request.targetId && character.targetId == character.id)
            return

        val target = gameObjectRepository.findByIdOrNull(request.targetId)
        when {
            target is ScatteredItem -> itemService.launchPickUp(character, target)
            target is Actor && target.id != character.targetId -> character.setTarget(target)
            target is Npc && target.isEnemyOf(character) -> combatService.launchAttack(character, target)
            target is Npc || target is PlayerCharacter && target.privateStore != null -> character.interactWith(target)
            target is PlayerCharacter && target.isEnemyOf(character) -> combatService.launchAttack(character, target)
            target is PlayerCharacter -> { send(ActionFailedResponse) } //TODO https://github.com/l2kserver/l2kserver-game/issues/25
            target == null -> {
                log.warn("Character '{}' tries to set target to non-existent target with id = '{}'", character, request.targetId)
                send(ActionFailedResponse)
            }
        }
    }

    /**
     * Cancels casting if character is casting or cancels target if character targets something
     */
    suspend fun cancelAction() {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())

        //TODO Cancel casting if character is casting

        character.targetId?.let {
            gameObjectRepository.findActorByIdOrNull(it)?.targetedBy?.remove(character.id)
        }

        character.targetId = null
        broadcastPacket(CancelActionResponse(character.id, character.position), character.position)
    }

    /**
     * Handles request to perform some basic action - switching sit/stand, walk/run, summon actions...
     */
    suspend fun performBasicAction(request: BasicActionRequest) {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())

        when (request.action) {
            BasicAction.TOGGLE_SIT_STAND -> {
                log.debug("Got request to toggle sit/stand")
                if (character.privateStore != null) return
                if (character.posture == Posture.STANDING) character.sitDown()
                else character.standUp()
            }
            BasicAction.TOGGLE_WALK_RUN -> newSuspendedTransaction {
                log.debug("Got request to toggle walk/run")

                if (character.moveType == MoveType.RUN) character.moveType = MoveType.WALK
                else character.moveType = MoveType.RUN

                broadcastPacket(ChangeMoveTypeResponse(character.id, character.moveType), character.position)
            }
            BasicAction.GENERAL_MANUFACTURE -> tradeService.startGeneralPrivateManufacture()
        }
    }

    /**
     * Handles request to perform some social action - laugh, greetings, dancing, etc.
     */
    suspend fun performSocialAction(request: SocialActionRequest) {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())

        if (character.isParalyzed) {
            send(ActionFailedResponse)
            return
        }

        broadcastPacket(SocialActionResponse(character.id, request.socialAction), character.position)
    }

    /**
     * Handles request to show map
     */
    suspend fun showMap() = send(ShowMapResponse)

    /**
     * Moves PlayerCharacter enough close to [target] and starts interaction with it
     */
    private suspend fun PlayerCharacter.interactWith(target: Actor) = asyncTaskService.launchAction(this.id) {
        val character = this@interactWith
        val requiredDistance = INTERACTION_DISTANCE +
                (this@interactWith.collisionBox.radius + target.collisionBox.radius).roundToInt()

        moveService.move(character, target, requiredDistance)

        val enoughCloseToInteract = this@interactWith.position.isCloseTo(
            other = target.position,
            distance = (character.collisionBox.radius + target.collisionBox.radius).roundToInt() + INTERACTION_DISTANCE
        )
        if (!coroutineContext.isActive || !enoughCloseToInteract) return@launchAction

        when (target) {
            is Npc -> npcService.talkTo(target)
            is PlayerCharacter -> tradeService.showPrivateStoreOf(target)
        }
    }

    /**
     * Set character's target to [targeted] and sends information about it
     */
    private suspend fun PlayerCharacter.setTarget(targeted: Actor) {
        this.targetId = targeted.id
        targeted.targetedBy.add(this.id)

        when (targeted) {
            is PlayerCharacter -> send(SetTargetResponse(targeted.id))
            is Npc -> {
                send(SetTargetResponse(targeted.id, this.level - targeted.level))
                send(
                    UpdateStatusResponse(
                        targeted.id,
                        StatusAttribute.CUR_HP to targeted.currentHp,
                        StatusAttribute.MAX_HP to targeted.stats.maxHp
                    )
                )
            }
        }
    }
}
