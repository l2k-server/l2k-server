package org.l2kserver.game.service

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.l2kserver.game.model.extensions.allUniqueBy
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.extensions.model.item.toItemInstance
import org.l2kserver.game.extensions.model.item.toItemInInventory
import org.l2kserver.game.extensions.model.item.toItemInWishList
import org.l2kserver.game.extensions.model.item.toItemOnSale
import org.l2kserver.game.extensions.model.store.subtractTradedItem
import org.l2kserver.game.extensions.model.store.toInfoResponse
import org.l2kserver.game.handler.dto.request.PrivateStoreBuySetMessageRequest
import org.l2kserver.game.handler.dto.request.PrivateStoreBuyStartRequest
import org.l2kserver.game.handler.dto.request.BuyInPrivateStoreRequest
import org.l2kserver.game.handler.dto.request.ExchangeRequest
import org.l2kserver.game.handler.dto.request.PrivateStoreSellSetMessageRequest
import org.l2kserver.game.handler.dto.request.PrivateStoreSellStartRequest
import org.l2kserver.game.handler.dto.request.RequestedToSellItem
import org.l2kserver.game.handler.dto.request.RequestedToSellToPrivateStoreItem
import org.l2kserver.game.handler.dto.request.SellToPrivateStoreRequest
import org.l2kserver.game.handler.dto.response.ActionFailedResponse
import org.l2kserver.game.handler.dto.response.ItemListForPrivateStoreBuyResponse
import org.l2kserver.game.handler.dto.response.ItemListForPrivateStoreSellResponse
import org.l2kserver.game.handler.dto.response.PlaySoundResponse
import org.l2kserver.game.handler.dto.response.PrivateStoreBuySetMessageResponse
import org.l2kserver.game.handler.dto.response.PrivateStoreSellSetMessageResponse
import org.l2kserver.game.handler.dto.response.Sound
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.handler.dto.response.UpdateItemsResponse
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.item.instance.ItemInstance
import org.l2kserver.game.model.store.ItemInWishList
import org.l2kserver.game.model.store.ItemOnSale
import org.l2kserver.game.model.store.PrivateStore
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sendTo
import org.l2kserver.game.network.session.sessionContext
import org.l2kserver.game.repository.GameObjectRepository
import org.springframework.stereotype.Service
import java.sql.Connection
import kotlin.collections.map
import kotlin.math.roundToInt

private const val PRIVATE_STORE_MESSAGE_MAX_SIZE = 29

