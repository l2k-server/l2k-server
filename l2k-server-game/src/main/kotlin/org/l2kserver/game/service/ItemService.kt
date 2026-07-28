package org.l2kserver.game.service

import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.l2kserver.game.configuration.properties.EnchantProperties
import org.l2kserver.game.extensions.forEachInstanceMatching
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.extensions.model.item.canBeEnchantedBy
import org.l2kserver.game.extensions.model.item.toScatteredItem
import org.l2kserver.game.handler.dto.request.AutoUseSsRequest
import org.l2kserver.game.handler.dto.request.DeleteItemRequest
import org.l2kserver.game.handler.dto.request.DropItemRequest
import org.l2kserver.game.handler.dto.request.EnchantRequest
import org.l2kserver.game.handler.dto.request.TakeOffItemRequest
import org.l2kserver.game.handler.dto.request.UseItemRequest
import org.l2kserver.game.handler.dto.response.ActionFailedResponse
import org.l2kserver.game.handler.dto.response.AutoUseSsResponse
import org.l2kserver.game.handler.dto.response.ChooseItemToEnchantResponse
import org.l2kserver.game.handler.dto.response.CloseChooseItemToEnchantResponse
import org.l2kserver.game.handler.dto.response.DeleteObjectResponse
import org.l2kserver.game.handler.dto.response.FullCharacterResponse
import org.l2kserver.game.handler.dto.response.DroppedItemResponse
import org.l2kserver.game.handler.dto.response.NpcChatWindowResponse
import org.l2kserver.game.handler.dto.response.PickUpItemResponse
import org.l2kserver.game.handler.dto.response.ShotUsedResponse
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.handler.dto.response.UpdateItemsResponse
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.PlayerCharacterInstanceImpl
import org.l2kserver.game.model.actor.ScatteredItem
import org.l2kserver.game.model.item.ArmorInstance
import org.l2kserver.game.model.item.ArrowInstanceImpl
import org.l2kserver.game.model.item.BookInstanceImpl
import org.l2kserver.game.model.item.EnchantScrollInstanceImpl
import org.l2kserver.game.model.item.MagicItemInstanceImpl
import org.l2kserver.game.model.item.EquippableItemInstanceImpl
import org.l2kserver.game.model.item.Grade
import org.l2kserver.game.model.item.SoulshotInstanceImpl
import org.l2kserver.game.model.item.SpiritshotInstanceImpl
import org.l2kserver.game.model.item.ItemInstance
import org.l2kserver.game.model.item.ItemInstanceImpl
import org.l2kserver.game.model.item.Slot
import org.l2kserver.game.model.item.WeaponInstanceImpl
import org.l2kserver.game.model.item.ShotInstance
import org.l2kserver.game.model.item.SoulshotInstance
import org.l2kserver.game.model.item.SpiritshotInstance
import org.l2kserver.game.model.item.ItemRegistry
import org.l2kserver.game.model.item.JewelryInstance
import org.l2kserver.game.model.item.WeaponInstance
import org.l2kserver.game.model.item.WeaponType
import org.l2kserver.game.model.reward.RewardItem
import org.l2kserver.game.model.store.PrivateStore
import org.l2kserver.game.utils.withChance
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sendTo
import org.l2kserver.game.network.session.sessionContext
import org.l2kserver.game.repository.GameObjectRepository
import org.springframework.stereotype.Service
import org.springframework.context.annotation.Lazy
import java.util.concurrent.ConcurrentHashMap
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
    @param:Lazy private val skillService: SkillService,
    private val idGenerationService: IdGenerationService,
    private val enchantProperties: EnchantProperties,

    override val gameObjectRepository: GameObjectRepository
) : AbstractService() {

    override val log = logger()

    /**
     * Opened enchant sessions. Key - character id, value - scroll item id
     */
    private val enchantSessions = ConcurrentHashMap<Int, Int>()

    /** Handles request to toggle ss auto usage */
    suspend fun toggleAutoUseSs(request: AutoUseSsRequest) {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())

        val shot = character.inventory.findAllByTemplateId(request.ssTemplateId).firstOrNull() as? ShotInstance ?: run {
            log.warn { "Character does not have item " +
                    "with template id='${request.ssTemplateId}', or it is not a soul- or spiritshot" }
            return
        }

        when(shot) {
            is SoulshotInstance -> toggleSoulshotAutoUsage(character, shot)
            is SpiritshotInstance -> toggleSpiritshotAutoUsage(character, shot)
        }
    }

    /** Handles request to use item */
    suspend fun useItem(request: UseItemRequest) {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        val item = character.inventory.findByIdOrNull(request.itemId) ?: run {
            log.warn { "No item with id='${request.itemId}' was found in $character's inventory" }
            return
        }

        log.info { "'$character' tries to use item '$item'" }

        when {
            character.isDead() -> {
                log.debug { "$character is dead and cannot use items" }
                send { SystemMessageResponse.ItemCannotBeUsed(item) }
                send { ActionFailedResponse }
                return
            }
            (character.privateStore as? PrivateStore.Sell)?.items?.contains(item.id) == true -> {
                log.debug { "$item is in $character's private store and cannot be used" }
                send { SystemMessageResponse.ItemCannotBeUsed(item) }
                send { ActionFailedResponse }
                return
            }
            item is EquippableItemInstanceImpl -> equipOrDisarmItem(character, item)
            item is SoulshotInstanceImpl -> useSoulshot(character, item)
            item is SpiritshotInstanceImpl -> useSpiritshot(character, item)
            item is BookInstanceImpl -> useBook(item)
            item is MagicItemInstanceImpl -> useMagicItem(character, item)
            item is EnchantScrollInstanceImpl -> {
                enchantSessions[character.id] = item.id
                send { SystemMessageResponse.SelectItemToEnchant }
                send { ChooseItemToEnchantResponse(item.templateId) }
            }
        }
    }

    /** Handles request to take off item */
    suspend fun takeOffItem(request: TakeOffItemRequest) {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        val item = character.inventory[request.slot] ?: run {
            log.warn { "Character has no item equipped at slot ${request.slot}" }
            return
        }

        log.debug { "'$character' tries to take off item $item" }
        equipOrDisarmItem(character, item)
        log.info { "'$character' has successfully taken off item $item" }
    }

    /** Handles request to delete item */
    suspend fun deleteItem(request: DeleteItemRequest) {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        val item = character.inventory.findById(request.itemId)
        log.debug { "'$character' tries to delete '${request.amount}' items '$item'" }

        when {
            character.privateStore != null -> {
                log.debug { "$character holds private store and cannot delete items" }
                send { SystemMessageResponse.CannotDiscardDestroyOrTradeWhileInShop }
                send { ActionFailedResponse }
                return
            }
            !item.isDestroyable || enchantSessions[character.id] == item.id -> {
                log.debug { "'$character' tried to delete undeletable item '$item'" }
                send { SystemMessageResponse.CannotDiscardItem }
                return
            }
            else -> {
                deleteItem(item, request.amount, character)
                log.info { "'$character' has deleted item '$item'" }
            }
        }
    }

    /** Handles request to drop item on the ground */
    suspend fun dropItem(request: DropItemRequest) {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        val item = character.inventory.findById(request.itemId)

        log.debug { "'$character' tries to drop '${request.amount}' items '$item'" }

        when {
            character.privateStore != null -> {
                log.debug { "$character holds private store and cannot drop items" }
                send { SystemMessageResponse.CannotDiscardDestroyOrTradeWhileInShop }
                send { ActionFailedResponse }
                return
            }
            !item.isDroppable || enchantSessions[character.id] == item.id-> {
                send { SystemMessageResponse.CannotDiscardItem }
                return
            }
            !character.position.isCloseTo(request.position, DROP_DISTANCE) -> {
                send { SystemMessageResponse.TooFarToDiscard }
                return
            }
            !item.isStackable && request.amount > 1 -> {
                throw IllegalArgumentException(
                    "'${character}' tried to drop '${request.amount}' non-stackable items '$item')!"
                )
            }
            request.amount > item.amount -> {
                send { SystemMessageResponse.NotEnoughItems }
                return
            }
            else -> {
                val scatteredItemPosition = geoDataService.getAvailableTargetPosition(
                    character.position, request.position
                )
                val scatteredItemId = idGenerationService.next()
                val scatteredItem = gameObjectRepository.save(
                    item.toScatteredItem(
                        id = scatteredItemId,
                        position = scatteredItemPosition,
                        amount = request.amount
                    )
                )
                this@ItemService.broadcastAround(character.position) {
                    DroppedItemResponse(character.id, scatteredItem)
                }
                deleteItem(item, request.amount, character)
                log.info { "'$character' has dropped item '$item'" }
            }
        }
    }

    suspend fun enchantItem(request: EnchantRequest) {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        if (request.itemId == -1) clearEnchantSession(character)

        log.debug { "'$character' tries to enchant item with id='${request.itemId}'" }

        val item = character.inventory.findByIdOrNull(request.itemId) ?: run {
            log.debug { "'$character has no item with id='${request.itemId} in inventory" }
            return
        }

        val enchantScroll = enchantSessions[character.id]
            ?.let { character.inventory.findByIdOrNull(it) as? EnchantScrollInstanceImpl }
            ?: run {
                log.warn { "'$character has no scroll registered to enchant '$item'" }
                send { SystemMessageResponse.InappropriateEnchantConditions }
                return
            }

        if (!item.canBeEnchantedBy(enchantScroll)) {
            log.debug { "'$character' tries to enchant '$item', that cannot be enchanted by '$enchantScroll'" }
            send { SystemMessageResponse.InappropriateEnchantConditions }
            return
        }

        val chancePercents = when {
            item is WeaponInstance -> {
                if (item.isMagicWeapon && item.grade > Grade.D)
                    enchantProperties.magicWeaponChance.floorEntry(item.enchantLevel)?.value
                else
                    enchantProperties.weaponChance.floorEntry(item.enchantLevel)?.value
            }
            (item is ArmorInstance || item is JewelryInstance) -> {
                enchantProperties.armorChance.floorEntry(item.enchantLevel)?.value
            }
            else -> null
        }

        if (chancePercents == null) {
            log.debug { "'$character' tries to enchant non-enchantable '$item'" }
            send { SystemMessageResponse.InappropriateEnchantConditions }
            return
        }

        suspendTransaction {
            deleteItem(enchantScroll, 1, character)

            withChance(chancePercents/100) {
                item.enchantLevel++
                send { UpdateItemsResponse().wasModified(item) }
                send { SystemMessageResponse.YourItemHasBeenSuccessfullyEnchanted(item) }

                // broadcast updated glow
                if (item is WeaponInstanceImpl && item.isEquipped) broadcastActorInfo(character)
                log.debug { "'$character' has successfully enchanted $item" }
                return@suspendTransaction
            }

            //If enchantment failed
            if (enchantScroll.isBlessed) {
                item.enchantLevel = 0
                send { UpdateItemsResponse().wasModified(item) }
                send { SystemMessageResponse.BlessedEnchantmentFailed }
            }
            else {
                send { SystemMessageResponse.EnchantmentFailed(item) }
                deleteItem(item, 1, character)
                //TODO Item Crystallization https://github.com/l2k-server/l2k-server/issues/130
            }
        }
        send { CloseChooseItemToEnchantResponse }
    }

    /** Cancels enchant session and notifies client (if necessary) */
    suspend fun clearEnchantSession(character: PlayerCharacterInstanceImpl) {
        log.debug { "'$character' has quit enchant session" }

        enchantSessions.remove(character.id)?.let {
            send { SystemMessageResponse.EnchantmentCancelled }
            send { CloseChooseItemToEnchantResponse }
        }
    }

    /**
     * Transforms [item] to ScatteredItem dropped in
     * random position in [DROP_REWARD_DISTANCE] radius and drops it by [dropper]
     */
    suspend fun dropRewardItem(item: RewardItem, dropper: ActorInstance) {
        val template = ItemRegistry.findByIdOrNull(item.templateId) ?: run {
            log.warn { "No item template found by id $item.id" }
            return
        }

        val scatteredItemsAmount = if (template.isStackable) 1 else item.amount.random()
        val itemsInStackAmount = if (template.isStackable) item.amount.random() else 1

        val scatteredItems = List(scatteredItemsAmount) {
            val dropX = ((dropper.position.x - DROP_REWARD_DISTANCE)..(dropper.position.x + DROP_REWARD_DISTANCE))
                .random()
            val dropY = ((dropper.position.y - DROP_REWARD_DISTANCE)..(dropper.position.y + DROP_REWARD_DISTANCE))
                .random()

            val calculatedPosition = Position(dropX, dropY, dropper.position.z)
            val dropPosition = geoDataService.getAvailableTargetPosition(
                dropper.position, calculatedPosition)
            val id = idGenerationService.next()

            item.toScatteredItem(id, dropPosition, itemsInStackAmount)?.let { gameObjectRepository.save(it) }
        }.filterNotNull()

        scatteredItems.forEach { scatteredItem ->
            this@ItemService.broadcastAround(dropper.position) {
                DroppedItemResponse(dropper.id, scatteredItem)
            }
        }
    }

    /** Picks up [scatteredItem] by [character] */
    suspend fun pickUp(character: PlayerCharacterInstanceImpl, scatteredItem: ScatteredItem) {
        log.debug { "Start picking up item '$scatteredItem' by '$character'" }

        val enoughCloseToPickUp = character.position.isCloseTo(
            scatteredItem.position,
            character.collisionBox.radius.roundToInt() + Position.GEO_CELL_SIZE
        )

        if (!enoughCloseToPickUp) {
            send { ActionFailedResponse }
            return
        }

        //TODO Binding item on being dropped to it's owner
        //TODO Checks if player can pick up this item
        val deletedScatteredItem = gameObjectRepository.delete(scatteredItem) ?: run {
            send { ActionFailedResponse }
            return
        }
        idGenerationService.release(deletedScatteredItem.id)

        broadcastAround(character.position) { PickUpItemResponse(character.id, deletedScatteredItem) }
        broadcastAround(character.position) { DeleteObjectResponse(deletedScatteredItem.id) }

        giveItem(
            itemReceiver = character,
            itemTemplateId = deletedScatteredItem.templateId,
            amount = deletedScatteredItem.amount,
            enchantLevel = deletedScatteredItem.enchantLevel,
        ).forEach { item ->
            broadcastAround(character) {
                SystemMessageResponse.AttentionPlayerPickedUp(character.name, item)
            }
        }
    }

    /** Creates new item(s) in [itemReceiver]'s inventory */
    suspend fun giveItem(
        itemReceiver: PlayerCharacterInstanceImpl, itemTemplateId: Int, amount: Int, enchantLevel: Int = 0
    ) = suspendTransaction {
        val existingItem = itemReceiver.inventory.findAllByTemplateId(itemTemplateId).firstOrNull()
        val itemTemplate = ItemRegistry.findById(itemTemplateId)

        val consumableId = itemReceiver.inventory.weapon?.consumes?.templateId
        val equippedAt = if (consumableId == itemTemplate.id) Slot.LEFT_HAND else null

        val items = if (itemTemplate.isStackable) {
            if (existingItem == null) {
                val newItem = itemReceiver.inventory.createItem(
                    id = idGenerationService.next(),
                    templateId = itemTemplateId,
                    amount = amount,
                    equippedAt = equippedAt,
                    enchantLevel = enchantLevel
                )
                sendTo(itemReceiver.id) { UpdateItemsResponse().wasAdded(newItem) }
                listOf(newItem)
            }
            else {
                existingItem.amount += amount
                sendTo(itemReceiver.id) { UpdateItemsResponse().wasModified(existingItem) }
                listOf(existingItem)
            }
        }
        else List(amount) {
            val newItem = itemReceiver.inventory.createItem(
                id = idGenerationService.next(),
                templateId = itemTemplateId,
                amount = 1,
                equippedAt = equippedAt,
                enchantLevel = enchantLevel
            )
            sendTo(itemReceiver.id) { UpdateItemsResponse().wasAdded(newItem) }
            newItem
        }

        send { UpdateStatusResponse.weightOf(itemReceiver) }
        items.forEach { sendTo(itemReceiver.id) { SystemMessageResponse.YouHaveObtained(it) }}

        log.info { "'$itemReceiver' has received $amount of '${itemTemplate.name}'" }

        commit()
        return@suspendTransaction items
    }

    /**
     * Deletes this item and notifies players about it
     *
     * @param item Item to delete
     * @param amount Amount of items to delete
     * @param owner Owner of this [item]
     */
    suspend fun deleteItem(
        item: ItemInstanceImpl,
        amount: Int,
        owner: PlayerCharacterInstanceImpl
    ) = suspendTransaction {
        require(item.isStackable || amount == 1) {
            "Cannot remove '$amount' of non-stackable '$item' of '${owner}'!"
        }

        if (amount > item.amount) {
            sendTo(owner.id) { SystemMessageResponse.NotEnoughItems }
            return@suspendTransaction
        }

        if (amount < item.amount) {
            item.amount -= amount
            sendTo(owner.id) { UpdateItemsResponse().wasModified(item) }
        } else {
            val response = UpdateItemsResponse()
            if (item is EquippableItemInstanceImpl && item.isEquipped) {
                owner.inventory.disarmItem(item)
                owner.inventory
                response.wasModified(item)
                val consumableId = (item as? WeaponInstanceImpl)?.consumes?.templateId
                if (consumableId != null) {
                    val arrow = owner.inventory.findAllByTemplateId(consumableId).firstOrNull() as? ArrowInstanceImpl
                    arrow?.let {
                        it.equippedAt = null
                        response.wasModified(it)
                    }
                }
                broadcastActorInfo(owner)
            }
            response.wasDeleted(item)
            sendTo(owner.id) { response }
            owner.inventory.delete(item)
            idGenerationService.release(item.id)
        }

        sendTo(owner.id) { UpdateStatusResponse.weightOf(owner) }
    }

    private suspend fun toggleSoulshotAutoUsage(
        character: PlayerCharacterInstanceImpl, soulshot: SoulshotInstance
    ) {
        val weapon = character.inventory.weapon ?: run {
            send { SystemMessageResponse.CannotUseSoulshot }
            return
        }

        if (!weapon.canUseSoulshot(soulshot)) return

        character.autoUsesSoulshot?.let {
            send { SystemMessageResponse.AutomaticUseDeactivated(it) }
            send { AutoUseSsResponse(it.templateId, enabled = false) }

            character.autoUsesSoulshot = null
            if (it == soulshot) return
        }

        useSoulshot(character, soulshot)

        if (weapon.canUseSoulshot(soulshot)) {
            character.autoUsesSoulshot = soulshot
            send { SystemMessageResponse.AutomaticUseActivated(soulshot) }
            send { AutoUseSsResponse(soulshot.templateId, enabled = true) }
        }
    }

    private suspend fun toggleSpiritshotAutoUsage(
        character: PlayerCharacterInstanceImpl, spiritshot: SpiritshotInstance
    ) {
        val weapon = character.inventory.weapon ?: run {
            send { SystemMessageResponse.CannotUseSoulshot }
            return
        }

        if (!weapon.canUseSpiritshot(spiritshot)) return

        character.autoUsesSpiritshot?.let {
            send { SystemMessageResponse.AutomaticUseDeactivated(it) }
            send { AutoUseSsResponse(it.templateId, enabled = false) }

            character.autoUsesSpiritshot = null
            if (it == spiritshot) return
        }

        useSpiritshot(character, spiritshot)

        if (weapon.canUseSpiritshot(spiritshot)) {
            character.autoUsesSpiritshot = spiritshot
            send { SystemMessageResponse.AutomaticUseActivated(spiritshot) }
            send { AutoUseSsResponse(spiritshot.templateId, enabled = true) }
        }
    }

    /**
     * Charges [soulshot] to equipped weapon  of [character]
     *
     * @return `true` if soulshot was successfully charged, `false` if not
     */
    suspend fun useSoulshot(character: PlayerCharacterInstanceImpl, soulshot: SoulshotInstance) {
        val weapon = character.inventory.weapon ?: run {
            send { SystemMessageResponse.CannotUseSoulshot }
            return
        }

        if (!weapon.canUseSoulshot(soulshot) || weapon.soulshotCharged) return

        val reducedSoulshot = character.inventory.reduceAmount(soulshot.id, weapon.soulshotUsed)

        weapon.soulshotCharged = true
        send { SystemMessageResponse.SoulshotEnabled }

        if (weapon.soulshotUsed > (reducedSoulshot?.amount ?: 0)) character.autoUsesSoulshot?.let {
            send { SystemMessageResponse.AutomaticUseDeactivated(it) }
            send { AutoUseSsResponse(it.templateId, enabled = false) }
            character.autoUsesSoulshot = null
        }

        if (reducedSoulshot == null ) send { UpdateItemsResponse().wasDeleted(soulshot) }
        else send { UpdateItemsResponse().wasModified(soulshot) }

        this@ItemService.broadcastAround(character.position) { ShotUsedResponse(character, soulshot) }
    }

    suspend fun useSpiritshot(character: PlayerCharacterInstanceImpl, spiritshot: SpiritshotInstance) {
        val weapon = character.inventory.weapon ?: run {
            send { SystemMessageResponse.CannotUseSpiritshot }
            return
        }

        if (!weapon.canUseSpiritshot(spiritshot) || weapon.spiritshotChargedType != null) return

        val reducedSpiritshot = character.inventory.reduceAmount(spiritshot.id, weapon.soulshotUsed)

        weapon.spiritshotChargedType = spiritshot.spiritshotType
        send { SystemMessageResponse.SpiritshotEnabled }

        if (weapon.spiritshotUsed > (reducedSpiritshot?.amount ?: 0)) character.autoUsesSpiritshot?.let {
            send { SystemMessageResponse.AutomaticUseDeactivated(it) }
            send { AutoUseSsResponse(it.templateId, enabled = false) }
            character.autoUsesSpiritshot = null
        }

        if (reducedSpiritshot == null ) send { UpdateItemsResponse().wasDeleted(spiritshot) }
        else send { UpdateItemsResponse().wasModified(spiritshot) }

        this@ItemService.broadcastAround(character.position) { ShotUsedResponse(character, spiritshot) }
    }

    suspend fun useBook(book: BookInstanceImpl) = send {
        NpcChatWindowResponse(npcId = book.templateId, message = book.text)
    }

    /**
     * Uses magic item (scroll, potion, etc.) to cast its skill
     * The item will be consumed immediately when skill casting starts
     */
    suspend fun useMagicItem(character: PlayerCharacterInstanceImpl, magicItem: MagicItemInstanceImpl) {
        log.info { "'$character' tries to use magic item '$magicItem'" }
        // Create skill instance with character's ID for individual cooldowns
        val skill = magicItem.createSkill(character.id)

        // Use the skill from the item
        skillService.useSkill(character, skill)
    }

    /**
     * Equip (or take off) item
     *
     * @param character Character, that tries to equip/take off item
     * @param item Item, that will be equipped/taken off
     */
    @Suppress("NestedBlockDepth", "CyclomaticComplexMethod") //TODO Refactor?
    private suspend fun equipOrDisarmItem(
        character: PlayerCharacterInstanceImpl,
        item: EquippableItemInstanceImpl
    ) {
        val updatedItems = ArrayList<ItemInstance>(3)
        suspendTransaction {
            val paperDoll = character.inventory

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

            character.autoUsesSoulshot?.let {
                if (item is WeaponInstanceImpl && !item.canUseSoulshot(character.autoUsesSoulshot)) {
                    send { AutoUseSsResponse(it.templateId, false) }
                    character.autoUsesSoulshot = null
                }
            }

            character.autoUsesSpiritshot?.let {
                if (item is WeaponInstanceImpl && !item.canUseSpiritshot(character.autoUsesSpiritshot)) {
                    send { AutoUseSsResponse(it.templateId, false) }
                    character.autoUsesSpiritshot = null
                }
            }

            updatedItems += equipAndDisarmArrows(updatedItems, character)
        }

        suspendTransaction {
            updatedItems.forEach {
                if (it.isEquipped) {
                    //CRUTCH: Server must send
                    //SystemMessage -> CharacterResponse -> UpdatedItemResponse -> CharacterResponse
                    //otherwise jewelry sucks
                    send { SystemMessageResponse.EquipItem(it) }
                    send { FullCharacterResponse(character) }
                } else send { SystemMessageResponse.DisarmItem(it) }
            }


            send {
                val response = UpdateItemsResponse()
                updatedItems.forEach { response.wasModified(it) }

                response
            }

            broadcastActorInfo(character)
            log.info { "'$character' has equipped item '$item'" }
            //TODO Recalculate skillList
        }
    }

    private suspend fun equipAndDisarmArrows(
        updatedItems: ArrayList<ItemInstance>, character: PlayerCharacterInstanceImpl
    ): List<ItemInstance> = buildList(2) {
        updatedItems.forEachInstanceMatching<WeaponInstanceImpl>({ it.type == WeaponType.BOW }) { bow ->
            if (bow.isEquipped) bow.consumes?.let {
                character.inventory.findAllByTemplateId(it.templateId).firstOrNull()
                    ?.let { consumable -> if (consumable is ArrowInstanceImpl) {
                        consumable.equippedAt = Slot.LEFT_HAND
                        add(consumable)
                    }}
            }
            else bow.consumes?.let {
                character.inventory.findAllByTemplateId(it.templateId).firstOrNull()
                    ?.let { consumable -> if (consumable is ArrowInstanceImpl) {
                        consumable.equippedAt = null
                        add(consumable)
                    }}
            }
        }
    }

    private suspend fun WeaponInstanceImpl.canUseSoulshot(soulshot: SoulshotInstance?) = when {
        this.soulshotUsed > (soulshot?.amount ?: 0) -> {
            send { SystemMessageResponse.NotEnoughSoulshots }
            false
        }
        this.grade != soulshot?.grade -> {
            send { SystemMessageResponse.SoulshotGradeMismatch }
            false
        }
        else -> true
    }

    private suspend fun WeaponInstanceImpl.canUseSpiritshot(spiritshot: SpiritshotInstance?) = when {
        this.spiritshotUsed > (spiritshot?.amount ?: 0) -> {
            send { SystemMessageResponse.NotEnoughSpiritshots }
            false
        }
        this.grade != spiritshot?.grade -> {
            send { SystemMessageResponse.SpiritshotGradeMismatch }
            false
        }
        else -> true
    }

}
