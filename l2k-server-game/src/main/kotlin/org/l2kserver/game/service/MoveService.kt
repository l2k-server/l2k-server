package org.l2kserver.game.service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.handler.dto.request.MoveRequest
import org.l2kserver.game.handler.dto.request.StartRotationRequest
import org.l2kserver.game.handler.dto.request.StopRotationRequest
import org.l2kserver.game.handler.dto.request.ValidatePositionRequest
import org.l2kserver.game.handler.dto.response.ActionFailedResponse
import org.l2kserver.game.handler.dto.response.ArrivedResponse
import org.l2kserver.game.handler.dto.response.StartMovingResponse
import org.l2kserver.game.handler.dto.response.StartMovingToTargetResponse
import org.l2kserver.game.handler.dto.response.StartRotationResponse
import org.l2kserver.game.handler.dto.response.StopRotationResponse
import org.l2kserver.game.handler.dto.response.TeleportResponse
import org.l2kserver.game.handler.dto.response.ValidatePositionResponse
import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.Intention
import org.l2kserver.game.model.actor.DestinationPoint
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.GameWorldObject
import org.l2kserver.game.model.actor.MoveType
import org.l2kserver.game.model.actor.MutableActorInstance
import org.l2kserver.game.model.actor.PlayerCharacterInstanceImpl
import org.l2kserver.game.model.actor.character.PlayerCharacterInstance
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sendTo
import org.l2kserver.game.network.session.sessionContext
import org.l2kserver.game.repository.GameObjectRepository
import org.l2kserver.game.model.time.GameTime
import org.l2kserver.game.utils.time.withDelay
import org.springframework.stereotype.Service
import kotlin.math.hypot

private const val ROTATE_SPEED_PER_SEC = 65536

