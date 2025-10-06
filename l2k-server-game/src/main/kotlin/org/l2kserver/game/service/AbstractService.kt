package org.l2kserver.game.service

import org.l2kserver.game.extensions.model.actor.toInfoResponse
import org.l2kserver.game.extensions.model.store.toMessageResponse
import org.l2kserver.game.handler.dto.response.ChangePostureResponse
import org.l2kserver.game.handler.dto.response.DeleteObjectResponse
import org.l2kserver.game.handler.dto.response.FullCharacterResponse
import org.l2kserver.game.handler.dto.response.PrivateStoreSellSetMessageResponse
import org.l2kserver.game.handler.dto.response.ResponsePacket
import org.l2kserver.game.handler.dto.response.SetTargetResponse
import org.l2kserver.game.handler.dto.response.StartMovingResponse
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.GameWorldObject
import org.l2kserver.game.model.actor.MutableActorInstance
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.actor.Posture
import org.l2kserver.game.model.actor.npc.NpcInstance
import org.l2kserver.game.network.session.send
import org.l2kserver.game.repository.GameObjectRepository
import org.l2kserver.game.network.session.sendTo
import org.slf4j.Logger

const val VISION_RANGE = 5_000

abstract class AbstractService {
    protected abstract val gameObjectRepository: GameObjectRepository
    protected abstract val log: Logger

    /**
     * Sends [responsePacket] to all characters near provided [position] on given [distance] (default - vision range).
     * Should be used only in SessionContext
     */
    protected suspend inline fun broadcastAround(
        position: Position,
        distance: Int = VISION_RANGE,
        crossinline responsePacket: () -> ResponsePacket
    ) {
        val addressees = gameObjectRepository.findAllCharactersNear(position, distance)
        val lazyResponse by lazy { responsePacket() }
        addressees.forEach { sendTo(it.id) { lazyResponse }}
    }

    /**
     * Sends [responsePacket] to all characters near provided [actor] on given [distance].
     * The [actor] himself won't get this packet
     *
     * Should be used only in SessionContext
     */
    protected suspend inline fun broadcastAround(
        actor: ActorInstance,
        distance: Int = VISION_RANGE,
        crossinline responsePacket: () -> ResponsePacket
    ) {
        val addressees = gameObjectRepository.findAllCharactersNear(actor, distance)
        val lazyResponse by lazy { responsePacket() }
        addressees.forEach { sendTo(it.id) { lazyResponse }}
    }

    /**
     * Sends Information about actor to all nearby characters
     *
     * Should be used only in SessionContext
     */
    protected suspend fun broadcastActorInfo(actor: ActorInstance) {
        broadcastAround(actor) { actor.toInfoResponse() }

        if (actor is PlayerCharacter) {
            sendTo(actor.id) { FullCharacterResponse(actor) }
            actor.privateStore?.let {
                broadcastAround(actor.position) { it.toMessageResponse(actor.id) }
            }
        }

    }

    /**
     * Sends information about appeared and disappeared objects
     *
     * @param actor Actor, who appeared at new place
     * @param destination Destination position of actor's moving.
     * If null, no StartMovingResponse will be sent to new players, who see this actor
     */
    suspend fun updateObjectsAround(actor: MutableActorInstance, destination: Position? = null) {
        if (actor is NpcInstance) updateObjectsAroundNpc(actor, destination)
        if (actor is PlayerCharacter) updateObjectsAroundCharacter(actor, destination)
    }

    /** Makes this character to sit down (if he is standing) */
    protected suspend fun PlayerCharacter.sitDown() {
        if (this.posture == Posture.STANDING) {
            this.posture = Posture.SITTING
            this@AbstractService.broadcastAround( this.position) {
                ChangePostureResponse(this.id, this.position, this.posture)
            }
        }
    }

    /** Makes this character to stand up (if he is not standing) */
    protected suspend fun PlayerCharacter.standUp() {
        if (this.posture != Posture.STANDING) {
            this.posture = Posture.STANDING
            this@AbstractService.broadcastAround(this.position) {
                ChangePostureResponse(this.id, this.position, this.posture)
            }
        }
    }

    private suspend fun updateObjectsAroundNpc(
        npc: NpcInstance, movementDestination: Position?
    ) = gameObjectRepository.findAllCharactersNear(npc).forEach {
        if (it.knownGameWorldObjects.add(npc)) {
            sendTo(it.id) { npc.toInfoResponse() }
            if (movementDestination != null) sendTo(it.id) {
                StartMovingResponse(npc, movementDestination)
            }
        }
    }

    private suspend fun updateObjectsAroundCharacter(character: PlayerCharacter, movementDestination: Position?) {
        val newGameObjectsAround = gameObjectRepository.findAllNear(character).toMutableSet()
        character.knownGameWorldObjects.forEach { knownObject ->
            //If known object now is too far - delete it.
            //If not - delete it from [newGameObjectsAround], to leave only not known objects for notifying
            if (newGameObjectsAround.contains(knownObject)) newGameObjectsAround.remove(knownObject)
            else character.removeObjectFromKnowsAndNotify(knownObject)
        }

        // For all game objects that are now enough close to see them
        newGameObjectsAround.forEach { newObject ->
            character.addToKnownListAndNotify(newObject, movementDestination)
        }
    }

    private suspend fun PlayerCharacter.removeObjectFromKnowsAndNotify(gameObject: GameWorldObject) {
        this.knownGameWorldObjects.remove(gameObject)
        send { DeleteObjectResponse(gameObject.id) }

        if (this.targetId == gameObject.id) {
            this.targetId = null
            if (gameObject is MutableActorInstance) gameObject.targetedBy.remove(this)
            send { SetTargetResponse(0, 0) }
        }

        if (gameObject is PlayerCharacter) {
            gameObject.knownGameWorldObjects.remove(this)
            sendTo(gameObject.id) { DeleteObjectResponse(this.id) }

            if (gameObject.targetId == this.id) {
                gameObject.targetId = null
                this.targetedBy.remove(gameObject)
                sendTo(gameObject.id) { SetTargetResponse(0, 0) }
            }
        }
    }

    private suspend fun PlayerCharacter.addToKnownListAndNotify(gameObject: GameWorldObject, movementDestination: Position?) {
        this.knownGameWorldObjects.add(gameObject)
        send { gameObject.toInfoResponse() }

        if (gameObject is PlayerCharacter) {
            gameObject.knownGameWorldObjects.add(this)
            sendTo(gameObject.id) { this.toInfoResponse() }

            movementDestination?.let { destination ->
                sendTo(gameObject.id) { StartMovingResponse(this, destination) }
            }
            gameObject.privateStore?.let { store ->
                send { PrivateStoreSellSetMessageResponse(gameObject.id, store.title) }
            }
        }
    }
}
