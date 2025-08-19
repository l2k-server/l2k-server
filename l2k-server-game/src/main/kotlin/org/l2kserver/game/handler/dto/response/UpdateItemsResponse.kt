package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.item.instance.ItemInstance

private const val UPDATE_ITEM_RESPONSE_PACKET_ID: UByte = 39u

/**
 * Response to notify client about changes in inventory
 *
 * @property operations List of item updates - [ItemInstance] to [UpdateItemOperationType]
 */
data class UpdateItemsResponse(
    val operations: List<UpdateItemOperation>
): ResponsePacket {

    @JvmInline
    value class Builder private constructor(
        private val operations: MutableList<UpdateItemOperation>
    ) {
        constructor(): this(mutableListOf())

        fun build() = UpdateItemsResponse(this.operations)

        fun operationAdd(item: ItemInstance): Builder {
            operations.add(UpdateItemOperation(item, UpdateItemOperationType.ADD))
            return this
        }

        fun operationModify(item: ItemInstance): Builder {
            operations.add(UpdateItemOperation(item, UpdateItemOperationType.MODIFY))
            return this
        }

        fun operationDelete(item: ItemInstance): Builder {
            operations.add(UpdateItemOperation(item, UpdateItemOperationType.REMOVE))
            return this
        }

        operator fun plus(other: Builder): Builder {
            this.operations.addAll(other.operations)
            return this
        }
    }

    companion object {
        fun operationAdd(item: ItemInstance) = UpdateItemsResponse(item to UpdateItemOperationType.ADD)
        fun operationModify(item: ItemInstance) = UpdateItemsResponse(item to UpdateItemOperationType.MODIFY)
        fun operationRemove(item: ItemInstance) = UpdateItemsResponse(item to UpdateItemOperationType.REMOVE)
    }

    constructor(vararg updatedItemPairs: Pair<ItemInstance, UpdateItemOperationType>): this(
        updatedItemPairs.asList().map { UpdateItemOperation(it.first, it.second) }
    )

    override val data = littleEndianByteArray {
        putUByte(UPDATE_ITEM_RESPONSE_PACKET_ID)
        putShort(operations.size.toShort())

        operations.forEach {
            putShort(it.operationType.id.toShort())
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

data class UpdateItemOperation(val item: ItemInstance, val operationType: UpdateItemOperationType)

enum class UpdateItemOperationType(val id: Int) {
    ADD(1),
    MODIFY(2),
    REMOVE(3)
}
