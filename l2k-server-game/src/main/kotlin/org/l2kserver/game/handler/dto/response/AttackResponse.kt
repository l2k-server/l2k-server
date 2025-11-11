package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.item.template.Grade
import org.l2kserver.game.model.skill.effect.DamageEffect

private const val ATTACK_RESPONSE_PACKET_ID: UByte = 5u

/**
 * Attack data
 *
 * @property attacker Actor, who attacks
 * @property attacks All the hits dealt by this attack
 */
data class AttackResponse(
    val attacker: ActorInstance,
    val attacks: List<DamageEffect>,
    val usedSoulshot: Boolean
): ResponsePacket {
    
    constructor(
        attacker: ActorInstance, attack: DamageEffect, usedSoulshot: Boolean
    ): this(attacker, listOf(attack), usedSoulshot)

    init {
        require(attacks.isNotEmpty()) { "Attack must contain at least one hit!" }
    }

    override val data = littleEndianByteArray {
        val weaponGrade = (attacker as? PlayerCharacter)?.inventory?.weapon?.grade ?: Grade.NO_GRADE

        putUByte(ATTACK_RESPONSE_PACKET_ID)
        putInt(attacker.id)
        put(attacks.first().toByteArray(usedSoulshot, weaponGrade))
        putInt(attacker.position.x)
        putInt(attacker.position.y)
        putInt(attacker.position.z)

        putShort((attacks.size - 1).toShort())
        for (i: Int in 1..< attacks.size) {
            put(attacks[i].toByteArray(usedSoulshot, weaponGrade))
        }
    }

}

private fun DamageEffect.toByteArray(usedSoulshot: Boolean, weaponGrade: Grade) = littleEndianByteArray {
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
