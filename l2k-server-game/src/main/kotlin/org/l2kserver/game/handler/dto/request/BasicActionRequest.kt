package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer
import org.l2kserver.game.extensions.getBoolean

const val BASIC_ACTION_REQUEST_PACKET_ID: UByte = 69u

/**
 * Character or servitor basic action request
 *
 * @param action what action should be performed
 * @param isForced if true, aggressive action (like summon attack) will be performed even on friendly target
 * @param holdPosition if true, prevents action performing, if distance to target is too far
 * (actor won't move to target to perform action)
 */
data class BasicActionRequest(
    val action: BasicAction,
    val isForced: Boolean,
    val holdPosition: Boolean
): RequestPacket {

    constructor(data: ByteBuffer): this(
        action = BasicAction.byId(data.getInt()),
        isForced = data.getInt() != 0,
        holdPosition = data.getBoolean()
    )

}

enum class BasicAction(val id: Int) {
    TOGGLE_SIT_STAND(0),
    //TODO summon actions
    TOGGLE_WALK_RUN(1),
    GENERAL_MANUFACTURE(51);

    companion object {
        private val entities = entries.associateBy { it.id }

        fun byId(id: Int) = requireNotNull(entities[id]) { "Invalid basic action id '$id'" }
    }
}
