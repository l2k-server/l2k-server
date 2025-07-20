package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.position.Position
import org.l2kserver.game.model.actor.Actor

private const val ATTACK_RESPONSE_PACKET_ID: UByte = 5u

/**
 * Attack data
 *
 * @property attackerId ID of actor, who attacks
 * @property attackerPosition Position of actor, who attacks
 * @property hits All the hits dealt by this attack
 */
data class AttackResponse(
    val attackerId: Int,
    val attackerPosition: Position,
    val hits: List<Hit>,
): ResponsePacket {

    constructor(attacker: Actor, hits: List<Hit>): this(
        attackerId = attacker.id,
        attackerPosition = attacker.position,
        hits = hits
    )

    init {
        require(hits.isNotEmpty()) { "Hits must not be empty" }
    }

    override val data = littleEndianByteArray {
        putUByte(ATTACK_RESPONSE_PACKET_ID)
        putInt(attackerId)
        put(hits.first().toByteArray())
        putInt(attackerPosition.x)
        putInt(attackerPosition.y)
        putInt(attackerPosition.z)

        putShort((hits.size - 1).toShort())
        for (i: Int in 1..< hits.size) {
            put(hits[i].toByteArray())
        }
    }

}

/**
 * Data class representing single target hit
 *
 * @param targetId ID of game object that was hit
 * @param damage How many damage has the attack dealt
 * @param usedSoulshot is soulshot used during this attack
 * @param isCritical is this attack critical
 * @param isBlocked is this attack blocked by shield
 * @param isAvoided is this attack missed
 */
data class Hit(
    val targetId: Int,
    val damage: Int = 0,

    val usedSoulshot: Boolean = false,
    val isCritical: Boolean = false,
    val isBlocked: Boolean = false,
    val isAvoided: Boolean = false
) {
    fun toByteArray() = littleEndianByteArray {
        putInt(targetId)
        putInt(damage)

        // The result will be byte value, where left bits correspond to attack flags
        // If all flags enabled, the result will be 0b11110000
        var flags: UByte = 0u
        if (isAvoided) flags = flags or 128u
        if (isCritical) flags = flags or 32u
        if (isBlocked) flags = flags or 64u
        if (usedSoulshot) flags = flags or 16u // TODO + soulshotGrade

        putUByte(flags)
    }

}
