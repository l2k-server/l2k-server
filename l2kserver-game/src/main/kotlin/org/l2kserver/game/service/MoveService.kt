package org.l2kserver.game.service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.l2kserver.game.extensions.forEachMatching
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.extensions.model.actor.toInfoResponse
import org.l2kserver.game.handler.dto.request.MoveRequest
import org.l2kserver.game.handler.dto.request.ValidatePositionRequest
import org.l2kserver.game.handler.dto.response.ActionFailedResponse
import org.l2kserver.game.handler.dto.response.ArrivedResponse
import org.l2kserver.game.handler.dto.response.DeleteObjectResponse
import org.l2kserver.game.handler.dto.response.PrivateStoreSellSetMessageResponse
import org.l2kserver.game.handler.dto.response.SetTargetResponse
import org.l2kserver.game.handler.dto.response.StartMovingResponse
import org.l2kserver.game.handler.dto.response.StartMovingToTargetResponse
import org.l2kserver.game.handler.dto.response.TeleportResponse
import org.l2kserver.game.handler.dto.response.ValidatePositionResponse
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.Actor
import org.l2kserver.game.model.actor.GameWorldObject
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sendTo
import org.l2kserver.game.network.session.sessionContext
import org.l2kserver.game.repository.GameObjectRepository
import org.l2kserver.game.model.time.GameTime
import org.springframework.stereotype.Service
import kotlin.coroutines.coroutineContext
import kotlin.math.hypot

private const val ROTATE_SPEED_PER_SEC = 65536

/**
 * Service to handle actors moving
 */