@Service
class TradeService(
    override val gameObjectRepository: GameObjectRepository
) : AbstractService() {

    override val log = logger()

    /**
     *
     * Stores private store (lol) titles.
     * For some Korean reasons setting title of private store is realized at server side,
     * so we have to keep it here
     *
     * Key - characterId, value - private store title
     */
    private val privateStoreTitlesCache = ConcurrentHashMap<Int, String>()

    /**
     * Start exchanging with [ExchangeRequest.targetId]
     */
    @Suppress("unused")
    suspend fun startExchanging(request: ExchangeRequest) {
        TODO("https://github.com/orgs/l2kserver/projects/1/views/3?pane=issue&itemId=103187674&issue=l2kserver%7Cl2kserver-game%7C16")
    }

    /**
     * Stops private store
     */
    suspend fun stopPrivateStore() = newSuspendedTransaction {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        if (character.privateStore != null) {
            log.debug("Cancelling private store of character '{}'", character)
            privateStoreTitlesCache.remove(character.id)

            character.privateStore = null
            character.standUp()
            broadcastActorInfo(character)

            log.debug("Private store of character '{}' was successfully cancelled", character)
        }
    }

    /** Handles request to get items for private store (sell) */
    suspend fun getItemsForPrivateStoreSell() = newSuspendedTransaction {
        val context = sessionContext()
        val character = gameObjectRepository.findCharacterById(context.getCharacterId())

        //Check that player has no private store, or it's private store is PrivateStore.Sell
        if (character.privateStore !is PrivateStore.Sell?) {
            send(ActionFailedResponse)
            return@newSuspendedTransaction
        }

        val privateStore = character.privateStore as? PrivateStore.Sell
        val itemsInStore = privateStore?.items?.values ?: emptyList()

        stopPrivateStore()

        val itemsInInventory = character.inventory
            .mapNotNull { item ->
                val itemOnSale = privateStore?.items[item.id]

                if (item.isEquipped || !item.isSellable || (itemOnSale != null && itemOnSale.amount >= item.amount)) null
                else item.toItemInInventory(item.amount - (itemOnSale?.amount ?: 0))
            }

        val adenaAmount = character.inventory.adena?.amount ?: 0

        send(
            ItemListForPrivateStoreSellResponse(
                characterId = character.id,
                packageSale = privateStore?.packageSale == true,
                characterAdena = adenaAmount,
                itemsInInventory = itemsInInventory,
                itemsInStore = itemsInStore
            )
        )
    }

    /** Set message of private store (sell) to cache */
    suspend fun setPrivateStoreSellMessage(request: PrivateStoreSellSetMessageRequest) {
        setPrivateStoreMessage(request.message)?.let {
            send(PrivateStoreSellSetMessageResponse(sessionContext().getCharacterId(), it))
        }
    }

    /** Start private store (sell) */
    suspend fun startPrivateStoreSell(request: PrivateStoreSellStartRequest) = newSuspendedTransaction {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        log.debug("Starting private store by request '{}' of character '{}'", request, character)

        if (request.items.isEmpty()) {
            log.warn("{} is trying to start private store (sell), but without items", character)
            send(ActionFailedResponse)
            return@newSuspendedTransaction
        }

        if (character.tradeAndInventoryStats.privateStoreSize < request.items.size) {
            send(SystemMessageResponse.YouHaveExceededPrivateStoreQuantity)

            getItemsForPrivateStoreSell()
            return@newSuspendedTransaction
        }

        val itemsOnSale = request.items.map { it.toItemOnSale(character) }
        require(itemsOnSale.allUniqueBy { it.itemId }) { "Several slots cannot refer to the same item!" }

        character.sitDown()

        val storeTitle = privateStoreTitlesCache[character.id] ?: ""
        val privateStore = PrivateStore.Sell(storeTitle, itemsOnSale, request.packageSale)

        character.privateStore = privateStore

        broadcastActorInfo(character)
        log.info("Started PrivateStoreSell='{}' of character '{}'", privateStore, character)
    }

    /** Buy items in private store */
    suspend fun buyInPrivateStore(request: BuyInPrivateStoreRequest) {
        val customer = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        val seller = gameObjectRepository.findCharacterById(request.storeOwnerId)
        log.debug("Start purchasing items='{}' from '{}' by '{}'", request.items, customer, seller)

        val requiredDistance = INTERACTION_DISTANCE + (customer.collisionBox.radius + seller.collisionBox.radius).roundToInt()
        if (!customer.position.isCloseTo(seller.position, requiredDistance)) {
            log.debug("StoreOwner is too far to buy")
            send(PlaySoundResponse(Sound.ITEMSOUND_SYS_SHORTAGE))
            send(ActionFailedResponse)
            return
        }

        val privateStore = seller.privateStore as? PrivateStore.Sell ?: run {
            log.debug("Cannot buy anything from '{}', because he has no private store (sell) opened", seller)
            send(ActionFailedResponse)
            return
        }

        //Lock store for transaction time
        privateStore.mutex.withLock {
            var itemsOnSale = privateStore.items
            newSuspendedTransaction(transactionIsolation = Connection.TRANSACTION_SERIALIZABLE) {
                if (!checkAllPresent(privateStore.items, request.items, seller)) {
                    log.debug(
                        "[SELL] '{}' or '{}' inventory does not contain all required items from request '{}'",
                        privateStore, seller, request
                    )
                    send(ActionFailedResponse)
                    return@newSuspendedTransaction
                }
                val totalPrice = calculateTotalPrice(privateStore.items, request.items)

                val customerAdena = customer.inventory.adena

                if ((customerAdena?.amount ?: 0) < totalPrice) {
                    send(SystemMessageResponse.NotEnoughAdena)
                    send(ActionFailedResponse)
                    return@newSuspendedTransaction
                }

                //Transfer adena
                val (adenaOperationsOfCustomer, adenaOperationsOfSeller) = transferItem(
                    customerAdena!!, from = customer, to = seller, amount = totalPrice
                )

                //Transfer items
                val (itemOperationsOfSeller, itemOperationsOfCustomer) = request.items.map {
                    val item = it.toItemInstance(seller)
                    val operations = transferItem(item, from = seller, to = customer, amount = it.amount)

                    //Subtract sold items from itemsOnSale
                    itemsOnSale = itemsOnSale.subtractTradedItem(it)

                    sendTo(customer.id, SystemMessageResponse.youHavePurchased(item, seller.name, it.amount))
                    sendTo(seller.id, SystemMessageResponse.otherHasPurchased(customer.name, item, it.amount))

                    operations
                }.reduce { acc, pair -> (acc.first + pair.first) to (acc.second + pair.second) }

                sendTo(customer.id, (adenaOperationsOfCustomer + itemOperationsOfCustomer).build())
                sendTo(seller.id, (adenaOperationsOfSeller + itemOperationsOfSeller).build())

                sendTo(customer.id, UpdateStatusResponse.weightOf(customer))
                sendTo(seller.id, UpdateStatusResponse.weightOf(seller))

                //If no items left to sell - close store, otherwise update it
                if (itemsOnSale.isEmpty()) {
                    seller.privateStore = null
                    broadcastActorInfo(seller)
                }
                else seller.privateStore = privateStore.copy(items = itemsOnSale)
            }
        }
    }

    /** Start private manufacture */
    suspend fun startGeneralPrivateManufacture() {
        //TODO https://github.com/l2kserver/l2kserver-game/issues/27
        send(SystemMessageResponse("Private manufacture is not implemented yet"), ActionFailedResponse)
    }

    /** Shows [character]'s private store info */
    suspend fun showPrivateStoreOf(character: PlayerCharacter) {
        val privateStore = character.privateStore ?: run {
            log.warn("No private store of character '{}' found", character)
            send(ActionFailedResponse)
            return
        }

        val customer = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        send(privateStore.toInfoResponse(character, customer))
    }

    /** Sends to the client items, suitable for private store (Buy) */
    suspend fun getItemsForPrivateStoreBuy(): Unit = newSuspendedTransaction {
        val context = sessionContext()
        val character = gameObjectRepository.findCharacterById(context.getCharacterId())

        //Check that player has no private store, or it's private store is PrivateStore.Buy
        if (character.privateStore !is PrivateStore.Buy?) {
            send(ActionFailedResponse)
            return@newSuspendedTransaction
        }

        val privateStore = character.privateStore as? PrivateStore.Buy
        val itemsInStore = privateStore?.items ?: emptyList()

        stopPrivateStore()

        val itemsInInventory = character.inventory
            .filter { !it.isEquipped && it.isSellable }
            .map { it.toItemInInventory() }

        val adenaAmount = character.inventory.adena?.amount ?: 0

        send(
            ItemListForPrivateStoreBuyResponse(
                characterId = character.id,
                characterAdena = adenaAmount,
                itemsInInventory = itemsInInventory,
                itemsInStore = itemsInStore
            )
        )
    }

    /** Set message of private store (sell) to cache */
    suspend fun setPrivateStoreBuyMessage(request: PrivateStoreBuySetMessageRequest) {
        setPrivateStoreMessage(request.message)?.let {
            send(PrivateStoreBuySetMessageResponse(sessionContext().getCharacterId(), it))
        }
    }

    /** Start private store (buy) */
    suspend fun startPrivateStoreBuy(request: PrivateStoreBuyStartRequest): Unit = newSuspendedTransaction {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        log.debug("Starting private store (Buy) by request '{}' of character '{}'", request, character)

        if (request.items.isEmpty()) {
            log.warn("{} is trying to start private store (buy), but without items", character)
            send(ActionFailedResponse)
            return@newSuspendedTransaction
        }

        if (character.tradeAndInventoryStats.privateStoreSize < request.items.size) {
            send(SystemMessageResponse.YouHaveExceededPrivateStoreQuantity)
            getItemsForPrivateStoreBuy()
            return@newSuspendedTransaction
        }

        val characterAdenaAmount = character.inventory.adena?.amount ?: 0
        val totalPrice = request.items.map { it.amount * it.price }.reduce { acc, i -> acc + i }

        if (characterAdenaAmount < totalPrice) {
            send(SystemMessageResponse.NotEnoughAdena)
            getItemsForPrivateStoreBuy()
            return@newSuspendedTransaction
        }

        val tradedItems = request.items.map { it.toItemInWishList(character.id) }
        character.sitDown()

        val storeTitle = privateStoreTitlesCache[character.id] ?: ""
        val privateStore = PrivateStore.Buy(storeTitle, tradedItems)

        character.privateStore = privateStore

        broadcastActorInfo(character)
        log.info("Started PrivateStoreBuy='{}' of character '{}'", privateStore, character)
    }

    suspend fun sellToPrivateStore(request: SellToPrivateStoreRequest) {
        val seller = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        val storeOwner = gameObjectRepository.findCharacterById(request.storeOwnerId)
        log.debug("Start selling items='{}' from '{}' by '{}'", request.items, seller, storeOwner)

        val requiredDistance = INTERACTION_DISTANCE + (seller.collisionBox.radius + storeOwner.collisionBox.radius).roundToInt()
        if (!seller.position.isCloseTo(storeOwner.position, requiredDistance)) {
            log.debug("StoreOwner is too far to sell")
            send(PlaySoundResponse(Sound.ITEMSOUND_SYS_SHORTAGE))
            send(ActionFailedResponse)
            return
        }

        val privateStore = storeOwner.privateStore as? PrivateStore.Buy ?: run {
            log.debug("Cannot buy anything from '{}', because he has no private store (buy) opened", seller)
            send(ActionFailedResponse)
            return
        }

        //Lock store for transaction time
        privateStore.mutex.withLock {
            var itemsInWishList = privateStore.items
            newSuspendedTransaction(transactionIsolation = Connection.TRANSACTION_SERIALIZABLE) {
                if (!checkAllPresent(privateStore.items, request.items, seller)) {
                    log.debug(
                        "[BUY] '{}' or '{}' inventory does not contain all required items from request '{}'",
                        privateStore, seller, request
                    )
                    send(ActionFailedResponse)
                    return@newSuspendedTransaction
                }

                val totalPrice = calculateTotalPrice(privateStore.items, request.items)
                val storeOwnerAdena = storeOwner.inventory.adena

                if ((storeOwnerAdena?.amount ?: 0) < totalPrice) {
                    send(ActionFailedResponse)
                    return@newSuspendedTransaction
                }

                //Transfer adena
                val (adenaOperationsOfStoreOwner, adenaOperationsOfSeller) = transferItem(
                    storeOwnerAdena!!, from = storeOwner, to = seller, amount = totalPrice
                )

                //Transfer items
                val (itemOperationsOfSeller, itemOperationsOfStoreOwner) = request.items.map {
                    val item = it.toItemInstance(seller)
                    val operations = transferItem(item, from = seller, to = storeOwner, amount = it.amount)

                    //Subtract bought items from itemsInWishList
                    itemsInWishList = itemsInWishList.subtractTradedItem(it)

                    sendTo(storeOwner.id, SystemMessageResponse.youHavePurchased(item, seller.name, it.amount))
                    sendTo(seller.id, SystemMessageResponse.otherHasPurchased(storeOwner.name, item, it.amount))

                    operations
                }.reduce { acc, pair -> (acc.first + pair.first) to (acc.second + pair.second) }

                sendTo(storeOwner.id, (adenaOperationsOfStoreOwner + itemOperationsOfStoreOwner).build())
                sendTo(seller.id, (adenaOperationsOfSeller + itemOperationsOfSeller).build())

                sendTo(storeOwner.id, UpdateStatusResponse.weightOf(storeOwner))
                sendTo(seller.id, UpdateStatusResponse.weightOf(seller))

                //If no items left to sell - close store, otherwise update it
                if (itemsInWishList.isEmpty()) {
                    storeOwner.privateStore = null
                    broadcastActorInfo(storeOwner)
                }
                else storeOwner.privateStore = privateStore.copy(items = itemsInWishList)
            }
        }
    }

    /**
     * Transfers [amount] of [item] to [to]
     *
     * @return Pair of lists of update items operations - first for item ex owner, second for [to]
     */
    //All the responses should be sent only after all the item transferring is complete
    private suspend fun transferItem(
        item: ItemInstance, from: PlayerCharacter, to: PlayerCharacter, amount: Int
    ): Pair<UpdateItemsResponse.Builder, UpdateItemsResponse.Builder> {
        require(amount <= item.amount) { "Not enough $item to transfer!" }

        val updateItemOperationsFrom = UpdateItemsResponse.Builder()
        val updateItemOperationsTo = UpdateItemsResponse.Builder()

        val existingReceiversItem = to.inventory.findAllByTemplateId(item.templateId).firstOrNull()

        val itemFrom = from.inventory.reduceAmount(item.id, amount)
        val itemTo = to.inventory.createItem(item.templateId, amount, enchantLevel = item.enchantLevel)

        if (!itemTo.isStackable || existingReceiversItem == null) updateItemOperationsTo.operationAdd(itemTo)
        else updateItemOperationsTo.operationModify(itemTo)

        if (itemFrom == null) updateItemOperationsFrom.operationDelete(item)
        else updateItemOperationsFrom.operationModify(itemFrom)

        return updateItemOperationsFrom to updateItemOperationsTo
    }

    /**
     * Saves private store message to cache and returns it
     *
     * @return saved message of null, if requested message cannot be set
     */
    private suspend fun setPrivateStoreMessage(message: String): String? {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        if (message.length > PRIVATE_STORE_MESSAGE_MAX_SIZE) {
            log.warn("'{}' was trying to set too big private store (Buy) message!", character)
            send(ActionFailedResponse)
            return null
        }
        //TODO Message censorship?
        privateStoreTitlesCache[character.id] = message

        return message
    }

    /**
     * Checks that all the requested items are in private store and inventory
     */
    private fun checkAllPresent(
        itemsInStore: Map<Int, ItemOnSale>, requestedItems: Iterable<RequestedToSellItem>, seller: PlayerCharacter
    ) = requestedItems.all { requestedItem ->
        val itemInStoreAmount = itemsInStore[requestedItem.itemId]?.amount ?: 0
        val itemInInventoryAmount = seller.inventory.findNotEquippedByIdOrNull(requestedItem.itemId)?.amount ?: 0

        itemInStoreAmount >= requestedItem.amount && itemInInventoryAmount >= requestedItem.amount
    }

    private fun checkAllPresent(
        itemsInWishList: Iterable<ItemInWishList>,
        requestedItems: Iterable<RequestedToSellToPrivateStoreItem>,
        seller: PlayerCharacter,
    ): Boolean = requestedItems.all { requestedItem ->
        val existsInPrivateStore = itemsInWishList.any {
            it.templateId == requestedItem.templateId &&
                it.enchantLevel == requestedItem.enchantLevel &&
                    it.amount >= requestedItem.amount
        }
        val existsInventory = seller.inventory.existsByIdAndAmount(requestedItem.itemId, requestedItem.amount)

        existsInPrivateStore && existsInventory
    }

    /**
     * Calculates total price of selected items
     */
    private fun calculateTotalPrice(
        itemsInStore: Map<Int, ItemOnSale>, requestedItems: Iterable<RequestedToSellItem>
    ): Int = requestedItems.map { requestedItem ->
        itemsInStore[requestedItem.itemId]!!.let { itemOnSale -> requestedItem.amount * itemOnSale.price }
    }.reduce { acc, i -> acc + i }

    /**
     * Calculates total price of selected items
     */
    private fun calculateTotalPrice(
        itemsInWishList: Iterable<ItemInWishList>,
        requestedItems: Iterable<RequestedToSellToPrivateStoreItem>
    ): Int = requestedItems.map { requestedItem ->
        itemsInWishList.find { it.templateId == requestedItem.templateId }!!.price * requestedItem.amount
    }.reduce { acc, i -> acc + i }

}
