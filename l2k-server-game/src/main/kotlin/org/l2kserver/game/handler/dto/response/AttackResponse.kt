package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.item.template.Grade
import org.l2kserver.game.model.skill.action.effect.DamageEffect

private const val ATTACK_RESPONSE_PACKET_ID: UByte = 5u

/**
 * Attack data
 *
 * @property attacker Actor, who attacks
 * @property hits All the hits dealt by this attack
 */
data class AttackResponse(
    val attacker: ActorInstance,
    val hits: List<DamageEffect>,
): ResponsePacket {

    init {
        require(hits.isNotEmpty()) { "Attack must contain at least one hit!" }
    }

    override val data = littleEndianByteArray {
        val weaponGrade = (attacker as? PlayerCharacter)?.inventory?.weapon?.grade ?: Grade.NO_GRADE

        putUByte(ATTACK_RESPONSE_PACKET_ID)
        putInt(attacker.id)
        put(hits.first().toByteArray(weaponGrade))
        putInt(attacker.position.x)
        putInt(attacker.position.y)
        putInt(attacker.position.z)

        putShort((hits.size - 1).toShort())
        for (i: Int in 1..< hits.size) {
            put(hits[i].toByteArray(weaponGrade))
        }
    }

}

private fun DamageEffect.toByteArray(weaponGrade: Grade) = littleEndianByteArray {
    putInt(targetId)
    putInt(damage)

    // The result will be byte value, where left bits correspond to attack flags
    // If all flags enabled, the result will be 0b11110000
    var flags: UByte = 0u
    if (isAvoided) flags = flags or 128u
    if (isBlocked) flags = flags or 64u
    if (isCritical) flags = flags or 32u
    if (usedSoulshot) flags = flags or 16u or weaponGrade.ordinal.toUByte()

    putUByte(flags)
}
