package org.l2kserver.game.service

import org.l2kserver.game.extensions.model.store.toMessageResponse
import org.l2kserver.game.handler.dto.response.ChangePostureResponse
import org.l2kserver.game.handler.dto.response.FullCharacterResponse
import org.l2kserver.game.handler.dto.response.ResponsePacket
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.MutableActorInstance
import org.l2kserver.game.model.actor.PlayerCharacterInstanceImpl
import org.l2kserver.game.model.actor.Posture
import org.l2kserver.game.repository.GameWorld
import org.l2kserver.game.network.session.sendTo
import io.github.oshai.kotlinlogging.KLogger
import org.l2kserver.game.extensions.model.actor.toInfoResponse
import org.springframework.beans.factory.annotation.Value
import kotlin.collections.minus

abstract class AbstractService {
    protected abstract val gameWorld: GameWorld
    protected abstract val log: KLogger

    @Value($$"${characters.perceptionRadius}")
    protected var charactersPerceptionRadius = 0; private set

    /**
     * Sends [responsePacket] to all characters near provided [position] on given [distance] (default - vision range).
     * Should be used only in SessionContext
     */
    protected suspend inline fun broadcastAround(
        position: Position,
        distance: Int = charactersPerceptionRadius,
        crossinline responsePacket: () -> ResponsePacket
    ) {
        val addressees = gameWorld.findAllCharactersNear(position, distance)
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
        distance: Int = charactersPerceptionRadius,
        crossinline responsePacket: () -> ResponsePacket
    ) {
        val addressees = gameWorld.findAllCharactersNear(actor, distance)
        val lazyResponse by lazy { responsePacket() }
        addressees.forEach { sendTo(it.id) { lazyResponse }}
    }

    /**
     * Sends Information about actor to all nearby characters
     *
     * Should be used only in SessionContext
     */
    protected suspend fun broadcastActorInfo(actor: ActorInstance) {
        gameWorld.findAllCharactersNear(actor, charactersPerceptionRadius)
            .forEach { addressee ->
                sendTo(addressee.id) { actor.toInfoResponse(addressee) }
            }

        if (actor is PlayerCharacterInstanceImpl) {
            sendTo(actor.id) { FullCharacterResponse(actor) }
            actor.privateStore?.let {
                broadcastAround(actor.position) { it.toMessageResponse(actor.id) }
            }
        }
    }

    /**
     * Scans `perception area` of [actor] and updates his's [MutableActorInstance.knownGameWorldObjects]
     *
     * @return 2 sets - appeared and disappeared objects
     */
    fun scanAround(actor: MutableActorInstance, scanRadius: Int) = with(
        gameWorld.findAllNear(actor, scanRadius).toSet()
    ) {
        val toAdd = this - actor.knownGameWorldObjects
        val toRemove = actor.knownGameWorldObjects - this
        actor.knownGameWorldObjects = this
        toAdd to toRemove
    }

    /**
     * Scans `perception area` of [actor] looking for players
     * and updates his's [MutableActorInstance.knownGameWorldObjects]
     *
     * @return 2 sets - appeared and disappeared objects
     */
    fun scanPlayersAround(actor: MutableActorInstance, scanRadius: Int) = with(
        gameWorld.findAllCharactersNear(actor, scanRadius).toSet()
    )  {
        val toAdd = (this - actor.knownGameWorldObjects)
        val toRemove = actor.knownGameWorldObjects - this

        actor.knownGameWorldObjects = this

        toAdd to toRemove
    }

    protected fun ActorInstance.exists() = gameWorld.existsById(this.id)

    /** Makes this character sit down (if he is standing) */
    protected suspend fun PlayerCharacterInstanceImpl.sitDown() {
        if (this.posture != Posture.SITTING) {
            this.posture = Posture.SITTING
            this@AbstractService.broadcastAround( this.position) {
                ChangePostureResponse(this.id, this.position, this.posture)
            }
        }
    }

    /** Makes this character to stand up (if he is not standing) */
    protected suspend fun PlayerCharacterInstanceImpl.standUp() {
        if (this.posture != Posture.STANDING) {
            this.posture = Posture.STANDING
            this@AbstractService.broadcastAround(this.position) {
                ChangePostureResponse(this.id, this.position, this.posture)
            }
        }
    }
}