/** Service to handle actors moving */
@Service
class MoveService(
    private val geoDataService: GeoDataService,
    private val asyncTaskService: AsyncTaskService,
    override val gameObjectRepository: GameObjectRepository
) : AbstractService() {

    override val log = logger()

    /** Handle request to move character to some destination point */
    suspend fun moveCharacter(request: MoveRequest) {
        val context = sessionContext()
        val character = gameObjectRepository.findCharacterById(context.getCharacterId())

        log.debug {
            "Player '${context.getAccountName()}' is trying to move character '${character.name}' " +
                "to position '${request.targetPosition}' ${if (request.byMouse) "by mouse" else "by arrow keys"}"
        }

        character.intentionQueue.enqueue(Intention.Move(DestinationPoint(request.targetPosition)))
    }

    /** Handles position validation request */
    suspend fun validatePosition(request: ValidatePositionRequest) = suspendTransaction {
        val characterId = sessionContext().getCharacterIdOrNull() ?: run {
            val accountName = sessionContext().getAccountNameOrNull()
            log.warn { "Player '$accountName' has not selected character" }
            return@suspendTransaction
        }

        val character = gameObjectRepository.findCharacterById(characterId)

        if (!character.position.isCloseTo(request.position, Position.GEO_CELL_SIZE)) {
            log.warn { "$character position at the client side differs from server one greatly!" +
                    " Client: '${request.position}', Server: '${character.position}'" }
            send { ValidatePositionResponse(characterId, character.position, character.heading) }
        }
        else character.position = request.position

        //CRUTCH If this response is not sent - character becomes stuck
        send { ActionFailedResponse }
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

    /** Moves [actor] to [position] */
    @Deprecated("Use intention version")
    suspend fun move(actor: MutableActorInstance, position: Position) =
        executeMoving(actor, Intention.Move(DestinationPoint(position), 0))

    /** Moves [actor] according to his [intention] */
    suspend fun executeMoving(actor: MutableActorInstance, intention: Intention.Move) = suspendTransaction {
        if (actor is PlayerCharacterInstance) log.debug { "Start moving '$actor' to '${intention.destination}'" }

        //If target of moving is actor himself (f.e. if he casts target skill on self) - do nothing
        if (actor == intention.destination) return@suspendTransaction

        //If actor is already at destination point - just turn to target
        if (actor.position.isCloseTo(intention.destination.position, intention.requiredDistance)) {
            //Actor should turn to target even if he is enough close
            launchTurning(actor, intention.destination.position)
            //This needed to show red target frame
            send { StartMovingToTargetResponse(actor, intention.destination.id, intention.requiredDistance) }
            return@suspendTransaction
        }

        try {
            var moveTimestamp = System.currentTimeMillis()
            var previousTargetPosition: Position? = null
            var requiredDistance: Int? = null
            var destination: Position? = null
            var target: GameWorldObject? = null
            var turningJob: Job? = null

            var moveIterations = 0

            while (
                currentCoroutineContext().isActive
                && intention.destination.exists()
                && !actor.position.isCloseTo(destination)
            ) withDelay {
                if (actor.isImmobilized) {
                    log.trace { "Actor '$actor' is immobilized" }
                    return@suspendTransaction
                }
                target = intention.destination

                //If target position changed, destination must be recalculated
                if (previousTargetPosition != target.position || requiredDistance != intention.requiredDistance) {
                    requiredDistance = intention.requiredDistance
                    previousTargetPosition = target.position
                    destination = geoDataService.getAvailableTargetPosition(
                        startPosition = actor.position,
                        targetPosition = actor.position.positionBetween(
                            target.position,
                            requiredDistance
                        )
                    )

                    broadcastAround(actor.position) {
                        if (target is ActorInstance)
                            StartMovingToTargetResponse(actor, target!!.id, requiredDistance)
                        else
                            StartMovingResponse(actor, target.position)
                    }

                    turningJob?.cancelAndJoin()
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

                updatePosition(actor, destination!!, moveDistance, updateObjects)
                moveTimestamp = System.currentTimeMillis()
            }
            //Join turning only if target is DestinationPoint, otherwise
            if (target is DestinationPoint) turningJob?.join()
            log.trace { "'$actor' has arrived" }
        } catch (e: CancellationException) {
            log.trace { "MoveToTarget job was cancelled for reason: ${e.message}" }
        } catch (e: Exception) {
            log.error(e) { "An error occurred while trying to update position of character '$actor'" }
        } finally {
            updateObjectsAround(actor)
            broadcastAround(actor.position) { ArrivedResponse(actor.id, actor.position, actor.heading) }
        }
    }

    /**
     * Launches coroutine, that turns [actor] to [targetPosition]
     */
    //TODO Turning must be async because character turns and moves/attacks simultaneously
    // (but not when moving to point)

    // Client shows turning by itself, so there is no need to send some responses here
    suspend fun launchTurning(actor: MutableActorInstance, targetPosition: Position) =
        CoroutineScope(currentCoroutineContext()).launch {
            if (actor is PlayerCharacterInstance) log.debug {
                "Started turning '$actor' to target position '$targetPosition'"
            }

            val newHeading = actor.position.headingTo(targetPosition)

            while (isActive && actor.heading != newHeading) withDelay {
                suspendTransaction {
                    val deltaHeading = (newHeading - actor.heading).toShort().toInt()

                    val rotation = if (deltaHeading > 0)
                        minOf((ROTATE_SPEED_PER_SEC / 1000 * GameTime.MILLIS_IN_TICK).toInt(), deltaHeading)
                    else maxOf((-ROTATE_SPEED_PER_SEC / 1000 * GameTime.MILLIS_IN_TICK).toInt(), deltaHeading)

                    actor.heading += rotation
                }

            }
            log.trace { "Successfully turned '$actor' to target position '$targetPosition'" }
        } 

    /** Teleports [actor] to [targetPosition] */
    suspend fun teleport(actor: MutableActorInstance, targetPosition: Position) {
        //TODO Checks if player can teleport ???
        log.debug { "Teleporting '$actor' to '$targetPosition'" }

        actor.intentionQueue.cancel()
        asyncTaskService.cancelActionByActorId(actor.id)

        val fixedPosition = targetPosition.copy(
            z = geoDataService.getNearestZ(targetPosition.x, targetPosition.y, targetPosition.z)
        )

        suspendTransaction {
            actor.position = fixedPosition
        }

        // CRUTCH: Remove character from all characters around and clear his known objects list
        // Otherwise client shows no objects around teleported character, if he teleports at position near previous
        if (actor is PlayerCharacterInstanceImpl) {
            actor.knownGameWorldObjects.forEach { obj -> actor.removeObjectFromKnownListAndNotify(obj) }
            actor.knownGameWorldObjects.clear()
        }

        sendTo(actor.id) { TeleportResponse(actor.id, fixedPosition) }
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
        val newZ = geoDataService.getFloorZ(newX, newY, actor.position.z)

        val newPosition = actor.position.copy(x = newX, y = newY, z = newZ)

        actor.position = newPosition
        log.trace { "Updated position of actor '$actor' to '$newPosition'" }
        if (updateObjects) updateObjectsAround(actor, destination)
    }

    //TODO Refactor this crutch
    private fun GameWorldObject?.exists() = when(this) {
        null -> false
        is DestinationPoint -> true
        else -> gameObjectRepository.existsById(this.id)
    }

}
