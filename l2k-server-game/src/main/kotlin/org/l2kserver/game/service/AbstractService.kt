package org.l2kserver.game.service

import org.l2kserver.game.extensions.model.actor.toInfoResponse
import org.l2kserver.game.extensions.model.store.toMessageResponse
import org.l2kserver.game.handler.dto.response.ChangePostureResponse
import org.l2kserver.game.handler.dto.response.FullCharacterResponse
import org.l2kserver.game.handler.dto.response.ResponsePacket
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.actor.Posture
import org.l2kserver.game.repository.GameObjectRepository
import org.l2kserver.game.network.session.sendTo
import org.slf4j.Logger

const val VISION_RANGE = 2000

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
     * Makes this character to sit down (if he is standing)
     */
    protected suspend fun PlayerCharacter.sitDown() {
        if (this.posture == Posture.STANDING) {
            this.posture = Posture.SITTING
            broadcastPacket(ChangePostureResponse(this.id, this.position, this.posture),this.position)
        }
    }

    /**
     * Makes this character to stand up (if he is not standing)
     */
    protected suspend fun PlayerCharacter.standUp() {
        if (this.posture != Posture.STANDING) {
            this.posture = Posture.STANDING
            broadcastPacket(ChangePostureResponse(this.id, this.position, this.posture),this.position)
        }
    }

}
