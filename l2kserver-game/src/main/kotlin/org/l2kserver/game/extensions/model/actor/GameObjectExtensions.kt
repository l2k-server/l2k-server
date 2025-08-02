package org.l2kserver.game.extensions.model.actor

import org.l2kserver.game.handler.dto.response.CharacterInfoResponse
import org.l2kserver.game.handler.dto.response.NpcInfoResponse
import org.l2kserver.game.handler.dto.response.ResponsePacket
import org.l2kserver.game.handler.dto.response.ScatteredItemResponse
import org.l2kserver.game.model.actor.GameObject
import org.l2kserver.game.model.actor.Npc
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.actor.ScatteredItem

fun GameObject.toInfoResponse(): ResponsePacket = when(this) {
    is Npc -> NpcInfoResponse(this)
    is PlayerCharacter -> CharacterInfoResponse(this)
    is ScatteredItem -> ScatteredItemResponse(this)
    else -> throw IllegalArgumentException("Unknown game object type ${this::class}")
}
