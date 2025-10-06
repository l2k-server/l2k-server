package org.l2kserver.game.service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.handler.dto.request.MoveRequest
import org.l2kserver.game.handler.dto.request.StartRotationRequest
import org.l2kserver.game.handler.dto.request.StopRotationRequest
import org.l2kserver.game.handler.dto.request.ValidatePositionRequest
import org.l2kserver.game.handler.dto.response.ActionFailedResponse
import org.l2kserver.game.handler.dto.response.ArrivedResponse
import org.l2kserver.game.handler.dto.response.StartMovingResponse
import org.l2kserver.game.handler.dto.response.StartMovingToAttackResponse
import org.l2kserver.game.handler.dto.response.StartRotationResponse
import org.l2kserver.game.handler.dto.response.StopRotationResponse
import org.l2kserver.game.handler.dto.response.TeleportResponse
import org.l2kserver.game.handler.dto.response.ValidatePositionResponse
import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.CollisionBox
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.GameWorldObject
import org.l2kserver.game.model.actor.MoveType
import org.l2kserver.game.model.actor.MutableActorInstance
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sendTo
import org.l2kserver.game.network.session.sessionContext
import org.l2kserver.game.repository.GameObjectRepository
import org.l2kserver.game.model.time.GameTime
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext
import kotlin.math.hypot

private const val ROTATE_SPEED_PER_SEC = 65536

/** Fake GameWorldObject, representing moving destination point */
private data class DestinationPoint(
    override var position: Position
) : GameWorldObject {
    override val id = 0
    override val collisionBox = CollisionBox()
}

