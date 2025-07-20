package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.domain.shortcut.Shortcut

private const val REGISTER_SHORTCUT_RESPONSE_PACKET_ID: UByte = 68u

data class CreateShortcutResponse(val shortcut: Shortcut): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(REGISTER_SHORTCUT_RESPONSE_PACKET_ID)
        putInt(shortcut.type.id)
        putInt(shortcut.index)
        putInt(shortcut.shortcutActionId)
        putInt(shortcut.actionLevel)
    }

}
