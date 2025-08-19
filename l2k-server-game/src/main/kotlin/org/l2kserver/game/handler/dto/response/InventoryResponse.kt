package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.item.instance.ItemInstance

private const val ITEMS_RESPONSE_PACKET_ID: UByte = 27u

data class InventoryResponse(
    val items: Collection<ItemInstance>,
    val showInventory: Boolean = false
) : ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(ITEMS_RESPONSE_PACKET_ID)
        putShort(if (showInventory) 1 else 0)
        putShort(items.size.toShort())

        items.forEach {
            putShort(it.group.id.toShort())
            putInt(it.id)
            putInt(it.templateId)
            putInt(it.amount)
            putShort(it.category.id.toShort())
            putShort(0) // Custom Type (?)
            putShort(if (it.isEquipped) 1 else 0)
            putInt(it.type.availableSlots.firstOrNull()?.id ?: 0)
            putShort(it.enchantLevel.toShort())
            putShort(0) // Custom Type 2 (?)
            putInt(it.augmentationId)
            putInt(0) //TODO Mana (of shadow item)
        }
    }

}
