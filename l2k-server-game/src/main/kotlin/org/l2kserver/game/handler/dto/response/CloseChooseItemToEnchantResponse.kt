package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte

private const val CLOSE_CHOOSE_ITEM_TO_ENCHANT_RESPONSE_PACKET_ID: UByte = 129u

data object CloseChooseItemToEnchantResponse: ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(CLOSE_CHOOSE_ITEM_TO_ENCHANT_RESPONSE_PACKET_ID)
    }

}
