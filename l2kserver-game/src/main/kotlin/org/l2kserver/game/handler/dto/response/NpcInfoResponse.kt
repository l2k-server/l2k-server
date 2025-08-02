package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUTF16String
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.extensions.toByte
import org.l2kserver.game.extensions.toInt
import org.l2kserver.game.model.actor.NpcImpl
import org.l2kserver.game.model.actor.MoveType

private const val NPC_INFO_RESPONSE_PACKET_ID: UByte = 22u

/**
 * Info about NPC that should be sent to all players who see it
 */
data class NpcInfoResponse(
    val npc: NpcImpl
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(NPC_INFO_RESPONSE_PACKET_ID)

        putInt(npc.id)
        putInt(npc.templateId)
        putInt(npc.isEnemy.toInt())

        putInt(npc.position.x)
        putInt(npc.position.y)
        putInt(npc.position.z)
        putInt(npc.heading.toInt())

        putInt(0)

        putInt(npc.stats.castingSpd)
        putInt(npc.stats.atkSpd)
        putInt(npc.stats.speed)
        putInt(npc.stats.walkSpeed)
        putInt(npc.stats.speed) //swimRunSpeed
        putInt(npc.stats.walkSpeed) //swimWalkSpeed
        putInt(npc.stats.speed) //flyRunSpeed
        putInt(npc.stats.walkSpeed) //flyWalkSpeed
        putInt(npc.stats.speed) //flyRunSpeed
        putInt(npc.stats.walkSpeed) //flyWalkSpeed TODO fly speed twice??
        putDouble(1.1) //runSpeedMultiplier
        putDouble(npc.stats.atkSpd / 277.478340719) //attackSpeedMultiplier

        putDouble(npc.collisionBox.radius)
        putDouble(npc.collisionBox.height)

        putInt(0) //right hand weapon
        putInt(0)
        putInt(0) //left hand

        put(1) //showName(?)
        put((npc.moveType == MoveType.RUN).toByte())
        put(npc.isFighting.toByte())
        put(npc.isDead().toByte()) //TODO fake death
        put(0) //isSummoned 2 - summoned, 1 - true, 0 - false

        putUTF16String(npc.name)
        putUTF16String(npc.title)

        putInt(0) //0 - green title, else - blue title. TODO check wtf
        putInt(0) //isPvp :D 0 - false, 1 - true, 2 - pvp is ending(blinking)
        putInt(0) // Karma :D

        putInt(0) // abnormalEffect

        putInt(0)
        putInt(0)
        putInt(0)
        putInt(0)
        put(0)

        put(0) //teamCircle 1 - blue, 2 - red

        putDouble(npc.collisionBox.radius)
        putDouble(npc.collisionBox.height)

        putInt(0)
        putInt(0)
    }

}
