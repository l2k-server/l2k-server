package org.l2kserver.game.extensions.model.actor

import org.l2kserver.game.handler.dto.response.CharacterInfoResponse
import org.l2kserver.game.handler.dto.response.NpcInfoResponse
import org.l2kserver.game.handler.dto.response.ResponsePacket
import org.l2kserver.game.handler.dto.response.ScatteredItemResponse
import org.l2kserver.game.model.actor.GameWorldObject
import org.l2kserver.game.model.actor.NpcInstanceImpl
import org.l2kserver.game.model.actor.PlayerCharacterInstanceImpl
import org.l2kserver.game.model.actor.ScatteredItem
import org.l2kserver.game.model.actor.character.PlayerCharacterInstance

/**
 * Creates InfoResponse about `this` game object
 *
 * @param sessionOwner - this packet addressee (required to send if `this` is an enemy of addressee, etc.)
 */
fun GameWorldObject.toInfoResponse(sessionOwner: PlayerCharacterInstance): ResponsePacket = when(this) {
    is NpcInstanceImpl -> NpcInfoResponse(this, sessionOwner)
    is PlayerCharacterInstanceImpl -> CharacterInfoResponse(this)
    is ScatteredItem -> ScatteredItemResponse(this)
    else -> throw IllegalArgumentException("Unknown game worldObject type ${this::class}")
}
