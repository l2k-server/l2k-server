package org.l2kserver.game.service

import kotlinx.coroutines.isActive
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.l2kserver.game.extensions.forEachInstanceMatching
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.extensions.model.item.delete
import org.l2kserver.game.extensions.model.item.findAllByOwnerIdAndTemplateId
import org.l2kserver.game.extensions.model.item.toItem
import org.l2kserver.game.extensions.model.item.toScatteredItem
import org.l2kserver.game.handler.dto.request.DeleteItemRequest
import org.l2kserver.game.handler.dto.request.DropItemRequest
import org.l2kserver.game.handler.dto.request.TakeOffItemRequest
import org.l2kserver.game.handler.dto.request.UseItemRequest
import org.l2kserver.game.handler.dto.response.ActionFailedResponse
import org.l2kserver.game.handler.dto.response.DeleteObjectResponse
import org.l2kserver.game.handler.dto.response.FullCharacterResponse
import org.l2kserver.game.handler.dto.response.DroppedItemResponse
import org.l2kserver.game.handler.dto.response.PickUpItemResponse
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.handler.dto.response.UpdateItemOperation
import org.l2kserver.game.handler.dto.response.UpdateItemOperationType
import org.l2kserver.game.handler.dto.response.UpdateItemsResponse
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.Actor
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.actor.ScatteredItem
import org.l2kserver.game.model.item.Arrow
import org.l2kserver.game.model.item.EquippableItem
import org.l2kserver.game.model.item.Item
import org.l2kserver.game.model.item.ItemTemplate
import org.l2kserver.game.model.item.Slot
import org.l2kserver.game.model.item.UsableItem
import org.l2kserver.game.model.item.Weapon
import org.l2kserver.game.model.item.WeaponType
import org.l2kserver.game.model.reward.RewardItem
import org.l2kserver.game.model.store.PrivateStore
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sendTo
import org.l2kserver.game.network.session.sessionContext
import org.l2kserver.game.repository.GameObjectDAO
import org.springframework.stereotype.Service
import kotlin.math.roundToInt
import kotlin.ranges.random

private const val DROP_DISTANCE = 150
private const val DROP_REWARD_DISTANCE = 25

/**
 * This service works with items, like using, wearing, dropping, creating, etc.
 */