/** Service to handle actors moving */
@Service
class MoveService(
    private val geoDataService: GeoDataService,
    private val asyncTaskService: AsyncTaskService,
    override val gameObjectRepository: GameObjectRepository
) : AbstractService() {

    override val log = logger()

    //Key - moving actor, value - move target
    private val movingCharacters = ConcurrentHashMap<ActorInstance, DestinationPoint>()

    /** Handle request to move character to some destination point */
    suspend fun moveCharacter(request: MoveRequest) {
        val context = sessionContext()
        val character = gameObjectRepository.findCharacterById(context.getCharacterId())

        log.debug(
            "Player '{}' is trying to move character '{}' to position '{}' {}",
            context.getAccountName(),
            character.name,
            request.targetPosition,
            if (request.byMouse) "by mouse" else "by arrow keys"
        )

        var destination = movingCharacters[character]
        if (destination != null) destination.position = request.targetPosition
        else {
            destination = DestinationPoint(request.targetPosition)
            movingCharacters[character] = destination
            asyncTaskService
                .launchAction(character.id) { move(character, destination) }
                .invokeOnCompletion { movingCharacters.remove(character) }
        }
    }

    /**
     * Handles position validation request.
     * If character position at client side differs from value at the client side insignificantly -
     * modifies character position at server side, otherwise - sends ValidatePositionResponse with actual position
     */
    suspend fun validatePosition(request: ValidatePositionRequest) = newSuspendedTransaction {
        val characterId = sessionContext().getCharacterIdOrNull() ?: run {
            log.warn("Player '{}' has not selected character", sessionContext().getAccountNameOrNull())
            return@newSuspendedTransaction
        }

        val character = gameObjectRepository.findCharacterById(characterId)

        if (character.position.isCloseTo(request.position)) {
            log.trace("Difference is too small, modifying position at server side")
            character.position = request.position

            //CRUTCH If this response is not sent - character becomes stuck
            send { ActionFailedResponse }
        } else {
            log.trace("Difference is too big, modifying position at client side")
            send { ValidatePositionResponse(character.id, character.position, character.heading) }
        }
    }

    suspend fun startRotation(request: StartRotationRequest) {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        broadcastAround(character.position) {
            StartRotationResponse(character.id, request.currentHeading, request.rotationDirection)
        }
    }

    suspend fun stopRotation(request: StopRotationRequest) {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        character.heading = request.heading
        broadcastAround(character.position) {
            StopRotationResponse(character.id, character.heading)
        }
    }

    /**
     * Moves [actor] to [position]
     *
     * This suspending function is `cancellable`
     */
    suspend fun move(actor: MutableActorInstance, position: Position) {
        move(actor, DestinationPoint(position))
    }

    /** Moves [actor] to [target] by specified [requiredDistance] */
    suspend fun move(
        actor: MutableActorInstance, target: GameWorldObject, requiredDistance: Int = 0
    ) = newSuspendedTransaction {
        //Actor should turn to target anyway
        var turningJob = launchTurning(actor, target.position)

        //If actor is already at destination point - no need to do anything else
        if (actor.position.isCloseTo(target.position, requiredDistance)) {
            send { StartMovingToAttackResponse(actor, target.id, requiredDistance) }
            return@newSuspendedTransaction
        }

        log.trace("Start moving actor '{}' to target '{}'", actor, target)

        try {
            var moveTimestamp = System.currentTimeMillis()
            var previousTargetPosition: Position? = null
            var destination: Position? = null

            var moveIterations = 0
            while (coroutineContext.isActive && target.exists() && !actor.position.isCloseTo(destination)) {
                val startUpdatingPositionTimestamp = System.currentTimeMillis()

                if (actor.isImmobilized) {
                    log.trace("Actor '{}' is immobilized", actor)
                    return@newSuspendedTransaction
                }

                //If target position changed, destination must be recalculated
                if (previousTargetPosition != target.position) {
                    previousTargetPosition = target.position
                    destination = geoDataService.getAvailableTargetPosition(
                        startPosition = actor.position,
                        targetPosition = actor.position.positionBetween(target.position, requiredDistance)
                    )

                    broadcastAround(actor.position) { StartMovingResponse(actor, target.position) }
                    send { StartMovingToAttackResponse(actor, target.id, requiredDistance) }
                    turningJob.cancelAndJoin()
                    turningJob = launchTurning(actor, target.position)
                }

                //Time since last position update
                val moveTime = System.currentTimeMillis() - moveTimestamp

                //Moving must be walking for the first 0.5 second (5 move iterations)
                val moveSpeed = if (moveIterations++ < 5 || actor.moveType == MoveType.WALK)
                    actor.stats.walkSpeed
                else actor.stats.speed

                //The path traveled since the last update
                val moveDistance = moveSpeed.toDouble() / 1000 * moveTime

                //Update objects only each 10 move iterations (~1 second) - it is very heavy operation
                val updateObjects = (moveIterations % 10) == 0

                updatePosition(actor,destination!!, moveDistance, updateObjects)
                moveTimestamp = System.currentTimeMillis()

                //Sleep for 1 tick minus time of updating operation
                delay(GameTime.MILLIS_IN_TICK - (System.currentTimeMillis() - startUpdatingPositionTimestamp))
            }
            turningJob.join()
            log.trace("Actor '{}' has arrived to target '{}' on distance '{}'", actor, target, requiredDistance)
        } catch (e: CancellationException) {
            log.trace("MoveToTarget job was cancelled for reason: {}", e.message)
        } catch (e: Exception) {
            log.error("An error occurred while trying to update position of character '{}'", actor, e)
        } finally {
            actor.isMoving = false
            updateObjectsAround(actor)
            broadcastAround(actor.position) {
                ArrivedResponse(actor.id, actor.position, actor.heading)
            }
        }
    }

    /**
     * Launches coroutine, that turns [actor] to [targetPosition]
     */
    // Turning must be async because character turns and moves/attacks simultaneously
    // Client shows turning by itself, so there is no need to send some responses here
    suspend fun launchTurning(actor: MutableActorInstance, targetPosition: Position) =
        CoroutineScope(coroutineContext).launch {
            log.trace("Started turning actor '{}' to target position '{}'", actor, targetPosition)
            val newHeading = actor.position.headingTo(targetPosition)

            while (isActive && actor.heading != newHeading) {
                newSuspendedTransaction {
                    val deltaHeading = (newHeading - actor.heading).toShort().toInt()

                    val rotation = if (deltaHeading > 0)
                        minOf((ROTATE_SPEED_PER_SEC / 1000 * GameTime.MILLIS_IN_TICK).toInt(), deltaHeading)
                    else maxOf((-ROTATE_SPEED_PER_SEC / 1000 * GameTime.MILLIS_IN_TICK).toInt(), deltaHeading)

                    actor.heading += rotation
                }

                delay(GameTime.MILLIS_IN_TICK)
            }
            log.trace("Successfully turned actor '{}' to target position '{}'", actor, targetPosition)
        }

    /**
     * Teleports [actor] to [targetPosition]
     */
    suspend fun teleport(actor: MutableActorInstance, targetPosition: Position) = newSuspendedTransaction {
        log.debug("Teleporting '{}' to '{}'", actor, targetPosition)
        asyncTaskService.cancelActionByActorId(actor.id)

        val fixedPosition = targetPosition.copy(
            z = geoDataService.getNearestZ(targetPosition.x, targetPosition.y, targetPosition.z)
        )

        //TODO Checks if player can teleport ???
        sendTo(actor.id) { TeleportResponse(actor.id, fixedPosition) }

        // Imitate teleporting process. Client validates position after disappearance animation ends,
        // so it will break if position will change immediately
        delay(1000)

        newSuspendedTransaction { actor.position = fixedPosition }
        updateObjectsAround(actor)
    }

    /**
     * Updates [actor]'s position after moving for [moveDistance]
     *
     * @param actor Character, that moves
     * @param destination Destination position
     * @param moveDistance Path traveled since last update
     *
     * @return True if actor arrived to position, false - if not
     */
    private suspend fun updatePosition(
        actor: MutableActorInstance, destination: Position, moveDistance: Double, updateObjects: Boolean
    ) {
        val deltaX = actor.position.deltaX(destination).toDouble()
        val deltaY = actor.position.deltaY(destination).toDouble()

        val distanceXY = hypot(deltaX, deltaY)
        val sin = deltaY / distanceXY
        val cos = deltaX / distanceXY

        // minOf prevents jumping around destination point if speed is too big
        // and moving is greater than way to go
        val newX = (minOf(moveDistance, distanceXY) * cos).toInt() + actor.position.x
        val newY = (minOf(moveDistance, distanceXY) * sin).toInt() + actor.position.y
        val newZ = geoDataService.getNearestZ(newX, newY, actor.position.z)

        val newPosition = actor.position.copy(x = newX, y = newY, z = newZ)

        actor.position = newPosition
        log.trace("Updated position of actor '{}' to '{}'", actor, newPosition)

        if (updateObjects) updateObjectsAround(actor, destination)

        //CRUTCH
        // If Z becomes lower too fast, send ValidatePositionResponse with actual position,
        // to prevent falling under the textures on client side
        if (actor is PlayerCharacter && actor.position.deltaZ(newPosition) < -Position.ACCEPTABLE_DELTA) {
            send { ValidatePositionResponse(actor.id, newPosition, actor.heading) }
        }
    }

    private fun GameWorldObject.exists() = if (this is DestinationPoint) true
    else (gameObjectRepository.existsById(this.id))

}
