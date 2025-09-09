package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.item.instance.ItemInstance

private const val UPDATE_ITEM_RESPONSE_PACKET_ID: UByte = 39u

/**
 * Response to notify client about changes in inventory
 *
 * @property operations List of item updates - [ItemInstance] to [UpdateItemOperation]
 */
@ConsistentCopyVisibility
data class UpdateItemsResponse private constructor(
    val operations: MutableList<Pair<ItemInstance, UpdateItemOperation>>
): ResponsePacket {
    constructor(): this(ArrayList())

    fun wasAdded(item: ItemInstance): UpdateItemsResponse {
        operations.add(item to UpdateItemOperation.ADD)
        return this
    }

    fun wasModified(item: ItemInstance): UpdateItemsResponse {
        operations.add(item to UpdateItemOperation.MODIFY)
        return this
    }

    fun wasDeleted(item: ItemInstance): UpdateItemsResponse {
        operations.add(item to UpdateItemOperation.REMOVE)
        return this
    }

    operator fun plus(other: UpdateItemsResponse): UpdateItemsResponse {
        this.operations.addAll(other.operations)
        return this
    }

    override val data by lazy {
        littleEndianByteArray {
            putUByte(UPDATE_ITEM_RESPONSE_PACKET_ID)
            putShort(operations.size.toShort())

            operations.forEach {
                putShort(it.operation.id.toShort())
                putShort(it.item.group.id.toShort())
                putInt(it.item.id)
                putInt(it.item.templateId)
                putInt(it.item.amount)
                putShort(it.item.category.id.toShort())
                putShort(0)
                putShort(if (it.item.isEquipped) 1 else 0)
                putInt(it.item.type.availableSlots.firstOrNull()?.id ?: 0)
                putShort(it.item.enchantLevel.toShort())
                putShort(0) // Custom Type 2 (?)
                putInt(it.item.augmentationId)
                putInt(0) //TODO Mana (of shadow item)
            }
        }
    }

}

enum class UpdateItemOperation(val id: Int) {
    ADD(1),
    MODIFY(2),
    REMOVE(3)
}

inline val Pair<ItemInstance, UpdateItemOperation>.item: ItemInstance get() = this.first
inline val Pair<ItemInstance, UpdateItemOperation>.operation: UpdateItemOperation get() = this.second
