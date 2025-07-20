package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.domain.shortcut.Shortcut
import org.l2kserver.game.domain.shortcut.ShortcutType

private const val SHORTCUT_PANEL_RESPONSE_PACKET_ID: UByte = 69u

data class ShortcutPanelResponse(
    val shortcuts: List<Shortcut>
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(SHORTCUT_PANEL_RESPONSE_PACKET_ID)
        putInt(shortcuts.size)

        shortcuts.forEach {
            putInt(it.type.id)
            putInt(it.index)
            putInt(it.shortcutActionId)
            putInt(it.actionLevel)

            when (it.type) {
                ShortcutType.ITEM -> {
                    putInt(-1)
                    putInt(0)
                    putInt(0)
                    putInt(0)
                }
                ShortcutType.SKILL -> {
                    put(0)
                    putInt(1)
                }
                else -> {}
            }
        }
    }

}