@Service
class MoveService(
    private val asyncTaskService: AsyncTaskService,
    private val geoDataService: GeoDataService,

    override val gameObjectRepository: GameObjectRepository
) : AbstractService() {

    override val log = logger()

    /**
     * Handle request to move character to some destination point
     */
    //TODO Fix cancelling task on new MoveRequest https://github.com/l2kserver/l2kserver-game/issues/7
    suspend fun moveCharacter(request: MoveRequest) {
        val context = sessionContext()
        val character = gameObjectRepository.findCharacterById(context.getCharacterId())

        log.debug(
            "Player '{}' is trying to move character '{}' to position '{}' {}",
            context.getAccountName(),
            character.name,
            request.targetPosition,
            if (request.byMouse) "by mouse" else "by arrows"
        )

        asyncTaskService.launchAction(character.id) { move(character, request.targetPosition) }
    }

    /**
     * Handles position validation request.
     * If character position at client side differs from value at the client side insignificantly -
     * modifies character position at server side, otherwise - sends ValidatePositionResponse with actual position
     */
    suspend fun validatePosition(request: ValidatePositionRequest) = newSuspendedTransaction {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())

        if (character.position.isCloseTo(request.position)) {
            log.trace("Difference is too small, modifying position at server side")
            character.position = request.position
        } else {
            log.trace("Difference is too big, modifying position at client side")
            send(ValidatePositionResponse(character.id, character.position, character.heading))
        }
    }

    /**
     * Moves [actor] to [position]
     *
     * This suspending function is `cancellable`
     */
    suspend fun move(actor: Actor, position: Position) = newSuspendedTransaction {
        if (actor.isImmobilized) {
            log.trace("Actor '{}' is immobilized and cannot move to position '{}'", actor, position)
            send(ActionFailedResponse)
            return@newSuspendedTransaction
        }
        log.trace("Start moving actor '{}' to position '{}'", actor, position)

        val destinationPosition = geoDataService.getAvailableTargetPosition(actor.position, position)
        val turningJob = launchTurning(actor, destinationPosition)

        try {
            broadcastPacket(StartMovingResponse(actor.id, actor.position, position), actor.position)
            actor.isMoving = true

            var moveTimestamp = System.currentTimeMillis()
            while (coroutineContext.isActive) {
                val startUpdatingPositionTimestamp = System.currentTimeMillis()
                if (actor.isImmobilized) return@newSuspendedTransaction

                if (updatePosition(actor, destinationPosition, System.currentTimeMillis() - moveTimestamp)) break

                moveTimestamp = System.currentTimeMillis()
                //Sleep for 1 tick minus time of updating operation
                delay(GameTime.MILLIS_IN_TICK - (System.currentTimeMillis() - startUpdatingPositionTimestamp))
            }

            log.trace("Actor '{}' has arrived to position '{}'", actor, actor.position)
            turningJob.join()
        } catch (e: CancellationException) {
            log.trace("MoveToPosition job was cancelled for reason: {}", e.message)
        } catch (e: Exception) {
            log.error("An error occurred while trying to update position of actor '{}'", actor, e)
        } finally {
            actor.isMoving = false
            broadcastPacket(ArrivedResponse(actor.id, actor.position, actor.position.headingTo(position)), actor.position)
        }
    }

    /**
     * Moves [actor] to [target] by specified [requiredDistance]
     */
    suspend fun move(actor: Actor, target: GameWorldObject, requiredDistance: Int = 0) = newSuspendedTransaction {
        //Actor should turn to target anyway
        val turningJob = launchTurning(actor, target.position)

        //If actor is already at destination point - no need to do anything else
        if (actor.position.isCloseTo(target.position, requiredDistance))
            return@newSuspendedTransaction

        if (actor.isImmobilized) {
            log.trace("Actor '{}' is immobilized and cannot move to target '{}'", actor, target)
            send(ActionFailedResponse)
            return@newSuspendedTransaction
        }
        log.trace("Start moving actor '{}' to target '{}'", actor, target)

        var destinationPosition = geoDataService.getAvailableTargetPosition(
            startPosition = actor.position,
            targetPosition = actor.position.positionBetween(target.position, requiredDistance)
        )

        try {
            broadcastPacket(StartMovingResponse(actor.id, actor.position, target.position))
            send(StartMovingToTargetResponse(actor.id, target.id, requiredDistance, actor.position))
            actor.isMoving = true

            var moveTimestamp = System.currentTimeMillis()
            var previousTargetPosition = target.position

            while (coroutineContext.isActive && gameObjectRepository.existsById(target.id)) {
                if (actor.isImmobilized) return@newSuspendedTransaction
                val startUpdatingPositionTimestamp = System.currentTimeMillis()

                if (previousTargetPosition != target.position) {
                    broadcastPacket(StartMovingResponse(actor.id, actor.position, target.position), actor.position)
                    send(StartMovingToTargetResponse(actor.id, target.id, requiredDistance, actor.position))

                    previousTargetPosition = target.position
                }

                destinationPosition = geoDataService.getAvailableTargetPosition(
                    startPosition = actor.position,
                    targetPosition = actor.position.positionBetween(target.position, requiredDistance)
                )

                if (updatePosition(actor, destinationPosition, System.currentTimeMillis() - moveTimestamp)) break

                moveTimestamp = System.currentTimeMillis()
                //Sleep for 1 tick minus time of updating operation
                delay(GameTime.MILLIS_IN_TICK - (System.currentTimeMillis() - startUpdatingPositionTimestamp))
                //TODO Commit?
            }
            turningJob.join()
            log.trace("Actor '{}' has arrived to target '{}' on distance '{}'", actor, target, requiredDistance)
        } catch (e: CancellationException) {
            log.trace("MoveToTarget job was cancelled for reason: {}", e.message)
        } catch (e: Exception) {
            log.error("An error occurred while trying to update position of character '{}'", actor, e)
        } finally {
            actor.isMoving = false
            broadcastPacket(ArrivedResponse(actor.id, actor.position, actor.heading), actor.position)
        }
    }

    /**
     * Launches coroutine, that turns [actor] to [targetPosition]
     */
    // Turning must be async because character turns and moves/attacks simultaneously
    // Client shows turning by itself, so there is no need to send some responses here
    suspend fun launchTurning(actor: Actor, targetPosition: Position) = CoroutineScope(coroutineContext).launch {
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
    suspend fun teleport(actor: Actor, targetPosition: Position) = newSuspendedTransaction {
        log.debug("Teleporting '{}' to '{}'", actor, targetPosition)
        asyncTaskService.cancelAndJoinActionByActorId(actor.id)

        val fixedPosition = targetPosition.copy(
            z = geoDataService.getNearestZ(targetPosition.x, targetPosition.y, targetPosition.z)
        )

        //TODO Checks if player can teleport ???
        sendTo(actor.id, TeleportResponse(actor.id, fixedPosition))
        broadcastPacket(DeleteObjectResponse(actor.id), actor)

        actor.targetedBy.forEach { gameObjectRepository.findActorById(it).targetId = null }
        actor.targetedBy.clear()
        actor.targetId = null

        // Imitate teleporting process. Client validates position after disappearance animation ends,
        // so it will break if position will change immediately
        delay(1000)

        newSuspendedTransaction { actor.position = fixedPosition }
        broadcastActorInfo(actor)
        gameObjectRepository.findAllNear(actor).forEach { sendTo(actor.id, it.toInfoResponse()) }
    }

    /**
     * Updates [actor]'s position after [movingTimeMillis] time moving
     *
     * @param actor Character, that moves
     * @param destination Destination position
     * @param movingTimeMillis How much time character was moving (in millis)
     *
     * @return True if actor arrived to position, false - if not
     */
    private suspend fun updatePosition(actor: Actor, destination: Position, movingTimeMillis: Long): Boolean {
        val gameObjectsAround = gameObjectRepository.findAllNear(gameObjectRepository.findById(actor.id))

        //Checking this at the beginning of position updating gives one more tick to move to emulate moving properly
        if (actor.position.isCloseTo(destination)) return true

        val deltaX = actor.position.deltaX(destination).toDouble()
        val deltaY = actor.position.deltaY(destination).toDouble()

        val distanceXY = hypot(deltaX, deltaY)
        val sin = deltaY / distanceXY
        val cos = deltaX / distanceXY

        //The path traveled since the last update
        val moving = actor.moveSpeed.toDouble() / 1000 * movingTimeMillis

        // minOf prevents jumping around destination point if speed is too big
        // and moving is greater than way to go
        val newX = (minOf(moving, distanceXY) * cos).toInt() + actor.position.x
        val newY = (minOf(moving, distanceXY) * sin).toInt() + actor.position.y
        val newZ = geoDataService.getNearestZ(newX, newY, actor.position.z)

        val newPosition = actor.position.copy(x = newX, y = newY, z = newZ)

        actor.position = newPosition
        log.trace("Updated position of actor '{}' to '{}'", actor, newPosition)

        updateObjectsAround(gameObjectsAround, actor, destination)

        //CRUTCH
        // If Z becomes lower too fast, send ValidatePositionResponse with actual position,
        // to prevent falling under the textures on client side
        if (actor is PlayerCharacter && actor.position.deltaZ(newPosition) < -Position.ACCEPTABLE_DELTA) {
            send(ValidatePositionResponse(actor.id, newPosition, actor.heading))
        }

        return false
    }

    /**
     * Sends information about new appeared objects and notifies characters about moving
     *
     * @param prevGameObjectsAround List of previously noticed objects
     * @param actor Moving actor
     * @param destination Destination position of actor's moving.
     * If null, no StartMovingResponse will be sent to new players, who see this actor
     */
    private suspend fun updateObjectsAround(
        prevGameObjectsAround: List<GameWorldObject>,
        actor: Actor,
        destination: Position? = null
    ) {
        val newGameObjectsAround = gameObjectRepository.findAllNear(actor)

        // For all game objects that are now too far to see them
        prevGameObjectsAround.forEachMatching({ !newGameObjectsAround.contains(it) }) {
            send(DeleteObjectResponse(it.id))

            if (actor is PlayerCharacter && actor.targetId == it.id) {
                actor.targetId = null
                if (it is Actor) it.targetedBy.remove(actor.id)
                send(SetTargetResponse(0, 0))
            }

            if (it is PlayerCharacter) {
                sendTo(it.id, DeleteObjectResponse(actor.id))

                if (it.targetId == actor.id) {
                    it.targetId = null
                    actor.targetedBy.remove(it.id)
                    sendTo(it.id, SetTargetResponse(0, 0))
                }
            }
        }

        // For all game objects that are now enough close to see them
        newGameObjectsAround.forEachMatching({ !prevGameObjectsAround.contains(it) }) {
            send(it.toInfoResponse())

            if (it is PlayerCharacter) {
                sendTo(it.id, actor.toInfoResponse())
                destination?.let { destination ->
                    sendTo(it.id, StartMovingResponse(actor.id, actor.position, destination))
                }
                it.privateStore?.let { store ->
                    broadcastPacket(PrivateStoreSellSetMessageResponse(it.id, store.title), it.position)
                }
            }
        }
    }

}
