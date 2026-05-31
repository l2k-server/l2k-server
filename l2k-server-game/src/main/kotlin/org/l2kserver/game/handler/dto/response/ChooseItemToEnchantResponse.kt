package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte

private const val CHOOSE_ITEM_TO_ENCHANT_RESPONSE_PACKET_ID: UByte = 111u

/**
 * Shows menu to choose item to enchant
 *
 * @property enchantScrollId Scroll item template identifier (to show its name in window heading)
 */
data class ChooseItemToEnchantResponse(val enchantScrollId: Int): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(CHOOSE_ITEM_TO_ENCHANT_RESPONSE_PACKET_ID)
        putInt(enchantScrollId)
    }

}