@Service
class ItemService(
    private val geoDataService: GeoDataService,
    private val asyncTaskService: AsyncTaskService,
    private val moveService: MoveService,

    override val gameObjectDAO: GameObjectDAO,
) : AbstractService() {

    override val log = logger()

    /**
     * Handles request to use item
     */
    suspend fun useItem(request: UseItemRequest) {
        val (character, item) = getItemAndOwner(request.itemId)
        log.info("Character '{}' tries to use item '{}'", character.name, item)

        when {
            character.isDead() -> {
                log.debug("{} is dead and cannot use items", character)
                send(SystemMessageResponse.ItemCannotBeUsed(item), ActionFailedResponse)
                return
            }
            (character.privateStore as? PrivateStore.Sell)?.items?.contains(item.id) == true -> {
                log.debug("{} is in {}'s private store and cannot be used", item, character)
                send(SystemMessageResponse.ItemCannotBeUsed(item), ActionFailedResponse)
                return
            }
            item is EquippableItem -> equipOrDisarmItem(character, item)
            item is UsableItem -> {
                //TODO https://github.com/l2kserver/l2kserver-game/issues/29
                send(SystemMessageResponse("Using items is not implemented yet"), ActionFailedResponse)
                return
            }
        }
    }

    /**
     * Handles request to take off item
     */
    suspend fun takeOffItem(request: TakeOffItemRequest) {
        val character = gameObjectDAO.findCharacterById(sessionContext().getCharacterId())
        val item = checkNotNull(character.paperDoll[request.slot]) {
            "Character has no item equipped at slot ${request.slot}"
        }

        log.debug("Player '{}' tries to take off item {}", character.name, item)
        equipOrDisarmItem(character, item)
        log.info("Player '{}' has successfully taken off item {}", character.name, item)
    }

    /**
     * Handles request to delete item
     */
    suspend fun deleteItem(request: DeleteItemRequest) {
        val (character, item) = getItemAndOwner(request.itemId)
        log.debug("'{}' tries to delete '{}' items '{}'", character, request.amount, item)

        when {
            character.privateStore != null -> {
                log.debug("{} holds private store and cannot delete items", character)
                send(SystemMessageResponse.CannotDiscardDestroyOrTradeWhileInShop, ActionFailedResponse)
                return
            }
            !item.isDestroyable -> {
                log.debug("Character '{}' tried to delete undeletable item '{}'", character.name, item)
                send(SystemMessageResponse.CannotDiscardItem)
                return
            }
            else -> {
                deleteItem(item, request.amount, character)
                log.info("Character '{}' has deleted item '{}'", character.name, item)
            }
        }
    }

    suspend fun dropItem(request: DropItemRequest) {
        val (character, item) = getItemAndOwner(request.itemId)
        log.debug("'{}' tries to drop '{}' items '{}'", character, request.amount, item)

        when {
            character.privateStore != null -> {
                log.debug("{} holds private store and cannot drop items", character)
                send(SystemMessageResponse.CannotDiscardDestroyOrTradeWhileInShop, ActionFailedResponse)
                return
            }
            !item.isDroppable -> {
                send(SystemMessageResponse.CannotDiscardItem)
                return
            }
            !character.position.isCloseTo(request.position, DROP_DISTANCE) -> {
                send(SystemMessageResponse.TooFarToDiscard)
                return
            }
            !item.isStackable && request.amount > 1 -> {
                throw IllegalArgumentException("'${character}' tried to drop '${request.amount}' non-stackable items '$item')!")
            }
            request.amount > item.amount -> {
                send(SystemMessageResponse.NotEnoughItems)
                return
            }
            else -> {
                val scatteredItemPosition = geoDataService.getAvailableTargetPosition(character.position, request.position)
                val scatteredItem = gameObjectDAO.save(item.toScatteredItem(scatteredItemPosition, request.amount))!!
                broadcastPacket(DroppedItemResponse(character.id, scatteredItem), position = character.position)
                deleteItem(item, request.amount, character)
                log.info("Character '{}' has dropped item '{}'", character.name, item)
            }
        }
    }

    /**
     * Transforms [item] to ScatteredItem dropped in
     * random position in [DROP_REWARD_DISTANCE] radius and drops it by [dropper]
     */
    suspend fun dropRewardItem(item: RewardItem, dropper: Actor) {
        val template = ItemTemplate.Registry.findById(item.id) ?: run {
            log.warn("No item template found by id {}", item.id)
            return
        }

        val scatteredItemsAmount = if (template.isStackable) 1 else item.amount.random()
        val itemsInStackAmount = if (template.isStackable) item.amount.random() else 1

        val scatteredItems = List(scatteredItemsAmount) {
            val dropX = ((dropper.position.x - DROP_REWARD_DISTANCE)..(dropper.position.x + DROP_REWARD_DISTANCE)).random()
            val dropY = ((dropper.position.y - DROP_REWARD_DISTANCE)..(dropper.position.y + DROP_REWARD_DISTANCE)).random()

            val calculatedPosition = Position(dropX, dropY, dropper.position.z)
            val dropPosition = geoDataService.getAvailableTargetPosition(dropper.position, calculatedPosition)

            gameObjectDAO.save(item.toScatteredItem(dropPosition, itemsInStackAmount))
        }.filterNotNull()

        scatteredItems.forEach { scatteredItem ->
            broadcastPacket(DroppedItemResponse(dropper.id, scatteredItem), position = dropper.position)
        }
    }

    /**
     * Moves [character] closer to [scatteredItem] and picks it up
     */
    suspend fun launchPickUp(
        character: PlayerCharacter, scatteredItem: ScatteredItem
    ) = asyncTaskService.launchAction(character.id) {
        moveService.move(character, scatteredItem)

        val enoughCloseToPickUp = character.position.isCloseTo(
            other = scatteredItem.position,
            distance = character.collisionBox.radius.roundToInt() + Position.ACCEPTABLE_DELTA
        )

        if (!isActive || !enoughCloseToPickUp) return@launchAction

        log.debug("Start picking up item '{}' by '{}'", scatteredItem, character.name)

        //TODO Binding item on being dropped to it's owner
        //TODO Checks if player can pick up this item
        val deletedScatteredItem = gameObjectDAO.delete(scatteredItem) ?: run {
            send(ActionFailedResponse)
            return@launchAction
        }

        broadcastPacket(PickUpItemResponse(character.id, deletedScatteredItem), character.position)
        broadcastPacket(DeleteObjectResponse(deletedScatteredItem.id), character.position)

        newSuspendedTransaction {
            val existingItem = Item.findAllByOwnerIdAndTemplateId(character.id, deletedScatteredItem.templateId).firstOrNull()
            val consumableId = character.paperDoll.getWeapon()?.consumes?.id

            val item = if (existingItem == null || !existingItem.isStackable) {
                val newItem = deletedScatteredItem.toItem(
                    character.id,
                    equippedAt = if (consumableId == deletedScatteredItem.templateId) Slot.LEFT_HAND else null
                )
                sendTo(character.id, UpdateItemsResponse.operationAdd(newItem))
                newItem
            }
            else {
                existingItem.amount += deletedScatteredItem.amount
                sendTo(character.id, UpdateItemsResponse.operationModify(existingItem))

                existingItem
            }
            send(UpdateStatusResponse.weightOf(character))
            broadcastPacket(SystemMessageResponse.AttentionPlayerPickedUp(character.name, item), character)
            send(SystemMessageResponse.YouHaveObtained(item))

            log.info("Character '{}' has picked up item '{}'", character.name, item)
        }
    }

    /**
     * Deletes this item and notifies players about it
     *
     * @param item Item to delete
     * @param amount Amount of items to delete
     * @param owner Owner of this [item]
     */
    suspend fun deleteItem(item: Item, amount: Int, owner: PlayerCharacter) = newSuspendedTransaction {
        require(item.isStackable || amount == 1) { "Cannot remove '$amount' of non-stackable '$item' of '${owner}'!" }

        if (amount > item.amount) {
            sendTo(owner.id, SystemMessageResponse.NotEnoughItems)
            return@newSuspendedTransaction
        }

        if (amount < item.amount) {
            item.amount -= amount
            sendTo(owner.id, UpdateItemsResponse.operationModify(item))
        } else {
            val responseBuilder = UpdateItemsResponse.Builder()
            if (item.isEquipped && item is EquippableItem) {
                owner.paperDoll.disarmItem(item)
                owner.paperDoll
                responseBuilder.operationModify(item)

                val consumableId = (item as? Weapon)?.consumes?.id
                if (consumableId != null) {
                    val arrow = Item.findAllByOwnerIdAndTemplateId(owner.id, consumableId).firstOrNull() as? Arrow
                    arrow?.let {
                        it.equippedAt = null
                        responseBuilder.operationModify(it)
                    }
                }
                broadcastActorInfo(owner)
            }
            responseBuilder.operationDelete(item)
            sendTo(owner.id, responseBuilder.build())
            item.delete()
        }

        sendTo(owner.id, UpdateStatusResponse.weightOf(owner))
    }

    /**
     * Equip (or take off) item
     *
     * @param playerCharacter Character, that tries to equip/take off item
     * @param item Item, that will be equipped/taken off
     */
    @Suppress("NestedBlockDepth") //TODO Refactor?
    private suspend fun equipOrDisarmItem(playerCharacter: PlayerCharacter, item: EquippableItem) {
        val updatedItems = ArrayList<Item>(3)
        newSuspendedTransaction {
            val paperDoll = playerCharacter.paperDoll

            if (item.isEquipped) updatedItems.add(paperDoll.disarmItem(item))
            else when {
                item.type.availableSlots.contains(Slot.TWO_HANDS) -> {
                    paperDoll[Slot.TWO_HANDS]?.let { updatedItems.add(paperDoll.disarmItem(it)) }
                    paperDoll[Slot.RIGHT_HAND]?.let { updatedItems.add(paperDoll.disarmItem(it)) }
                    paperDoll[Slot.LEFT_HAND]?.let { updatedItems.add(paperDoll.disarmItem(it)) }

                    updatedItems.add(paperDoll.equipItem(item, Slot.TWO_HANDS))
                }

                item.type.availableSlots.contains(Slot.RIGHT_HAND) -> {
                    paperDoll[Slot.TWO_HANDS]?.let { updatedItems.add(paperDoll.disarmItem(it)) }
                    paperDoll[Slot.RIGHT_HAND]?.let { updatedItems.add(paperDoll.disarmItem(it)) }

                    updatedItems.add(paperDoll.equipItem(item, Slot.RIGHT_HAND))
                }

                item.type.availableSlots.contains(Slot.LEFT_HAND) -> {
                    paperDoll[Slot.TWO_HANDS]?.let { updatedItems.add(paperDoll.disarmItem(it)) }
                    paperDoll[Slot.LEFT_HAND]?.let { updatedItems.add(paperDoll.disarmItem(it)) }

                    updatedItems.add(paperDoll.equipItem(item, Slot.LEFT_HAND))
                }

                item.type.availableSlots.contains(Slot.UPPER_AND_LOWER_BODY) -> {
                    paperDoll.upperBody?.let { updatedItems.add(paperDoll.disarmItem(it)) }
                    paperDoll.lowerBody?.let { updatedItems.add(paperDoll.disarmItem(it)) }

                    updatedItems.add(paperDoll.equipItem(item, Slot.UPPER_AND_LOWER_BODY))
                }

                item.type.availableSlots.contains(Slot.UPPER_BODY) -> {
                    paperDoll.upperBody?.let { updatedItems.add(paperDoll.disarmItem(it)) }
                    updatedItems.add(paperDoll.equipItem(item, Slot.UPPER_BODY))
                }

                item.type.availableSlots.contains(Slot.LOWER_BODY) -> {
                    paperDoll.upperBody?.let {
                        if (it.type.availableSlots.contains(Slot.UPPER_AND_LOWER_BODY))
                            updatedItems.add(paperDoll.disarmItem(it))
                    }
                    paperDoll.lowerBody?.let { updatedItems.add(paperDoll.disarmItem(it)) }

                    updatedItems.add(paperDoll.equipItem(item, Slot.LOWER_BODY))
                }

                item.type.availableSlots.contains(Slot.TWO_SLOT_ACCESSORY) -> {
                    paperDoll[Slot.FACE_ACCESSORY]?.let { updatedItems.add(paperDoll.disarmItem(it)) }
                    paperDoll[Slot.HAIR_ACCESSORY]?.let { updatedItems.add(paperDoll.disarmItem(it)) }
                    paperDoll[Slot.TWO_SLOT_ACCESSORY]?.let { updatedItems.add(paperDoll.disarmItem(it)) }

                    updatedItems.add(paperDoll.equipItem(item, Slot.TWO_SLOT_ACCESSORY))
                }

                item.type.availableSlots.contains(Slot.HAIR_ACCESSORY) -> {
                    paperDoll[Slot.HAIR_ACCESSORY]?.let { updatedItems.add(paperDoll.disarmItem(it)) }
                    paperDoll[Slot.TWO_SLOT_ACCESSORY]?.let { updatedItems.add(paperDoll.disarmItem(it)) }

                    updatedItems.add(paperDoll.equipItem(item, Slot.HAIR_ACCESSORY))
                }

                item.type.availableSlots.contains(Slot.FACE_ACCESSORY) -> {
                    paperDoll[Slot.FACE_ACCESSORY]?.let { updatedItems.add(paperDoll.disarmItem(it)) }
                    paperDoll[Slot.TWO_SLOT_ACCESSORY]?.let { updatedItems.add(paperDoll.disarmItem(it)) }

                    updatedItems.add(paperDoll.equipItem(item, Slot.FACE_ACCESSORY))
                }

                else -> for ((index, slot) in item.type.availableSlots.withIndex()) {
                    if (paperDoll[slot] == null) {
                        updatedItems.add(paperDoll.equipItem(item, slot))
                        break
                    } else if (index == item.type.availableSlots.size - 1) {
                        updatedItems.add(paperDoll.disarmItem(paperDoll[slot]!!))
                        updatedItems.add(paperDoll.equipItem(item, slot))
                    }
                }
            }
            updatedItems += equipAndDisarmArrows(updatedItems, playerCharacter.id)
        }

        newSuspendedTransaction {
            updatedItems.forEach {
                if (it.isEquipped) {
                    //CRUTCH: Server must send SystemMessage -> CharacterResponse -> UpdatedItemResponse -> CharacterResponse
                    //otherwise jewellery sucks
                    send(SystemMessageResponse.EquipItem(it))
                    send(FullCharacterResponse(playerCharacter))
                } else send(SystemMessageResponse.DisarmItem(it))
            }

            send(UpdateItemsResponse(updatedItems.map { UpdateItemOperation(it, UpdateItemOperationType.MODIFY) }))
            broadcastActorInfo(playerCharacter)
            log.info("Character '{}' has equipped item '{}'", playerCharacter.name, item)
            //TODO Recalculate skillList
        }
    }

    private suspend fun equipAndDisarmArrows(
        updatedItems: ArrayList<Item>, characterId: Int
    ): List<Item> = buildList(2) {
        updatedItems.forEachInstanceMatching<Weapon>({ it.type == WeaponType.BOW }) { bow ->
            if (bow.isEquipped) bow.consumes?.let {
                Item.findAllByOwnerIdAndTemplateId(characterId, it.id)
                    .filterIsInstance<Arrow>()
                    .firstOrNull()
                    ?.let { arrow -> add(arrow.toEquipped(Slot.LEFT_HAND)) }
            }
            else bow.consumes?.let {
                Item.findAllByOwnerIdAndTemplateId(characterId, it.id)
                    .filterIsInstance<Arrow>()
                    .firstOrNull()
                    ?.let { arrow -> add(arrow.toUnequipped()) }
            }
        }
    }

    /**
     * Loads character and requested item, and checks if player owns this item
     *
     * @param itemId Item id
     *
     * @return character and item
     */
    private suspend fun getItemAndOwner(itemId: Int): Pair<PlayerCharacter, Item> = newSuspendedTransaction {
        val context = sessionContext()
        val character = gameObjectDAO.findCharacterById(context.getCharacterId())
        val item = requireNotNull(Item.findById(itemId)) {
            "Player with characterId=${character.id} tried to use non-existing item!"
        }

        require(item.ownerId == character.id) {
            "Player ${context.getAccountName()} with character=$character tried to use someone else's item!"
        }

        Pair(character, item)
    }

}
