package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer
import org.l2kserver.game.extensions.getBoolean
import org.l2kserver.game.model.position.Position

const val ACTION_REQUEST_PACKET_ID: UByte = 4u

/**
 * If player has no target selected, sets target to npc or player with targetId.
 * If targetId equals selected target id, applies an action on it (attack monster or talk to npc)
 *
 * @param targetId selected target id
 * @param position target's position
 * @param isForced set target force (for example, if target is dead)
 */
data class ActionRequest(
    val targetId: Int,
    val position: Position,
    val isForced: Boolean
): RequestPacket {

    constructor(data: ByteBuffer): this(
        targetId = data.getInt(),
        position = Position(
            x = data.getInt(),
            y = data.getInt(),
            z = data.getInt()
        ),
        isForced = data.getBoolean()
    )

}
