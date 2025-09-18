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
     * Sends [responsePacket] to all characters near provided [position] on given [distance].
     * If position is null, sends packet to all players in game
     *
     * Should be used only in SessionContext
     */
    protected suspend fun broadcastPacket(
        responsePacket: ResponsePacket, position: Position? = null, distance: Int = VISION_RANGE
    ) {
        val addressees = position?.let { gameObjectRepository.findAllCharactersNear(it, distance) }
            ?: gameObjectRepository.findAllCharacters()

        addressees.forEach { sendTo(it.id, responsePacket) }
    }

    /**
     * Sends [responsePacket] to all characters near provided [actor] on given [distance].
     * The [actor] himself won't get this packet
     *
     * Should be used only in SessionContext
     */
    protected suspend fun broadcastPacket(
        responsePacket: ResponsePacket, actor: ActorInstance, distance: Int = VISION_RANGE
    ) {
        gameObjectRepository.findAllCharactersNear(actor, distance)
            .forEach { sendTo(it.id, responsePacket) }
    }

    /**
     * Sends Information about actor to all nearby characters
     *
     * Should be used only in SessionContext
     */
    protected suspend fun broadcastActorInfo(actor: ActorInstance) {
        broadcastPacket(actor.toInfoResponse(), actor)

        if (actor is PlayerCharacter) {
            sendTo(actor.id, FullCharacterResponse(actor))
            actor.privateStore?.let { broadcastPacket(it.toMessageResponse(actor.id), actor.position) }
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
            broadcastPacket(ChangePostureResponse(this.id, this.position, this.posture), this.position)
        }
    }

    /** Makes this character to stand up (if he is not standing) */
    protected suspend fun PlayerCharacter.standUp() {
        if (this.posture != Posture.STANDING) {
            this.posture = Posture.STANDING
            broadcastPacket(ChangePostureResponse(this.id, this.position, this.posture), this.position)
        }
    }

    private suspend fun updateObjectsAroundNpc(
        npc: NpcInstance, movementDestination: Position?
    ) = gameObjectRepository.findAllCharactersNear(npc).forEach {
        if (it.knownGameWorldObjects.add(npc)) {
            sendTo(it.id, npc.toInfoResponse())
            if (movementDestination != null) sendTo(
                it.id,
                StartMovingResponse(npc.id, npc.position, movementDestination)
            )
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
        send(DeleteObjectResponse(gameObject.id))

        if (this.targetId == gameObject.id) {
            this.targetId = null
            if (gameObject is MutableActorInstance) gameObject.targetedBy.remove(this)
            send(SetTargetResponse(0, 0))
        }

        if (gameObject is PlayerCharacter) {
            gameObject.knownGameWorldObjects.remove(this)
            sendTo(gameObject.id, DeleteObjectResponse(this.id))

            if (gameObject.targetId == this.id) {
                gameObject.targetId = null
                this.targetedBy.remove(gameObject)
                sendTo(
                    gameObject.id,
                    SetTargetResponse(0, 0)
                )
            }
        }
    }

    private suspend fun PlayerCharacter.addToKnownListAndNotify(gameObject: GameWorldObject, movementDestination: Position?) {
        this.knownGameWorldObjects.add(gameObject)
        send(gameObject.toInfoResponse())

        if (gameObject is PlayerCharacter) {
            gameObject.knownGameWorldObjects.add(this)
            sendTo(gameObject.id, this.toInfoResponse())
            movementDestination?.let { destination ->
                sendTo(
                    gameObject.id,
                    StartMovingResponse(this.id, this.position, destination)
                )
            }
            gameObject.privateStore?.let { store ->
                send(
                    PrivateStoreSellSetMessageResponse(gameObject.id, store.title)
                )
            }
        }
    }
}
