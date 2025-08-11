package org.l2kserver.game.handler.dto.request

import org.l2kserver.game.model.actor.character.ShortcutType
import java.nio.ByteBuffer

const val REGISTER_SHORTCUT_REQUEST_PACKET_ID: UByte = 51u

/**
 * Request to register new shortcut
 *
 * @param type Type of shortcut
 * @param index Index of shortcut at shortcut panel
 * @param shortcutActionId ID of stuff bound to this shortcut.
 * For action - action id, for skill - skill id, for item - item id, etc.
 */
data class CreateShortcutRequest(
    val type: ShortcutType,
    val index: Int,
    val shortcutActionId: Int
): RequestPacket {
    constructor(data: ByteBuffer): this(
        type = ShortcutType.byId(data.getInt()),
        index = data.getInt(),
        shortcutActionId = data.getInt()
    )
}
