package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer
import org.l2kserver.game.model.actor.position.Position

const val ATTACK_REQUEST_PACKET_ID: UByte = 10u

/**
 * Request to attack target with [targetId]
 * This request is sent by client when CTRL is pressed on attack action or left-click
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
            data.getInt(),
            data.getInt(),
            data.getInt()
        )
    )

}
