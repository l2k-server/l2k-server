package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer
import org.l2kserver.game.model.actor.position.Position

const val ATTACK_REQUEST_PACKET_ID: UByte = 10u

/**
 * Request to attack target with [targetId]
 *
 * @property targetId Attacked actor id
 * @property attackerPosition Position of character, who performs attack
 */
data class AttackRequest(
    val targetId: Int,
    val attackerPosition: Position,
): RequestPacket {

    constructor(data: ByteBuffer): this(
        targetId = data.getInt(),
        attackerPosition = Position(
            x = data.getInt(),
            y = data.getInt(),
            z = data.getInt()
        )
    )

}
