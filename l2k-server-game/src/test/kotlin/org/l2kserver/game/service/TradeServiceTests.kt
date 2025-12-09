package org.l2kserver.game.service

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.assertThrows
import org.l2kserver.game.AbstractTests
import org.l2kserver.game.data.item.arrows.BoneArrow
import org.l2kserver.game.data.item.arrows.WoodenArrow
import org.l2kserver.game.data.item.etc.Adena
import org.l2kserver.game.data.item.weapons.DemonSplinter
import org.l2kserver.game.data.item.weapons.HeavensDivider
import org.l2kserver.game.data.item.weapons.TallumBladeDarkLegionsEdge
import org.l2kserver.game.domain.ItemEntity
import org.l2kserver.game.extensions.toItemInWishList
import org.l2kserver.game.extensions.toItemOnSale
import org.l2kserver.game.handler.dto.request.PrivateStoreBuySetMessageRequest
import org.l2kserver.game.handler.dto.request.PrivateStoreBuyStartRequest
import org.l2kserver.game.handler.dto.request.BuyInPrivateStoreRequest
import org.l2kserver.game.handler.dto.request.PrivateStoreSellSetMessageRequest
import org.l2kserver.game.handler.dto.request.PrivateStoreSellStartRequest
import org.l2kserver.game.handler.dto.request.RequestedToBuyItem
import org.l2kserver.game.handler.dto.request.RequestedToSellItem
import org.l2kserver.game.handler.dto.request.RequestedToSellToPrivateStoreItem
import org.l2kserver.game.handler.dto.request.SellToPrivateStoreRequest
import org.l2kserver.game.handler.dto.response.ActionFailedResponse
import org.l2kserver.game.handler.dto.response.ChangePostureResponse
import org.l2kserver.game.handler.dto.response.CharacterInfoResponse
import org.l2kserver.game.handler.dto.response.FullCharacterResponse
import org.l2kserver.game.handler.dto.response.ItemListForPrivateStoreBuyResponse
import org.l2kserver.game.handler.dto.response.ItemListForPrivateStoreSellResponse
import org.l2kserver.game.handler.dto.response.PlaySoundResponse
import org.l2kserver.game.handler.dto.response.PrivateStoreBuySetMessageResponse
import org.l2kserver.game.handler.dto.response.PrivateStoreSellSetMessageResponse
import org.l2kserver.game.handler.dto.response.ShowPrivateStoreBuyResponse
import org.l2kserver.game.handler.dto.response.ShowPrivateStoreSellResponse
import org.l2kserver.game.handler.dto.response.Sound
import org.l2kserver.game.handler.dto.response.StatusAttribute
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.handler.dto.response.UpdateItemOperation
import org.l2kserver.game.handler.dto.response.UpdateItemsResponse
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.handler.dto.response.item
import org.l2kserver.game.handler.dto.response.operation
import org.l2kserver.game.model.actor.Posture
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.store.PrivateStore
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Suppress("LargeClass")
class TradeServiceTests(
    @param:Autowired private val tradeService: TradeService
): AbstractTests() {

    @Test
    fun shouldSendCharactersSellableItems(): Unit = runBlocking {
        //Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Create our goods
        val adena = createTestItem(Adena.id, character, 1000)
        val demonSplinter = createTestItem(DemonSplinter.id, character)
        val heavensDivider = createTestItem(HeavensDivider.id, character)

        withContext(context) { tradeService.getItemsForPrivateStoreSell() }

        val response = assertIs<ItemListForPrivateStoreSellResponse>(context.responseChannel.receive())

        assertFalse(response.packageSale)
        assertEquals(adena.amount, response.characterAdena)
        assertContains(response.itemsInInventory.map { it.itemId }, demonSplinter.id)
        assertContains(response.itemsInInventory.map { it.itemId }, heavensDivider.id)
        assertFalse(response.itemsInInventory.map { it.itemId }.contains(adena.id))
    }

    @Test
    fun shouldStartPrivateStoreSell(): Unit = runBlocking {
        //Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Create customer
        val customerContext = createTestSessionContext()
        val customerCharacter = createTestCharacter(name = "MrCustomer")
        customerContext.setCharacterId(customerCharacter.id)

        val testStoreMessage = "Baium Express"

        //Create our goods
        val arrowsAmount = 1000
        val arrowPrice = 2
        val woodenArrow = createTestItem(WoodenArrow.id, character, arrowsAmount)

        val testWeaponsPrice = 50_000_000
        val demonSplinter = createTestItem(DemonSplinter.id, character)
        val heavensDivider = createTestItem(HeavensDivider.id, character)

        //Open our little store!
        withContext(context) {
            tradeService.setPrivateStoreSellMessage(PrivateStoreSellSetMessageRequest(testStoreMessage))
            tradeService.startPrivateStoreSell(PrivateStoreSellStartRequest(
                packageSale = false,
                items = listOf(
                    RequestedToSellItem(woodenArrow.id, arrowsAmount, arrowPrice),
                    RequestedToSellItem(demonSplinter.id, 1, testWeaponsPrice),
                    RequestedToSellItem(heavensDivider.id, 1, testWeaponsPrice)
                )
            ))
        }

        //Check our responses
        val setStoreMessageResponse = assertIs<PrivateStoreSellSetMessageResponse>(context.responseChannel.receive())
        assertEquals(testStoreMessage, setStoreMessageResponse.message)

        val changePostureResponse = assertIs<ChangePostureResponse>(context.responseChannel.receive())
        assertEquals(Posture.SITTING, changePostureResponse.posture)

        val characterResponse = assertIs<FullCharacterResponse>(context.responseChannel.receive())
        assertNotNull(characterResponse.character.privateStore)

        val store = assertIs<PrivateStore.Sell>(characterResponse.character.privateStore!!)
        assertEquals(testStoreMessage, store.title)

        assertNotNull(store.items[heavensDivider.id])
        assertEquals(1, store.items[heavensDivider.id]!!.amount)
        assertEquals(testWeaponsPrice, store.items[heavensDivider.id]!!.price)

        assertNotNull(store.items[demonSplinter.id])
        assertEquals(1, store.items[demonSplinter.id]!!.amount)
        assertEquals(testWeaponsPrice, store.items[demonSplinter.id]!!.price)

        assertNotNull(store.items[woodenArrow.id])
        assertEquals(arrowsAmount, store.items[woodenArrow.id]!!.amount)
        assertEquals(arrowPrice, store.items[woodenArrow.id]!!.price)

        val storeMessageResponse = assertIs<PrivateStoreSellSetMessageResponse>(context.responseChannel.receive())
        assertEquals(testStoreMessage, storeMessageResponse.message)

        //Check customer responses
        val changePostureResponseForCustomer =
            assertIs<ChangePostureResponse>(customerContext.responseChannel.receive())
        assertEquals(Posture.SITTING, changePostureResponseForCustomer.posture)

        val characterResponseForCustomer = assertIs<CharacterInfoResponse>(customerContext.responseChannel.receive())
        assertNotNull(characterResponseForCustomer.character.privateStore)
        assertIs<PrivateStore.Sell>(characterResponse.character.privateStore!!)

        val storeMessageResponseForCustomer =
            assertIs<PrivateStoreSellSetMessageResponse>(customerContext.responseChannel.receive())
        assertEquals(testStoreMessage, storeMessageResponseForCustomer.message)
    }

    @Test
    fun shouldOpenCurrentStoreSettingsWindowIfStoreAlreadyExists(): Unit = runBlocking {
        //Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Create our goods
        val arrowsForSaleAmount = 300
        val arrowsInInventoryAmount = 700
        val arrowPrice = 2
        val woodenArrow = createTestItem(
            WoodenArrow.id, character, arrowsForSaleAmount+arrowsInInventoryAmount
        )

        character.posture = Posture.SITTING
        character.privateStore = PrivateStore.Sell(
            title = "Wooden arrows - cheap and cheerful",
            items = listOf(woodenArrow.toItemOnSale(arrowPrice, arrowsForSaleAmount)),
            packageSale = true
        )

        //Get items for private store
        withContext(context) { tradeService.getItemsForPrivateStoreSell() }

        //Check responses
        val standUpResponse = assertIs<ChangePostureResponse>(context.responseChannel.receive())
        assertEquals(Posture.STANDING, standUpResponse.posture)
        assertIs<FullCharacterResponse>(context.responseChannel.receive())

        val itemsForPrivateStoreSellResponse = assertIs<ItemListForPrivateStoreSellResponse>(
            context.responseChannel.receive()
        )

        assertTrue(itemsForPrivateStoreSellResponse.packageSale, "packageSale must be 'true'")

        assertEquals(1, itemsForPrivateStoreSellResponse.itemsInInventory.size)
        val woodenArrowInInventory = assertNotNull(
            itemsForPrivateStoreSellResponse.itemsInInventory.find { it.itemId == woodenArrow.id }
        )
        assertEquals(arrowsInInventoryAmount, woodenArrowInInventory.amount)

        assertEquals(1, itemsForPrivateStoreSellResponse.itemsInStore.size)
        val woodenArrowOnSale = assertNotNull(
            itemsForPrivateStoreSellResponse.itemsInStore.find { it.itemId == woodenArrow.id }
        )
        assertEquals(arrowsForSaleAmount, woodenArrowOnSale.amount)
    }

    @Test
    fun shouldFailStartingPrivateStoreSellNotEnoughItemsToSell(): Unit = runBlocking {
        //Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Create our goods
        val arrowsAmount = 1000
        val arrowPrice = 2
        val woodenArrow = createTestItem(WoodenArrow.id, character, arrowsAmount)

        //Open our little store!
        val exception = assertThrows<IllegalArgumentException> {
            withContext(context) {
                tradeService.startPrivateStoreSell(PrivateStoreSellStartRequest(
                    packageSale = false,
                    items = listOf(RequestedToSellItem(woodenArrow.id, arrowsAmount + 2, arrowPrice))
                ))
            }
        }

        assertTrue(
            exception.message!!.startsWith("Not enough Arrow"),
            "Exception message should be 'Not enough Arrow'"
        )
    }

    @Test
    fun shouldFailStartingPrivateStoreSellSeveralSlotsWithSameItem(): Unit = runBlocking {
        //Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Create our goods
        val arrowsAmount = 1000
        val arrowPrice = 2
        val woodenArrow = createTestItem(WoodenArrow.id, character, arrowsAmount)

        //Open our little store!
        val exception = assertThrows<IllegalArgumentException> {
            withContext(context) {
                tradeService.startPrivateStoreSell(PrivateStoreSellStartRequest(
                    packageSale = false,
                    items = listOf(
                        RequestedToSellItem(woodenArrow.id, arrowsAmount / 2, arrowPrice),
                        RequestedToSellItem(woodenArrow.id, arrowsAmount / 2, arrowPrice)
                    )
                ))
            }
        }

        assertTrue(
            exception.message!!.startsWith("Several slots cannot refer to the same item!"),
            "Exception message should be 'Several slots cannot refer to the same item!'"
        )
    }

    @Test
    fun shouldFailStartingPrivateStoreSellTooManySlots(): Unit = runBlocking {
        //Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Create our goods
        val amount = 1000
        val price = 2
        val woodenArrow = createTestItem(WoodenArrow.id, character, amount)
        val boneArrow = createTestItem(BoneArrow.id, character, amount)
        val demonSplinter = createTestItem(DemonSplinter.id, character)
        val heavensDivider = createTestItem(HeavensDivider.id, character)
        val duals = createTestItem(TallumBladeDarkLegionsEdge.id, character)

        //Open our little store!
        withContext(context) {
            tradeService.startPrivateStoreSell(PrivateStoreSellStartRequest(
                packageSale = false,
                items = listOf(
                    RequestedToSellItem(woodenArrow.id, amount, price),
                    RequestedToSellItem(boneArrow.id, amount, price),
                    RequestedToSellItem(demonSplinter.id, 1, price),
                    RequestedToSellItem(heavensDivider.id, 1, price),
                    RequestedToSellItem(duals.id, 1, price)
                )
            ))
        }
        assertIs<SystemMessageResponse.YouHaveExceededPrivateStoreQuantity>(context.responseChannel.receive())
    }

    @Test
    fun shouldFailStartingPrivateStoreSellNotSellableItems(): Unit = runBlocking {
        //Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Create our goods
        val amount = 1000
        val price = 2
        val adena = createTestItem(Adena.id, character, amount)

        //Open our little store!
        val exception = assertThrows<IllegalArgumentException>{
            withContext(context) {
                tradeService.startPrivateStoreSell(PrivateStoreSellStartRequest(
                    packageSale = false,
                    items = listOf(RequestedToSellItem(adena.id, amount, price))
                ))
            }
        }

        assertContains(exception.message!!, "is trying to sell non-sellable item")
    }

    @Test
    fun shouldStopPrivateStore(): Unit = runBlocking {
        //Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Create our goods
        val arrowsForSaleAmount = 300
        val arrowsInInventoryAmount = 700
        val arrowPrice = 2
        val woodenArrow = createTestItem(
            WoodenArrow.id, character, arrowsForSaleAmount+arrowsInInventoryAmount
        )

        character.posture = Posture.SITTING
        character.privateStore = PrivateStore.Sell(
            title = "Wooden arrows - cheap and cheerful",
            items = listOf(woodenArrow.toItemOnSale(arrowPrice, arrowsForSaleAmount)),
            packageSale = true
        )

        //Get items for private store
        withContext(context) { withContext(context) { tradeService.stopPrivateStore() }}

        //Check responses
        val changePostureResponse = assertIs<ChangePostureResponse>(context.responseChannel.receive())
        assertEquals(Posture.STANDING, changePostureResponse.posture)

        val characterInfoResponse = assertIs<FullCharacterResponse>(context.responseChannel.receive())
        assertNull(characterInfoResponse.character.privateStore)
    }

    @Test
    fun shouldOpenOtherCharactersPrivateStore(): Unit = runBlocking {
        //Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Create our goods
        val arrowsForSaleAmount = 300
        val arrowsInInventoryAmount = 700
        val arrowPrice = 2
        val woodenArrow = createTestItem(
            WoodenArrow.id, character, arrowsForSaleAmount+arrowsInInventoryAmount
        )

        character.posture = Posture.SITTING
        character.privateStore = PrivateStore.Sell(
            title = "Wooden arrows - cheap and cheerful",
            items = listOf(woodenArrow.toItemOnSale(arrowPrice, arrowsForSaleAmount)),
            packageSale = true
        )

        //Create customer
        val customerContext = createTestSessionContext()
        val customerCharacter = createTestCharacter(name = "Customer")
        customerContext.setCharacterId(customerCharacter.id)

        //Get items for private store
        withContext(context) { withContext(customerContext) { tradeService.showPrivateStoreOf(character) }}

        //Check responses
        val response = assertIs<ShowPrivateStoreSellResponse>(customerContext.responseChannel.receive())
        assertEquals(character.id, response.ownerId)
        assertTrue(response.packageSale, "Package sale must be true")
        assertEquals((character.privateStore as? PrivateStore.Sell)?.items?.values, response.items)
    }

    @Test
    @Suppress("LongMethod")
    fun shouldSuccessfullyBuyItemsFromPrivateStore(): Unit = runBlocking {
        // Create seller
        val sellerContext = createTestSessionContext()
        val seller = createTestCharacter(name = "Seller")
        sellerContext.setCharacterId(seller.id)

        // Create goods for our store
        val arrowsAmount = 1000
        val arrowPrice = 2
        val demonSplinterPrice = 50000

        val woodenArrow = createTestItem(WoodenArrow.id, seller, arrowsAmount)
        val demonSplinter = createTestItem(DemonSplinter.id, seller)

        // Create store
        seller.posture = Posture.SITTING
        seller.privateStore = PrivateStore.Sell(
            title = "Test shop",
            items = listOf(
                woodenArrow.toItemOnSale(arrowPrice),
                demonSplinter.toItemOnSale(demonSplinterPrice)
            ),
            packageSale = false
        )

        // Create customer
        val buyerContext = createTestSessionContext()
        val buyer = createTestCharacter(name = "Buyer")
        buyerContext.setCharacterId(buyer.id)

        // Create customer's adena
        val initialBuyerAdena = 1000000
        createTestItem(Adena.id, buyer, initialBuyerAdena)

        // Let's buy some arrows!
        val arrowsToBuyAmount = 600

        withContext(buyerContext) {
            tradeService.buyInPrivateStore(BuyInPrivateStoreRequest(
                storeOwnerId = seller.id,
                items = listOf(
                    RequestedToSellItem(woodenArrow.id, arrowsToBuyAmount, arrowPrice),
                    RequestedToSellItem(demonSplinter.id, 1, 50000)
                )
            ))
        }


        val totalPrice = arrowsToBuyAmount * arrowPrice + demonSplinterPrice


        // Check buyer's responses
        val buyerSystemMessageArrows = assertIs<SystemMessageResponse.YouHavePurchasedStackable>(
            buyerContext.responseChannel.receive())
        assertEquals(arrowsToBuyAmount, buyerSystemMessageArrows.amount)
        assertEquals(WoodenArrow.id, buyerSystemMessageArrows.item.templateId)

        val buyerSystemMessageDemonSplinter = assertIs<SystemMessageResponse.YouHavePurchasedNonStackable>(
            buyerContext.responseChannel.receive())
        assertEquals(1, buyerSystemMessageDemonSplinter.item.amount)
        assertEquals(DemonSplinter.id, buyerSystemMessageDemonSplinter.item.templateId)

        //Check items responses
        val buyerUpdateItemsResponse = assertIs<UpdateItemsResponse>(buyerContext.responseChannel.receive())
        assertEquals(3, buyerUpdateItemsResponse.operations.size)

        // Check adena update
        val buyerAdenaOperation = buyerUpdateItemsResponse.operations
            .find { it.item.templateId == Adena.id }
        assertNotNull(buyerAdenaOperation)
        assertEquals(UpdateItemOperation.MODIFY, buyerAdenaOperation.operation)
        assertEquals(initialBuyerAdena - totalPrice, buyerAdenaOperation.item.amount)

        // Check items update
        val buyerArrowOperation = buyerUpdateItemsResponse.operations
            .find { it.item.templateId == WoodenArrow.id }

        assertNotNull(buyerArrowOperation)
        assertEquals(UpdateItemOperation.ADD, buyerArrowOperation.operation)
        assertEquals(arrowsToBuyAmount, buyerArrowOperation.item.amount)

        val buyerSplinterOperation = buyerUpdateItemsResponse.operations
            .find { it.item.templateId == DemonSplinter.id }
        assertNotNull(buyerSplinterOperation)
        assertEquals(UpdateItemOperation.ADD, buyerSplinterOperation.operation)
        assertEquals(1, buyerSplinterOperation.item.amount)

        // Check buyer's weight change
        val buyerUpdateStatus = assertIs<UpdateStatusResponse>(buyerContext.responseChannel.receive())
        assertContains(buyerUpdateStatus.attributes, StatusAttribute.CUR_LOAD)

        // Check seller's responses
        val sellerSystemMessageArrows = assertIs<SystemMessageResponse.OtherHasPurchasedStackable>(
            sellerContext.responseChannel.receive())
        assertEquals(buyer.name, sellerSystemMessageArrows.customerName)
        assertEquals(arrowsToBuyAmount, sellerSystemMessageArrows.amount)
        assertEquals(WoodenArrow.id, sellerSystemMessageArrows.item.templateId)

        val sellerSystemMessageDemonSplinter = assertIs<SystemMessageResponse.OtherHasPurchasedNonStackable>(
            sellerContext.responseChannel.receive())
        assertEquals(buyer.name, sellerSystemMessageDemonSplinter.customerName)
        assertEquals(1, sellerSystemMessageDemonSplinter.item.amount)
        assertEquals(DemonSplinter.id, sellerSystemMessageDemonSplinter.item.templateId)

        val sellerUpdateItems = assertIs<UpdateItemsResponse>(sellerContext.responseChannel.receive())
        assertEquals(3, sellerUpdateItems.operations.size)

        // Check seller's adena update
        val sellerAdenaOperation = sellerUpdateItems.operations.find { it.item.templateId == Adena.id }
        assertNotNull(sellerAdenaOperation)
        assertEquals(UpdateItemOperation.ADD, sellerAdenaOperation.operation)
        assertEquals(totalPrice, sellerAdenaOperation.item.amount)

        // Check seller's item updates
        val sellerArrowOperation = sellerUpdateItems.operations
            .find { it.item.templateId == WoodenArrow.id }
        assertNotNull(sellerArrowOperation)
        assertEquals(UpdateItemOperation.MODIFY, sellerArrowOperation.operation)
        assertEquals(arrowsAmount - arrowsToBuyAmount, sellerArrowOperation.item.amount)

        val sellerSplinterOperation = sellerUpdateItems.operations
            .find { it.item.templateId == DemonSplinter.id }
        assertNotNull(sellerSplinterOperation)
        assertEquals(UpdateItemOperation.REMOVE, sellerSplinterOperation.operation)

        val sellerUpdateStatus = assertIs<UpdateStatusResponse>(sellerContext.responseChannel.receive())
        assertContains(sellerUpdateStatus.attributes, StatusAttribute.CUR_LOAD)

        transaction {
            // Check seller adena at database
            val sellerAdena = ItemEntity.findAllByOwnerIdAndTemplateId(seller.id, Adena.id).firstOrNull()
            assertNotNull(sellerAdena)
            assertEquals(totalPrice, sellerAdena.amount)

            // Check buyer adena at database
            val remainingBuyerAdena = ItemEntity.findAllByOwnerIdAndTemplateId(buyer.id, Adena.id).firstOrNull()
            assertNotNull(remainingBuyerAdena)
            assertEquals(initialBuyerAdena - totalPrice, remainingBuyerAdena.amount)

            // Check items at database
            val sellerArrow = ItemEntity.findAllByOwnerIdAndTemplateId(seller.id, WoodenArrow.id).firstOrNull()
            assertNotNull(sellerArrow)
            assertEquals(arrowsAmount - arrowsToBuyAmount, sellerArrow.amount)

            val buyerArrow = ItemEntity.findAllByOwnerIdAndTemplateId(buyer.id, WoodenArrow.id).firstOrNull()
            assertNotNull(buyerArrow)
            assertEquals(arrowsToBuyAmount, buyerArrow.amount)

            val buyerSplinter = ItemEntity.findAllByOwnerIdAndTemplateId(buyer.id, DemonSplinter.id).firstOrNull()
            assertNotNull(buyerSplinter)
            assertEquals(1, buyerSplinter.amount)
        }
    }

    @Test
    fun shouldFailToBuyWhenNotEnoughAdena(): Unit = runBlocking {
        // Create seller and customer
        val sellerContext = createTestSessionContext()
        val seller = createTestCharacter(name = "Seller")
        sellerContext.setCharacterId(seller.id)

        val buyerContext = createTestSessionContext()
        val buyer = createTestCharacter(name = "Buyer")
        buyerContext.setCharacterId(buyer.id)

        // Create goods
        val woodenArrow = createTestItem(WoodenArrow.id, seller, 1000)
        val arrowPrice = 1000

        // Create customer aden (lesser than price)
        createTestItem(Adena.id, buyer, 500)

        // Create store
        seller.posture = Posture.SITTING
        seller.privateStore = PrivateStore.Sell(
            title = "Test shop",
            items = listOf(woodenArrow.toItemOnSale(arrowPrice)),
            packageSale = false
        )

        // Try to buy
        withContext(buyerContext) {
            tradeService.buyInPrivateStore(BuyInPrivateStoreRequest(
                storeOwnerId = seller.id,
                items = listOf(RequestedToSellItem(woodenArrow.id, 1, arrowPrice))
            ))
        }

        // Check responses
        assertIs<SystemMessageResponse.NotEnoughAdena>(buyerContext.responseChannel.receive())
        assertIs<ActionFailedResponse>(buyerContext.responseChannel.receive())
    }

    @Test
    fun shouldFailToBuyMoreThanOnSale(): Unit = runBlocking {
        // Create seller and buyer
        val sellerContext = createTestSessionContext()
        val seller = createTestCharacter(name = "Seller")
        sellerContext.setCharacterId(seller.id)

        val buyerContext = createTestSessionContext()
        val buyer = createTestCharacter(name = "Buyer")
        buyerContext.setCharacterId(buyer.id)

        // Create goods
        val arrowsAmount = 1000
        val woodenArrow = createTestItem(WoodenArrow.id, seller, arrowsAmount)
        val arrowPrice = 2

        // Create store
        seller.posture = Posture.SITTING
        seller.privateStore = PrivateStore.Sell(
            title = "Test shop",
            //Sell just half of arrows we have
            items = listOf(woodenArrow.toItemOnSale(arrowPrice, arrowsAmount / 2)),
            packageSale = false
        )

        // Create customer's adena
        createTestItem(Adena.id, buyer, 1000000)

        // Buy (more than store has)
        withContext(buyerContext) {
            tradeService.buyInPrivateStore(BuyInPrivateStoreRequest(
                storeOwnerId = seller.id,
                items = listOf(RequestedToSellItem(woodenArrow.id, arrowsAmount - 2, arrowPrice))
            ))
        }

        // Check
        assertIs<ActionFailedResponse>(buyerContext.responseChannel.receive())

        transaction {
            val sellerArrow = ItemEntity.findAllByOwnerIdAndTemplateId(seller.id, WoodenArrow.id).firstOrNull()
            assertNotNull(sellerArrow)
            assertEquals(arrowsAmount, sellerArrow.amount)

            val buyerArrow = ItemEntity.findAllByOwnerIdAndTemplateId(buyer.id, WoodenArrow.id).firstOrNull()
            assertNull(buyerArrow)
        }
    }

    @Test
    fun shouldFailToBuyWhenStoreClosed(): Unit = runBlocking {
        // Create seller and customer
        val sellerContext = createTestSessionContext()
        val seller = createTestCharacter(name = "Seller")
        sellerContext.setCharacterId(seller.id)

        val buyerContext = createTestSessionContext()
        val buyer = createTestCharacter(name = "Buyer")
        buyerContext.setCharacterId(buyer.id)

        // Create items and adena
        val woodenArrow = createTestItem(WoodenArrow.id, seller, 1000)
        createTestItem(Adena.id, buyer, 1000000)

        // Try to buy
        withContext(buyerContext) {
            tradeService.buyInPrivateStore(BuyInPrivateStoreRequest(
                storeOwnerId = seller.id,
                items = listOf(RequestedToSellItem(woodenArrow.id, 1, 2))
            ))
        }

        // Check
        assertIs<ActionFailedResponse>(buyerContext.responseChannel.receive())
    }

    @Test
    fun shouldSendCharactersItemsForPrivateStoreBuy(): Unit = runBlocking {
        //Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Create our goods
        val adena = createTestItem(Adena.id, character, 1000)
        val demonSplinter = createTestItem(DemonSplinter.id, character)
        val heavensDivider = createTestItem(HeavensDivider.id, character)

        withContext(context) { tradeService.getItemsForPrivateStoreBuy() }

        val response = assertIs<ItemListForPrivateStoreBuyResponse>(context.responseChannel.receive())

        assertEquals(adena.amount, response.characterAdena)
        assertContains(response.itemsInInventory.map { it.itemId }, demonSplinter.id)
        assertContains(response.itemsInInventory.map { it.itemId }, heavensDivider.id)
        assertFalse(response.itemsInInventory.map { it.itemId }.contains(adena.id))
    }

    @Test
    fun shouldSuccessfullyStartPrivateStoreBuy(): Unit = runBlocking {
        //Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Create customer
        val customerContext = createTestSessionContext()
        val customerCharacter = createTestCharacter(name = "MrCustomer")
        customerContext.setCharacterId(customerCharacter.id)

        val testStoreMessage = "Baium Express"

        //Create items
        val arrowsAmount = 1000
        val arrowPrice = 2
        val woodenArrow = createTestItem(WoodenArrow.id, customerCharacter, arrowsAmount)

        val testWeaponsPrice = 50_000_000
        val demonSplinter = createTestItem(DemonSplinter.id, customerCharacter)
        val heavensDivider = createTestItem(HeavensDivider.id, customerCharacter)

        //Create adena
        createTestItem(Adena.id, character, testWeaponsPrice * 2 + arrowPrice * arrowsAmount)

        //Open our little store!
        withContext(context) {
            tradeService.setPrivateStoreBuyMessage(PrivateStoreBuySetMessageRequest(testStoreMessage))
            tradeService.startPrivateStoreBuy(PrivateStoreBuyStartRequest(
                items = listOf(
                    RequestedToBuyItem(woodenArrow.templateId, 0, arrowsAmount, arrowPrice),
                    RequestedToBuyItem(demonSplinter.templateId, 0, 1, testWeaponsPrice),
                    RequestedToBuyItem(heavensDivider.templateId, 0, 1, testWeaponsPrice)
                )
            ))
        }

        //Check our responses

        //First setMessageResponse is needed to it in private store interface
        val setStoreMessageResponse = assertIs<PrivateStoreBuySetMessageResponse>(context.responseChannel.receive())
        assertEquals(testStoreMessage, setStoreMessageResponse.message)

        val changePostureResponse = assertIs<ChangePostureResponse>(context.responseChannel.receive())
        assertEquals(Posture.SITTING, changePostureResponse.posture)

        val characterResponse = assertIs<FullCharacterResponse>(context.responseChannel.receive())
        assertNotNull(characterResponse.character.privateStore)

        val store = assertIs<PrivateStore.Buy>(characterResponse.character.privateStore!!)
        assertEquals(testStoreMessage, store.title)

        val wishedHeavensDivider = store.items.find { it.templateId == heavensDivider.templateId }
        assertNotNull(wishedHeavensDivider, "No Heavens Divider found in private store (buy)!")
        assertEquals(1, wishedHeavensDivider.amount)
        assertEquals(testWeaponsPrice, wishedHeavensDivider.price)

        val wishedDemonSplinter = store.items.find { it.templateId == heavensDivider.templateId }
        assertNotNull(wishedDemonSplinter)
        assertEquals(1, wishedDemonSplinter.amount)
        assertEquals(testWeaponsPrice, wishedDemonSplinter.price)

        val wishedWoodenArrows = store.items.find { it.templateId == woodenArrow.templateId }
        assertNotNull(wishedWoodenArrows)
        assertEquals(arrowsAmount, wishedWoodenArrows.amount)
        assertEquals(arrowPrice, wishedWoodenArrows.price)

        val storeMessageResponse = assertIs<PrivateStoreBuySetMessageResponse>(context.responseChannel.receive())
        assertEquals(testStoreMessage, storeMessageResponse.message)

        //Check customer responses
        val changePostureResponseForCustomer =
            assertIs<ChangePostureResponse>(customerContext.responseChannel.receive())
        assertEquals(Posture.SITTING, changePostureResponseForCustomer.posture)

        val characterResponseForCustomer = assertIs<CharacterInfoResponse>(customerContext.responseChannel.receive())
        assertNotNull(characterResponseForCustomer.character.privateStore)
        assertIs<PrivateStore.Buy>(characterResponse.character.privateStore!!)

        val storeMessageResponseForCustomer =
            assertIs<PrivateStoreBuySetMessageResponse>(customerContext.responseChannel.receive())
        assertEquals(testStoreMessage, storeMessageResponseForCustomer.message)
    }

    @Test
    fun shouldFailToStartPrivateStoreBuyCauseOfNotEnoughAdena(): Unit = runBlocking {
        //Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val testStoreMessage = "Baium Express"

        //Create our goods

        //Open our little store!
        withContext(context) {
            tradeService.setPrivateStoreBuyMessage(PrivateStoreBuySetMessageRequest(testStoreMessage))
            tradeService.startPrivateStoreBuy(PrivateStoreBuyStartRequest(
                items = listOf(
                    RequestedToBuyItem(WoodenArrow.id, 0, 1000, 1),
                    RequestedToBuyItem(DemonSplinter.id, 0, 1, 50_000_000),
                    RequestedToBuyItem(HeavensDivider.id, 0, 1, 50_000_000)
                )
            ))
        }

        //Check our responses
        val setStoreMessageResponse = assertIs<PrivateStoreBuySetMessageResponse>(context.responseChannel.receive())
        assertEquals(testStoreMessage, setStoreMessageResponse.message)

        assertIs<SystemMessageResponse.NotEnoughAdena>(context.responseChannel.receive())
    }

    @Test
    fun shouldFailToStartPrivateStoreBuyCauseOfNonSellableItems(): Unit = runBlocking {
        //Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val testStoreMessage = "Baium Express"

        createTestItem(Adena.id, character, 50_000)

        //Open our little store!
        val exception = assertThrows<IllegalArgumentException> {
            withContext(context) {
                tradeService.setPrivateStoreBuyMessage(PrivateStoreBuySetMessageRequest(testStoreMessage))
                tradeService.startPrivateStoreBuy(PrivateStoreBuyStartRequest(
                    items = listOf(RequestedToBuyItem(Adena.id, 0, 1000, 1))
                ))
            }
        }

        assertContains(exception.message!!, "is trying to buy non-sellable item in private store (buy)")
    }

    @Test
    fun shouldOpenCurrentStoreBuySettingsWindowIfStoreAlreadyExists(): Unit = runBlocking {
        //Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Create our goods
        val arrowsToBuyAmount = 1000
        val arrowsInInventoryAmount = 700
        val arrowPrice = 2
        val woodenArrow = createTestItem(
            WoodenArrow.id, character, arrowsInInventoryAmount
        )

        character.posture = Posture.SITTING
        character.privateStore = PrivateStore.Buy(
            title = "Wooden arrows - cheap and cheerful",
            items = listOf(woodenArrow.toItemInWishList(arrowPrice, arrowsToBuyAmount)),
        )

        //Get items for private store
        withContext(context) { tradeService.getItemsForPrivateStoreBuy() }

        //Check responses
        val standUpResponse = assertIs<ChangePostureResponse>(context.responseChannel.receive())
        assertEquals(Posture.STANDING, standUpResponse.posture)
        assertIs<FullCharacterResponse>(context.responseChannel.receive())

        val itemsForPrivateStoreBuyResponse = assertIs<ItemListForPrivateStoreBuyResponse>(
            context.responseChannel.receive())

        assertEquals(1, itemsForPrivateStoreBuyResponse.itemsInInventory.size)
        val woodenArrowInInventory = assertNotNull(
            itemsForPrivateStoreBuyResponse.itemsInInventory.find { it.itemId == woodenArrow.id }
        )
        assertEquals(arrowsInInventoryAmount, woodenArrowInInventory.amount)

        assertEquals(1, itemsForPrivateStoreBuyResponse.itemsInStore.size)
        val woodenArrowToBuy = assertNotNull(
            itemsForPrivateStoreBuyResponse.itemsInStore.find { it.templateId == woodenArrow.templateId }
        )
        assertEquals(arrowsToBuyAmount, woodenArrowToBuy.amount)
    }

    @Test
    fun shouldOpenOtherCharactersPrivateStoreBuy(): Unit = runBlocking {
        //Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Create customer
        val customerContext = createTestSessionContext()
        val customerCharacter = createTestCharacter(name = "Customer")
        customerContext.setCharacterId(customerCharacter.id)

        //Create goods
        val woodenArrowsToBuyAmount = 1000
        val woodenArrowsInInventoryAmount = 700
        val woodenArrowPrice = 2

        val woodenArrow = createTestItem(WoodenArrow.id, customerCharacter, woodenArrowsInInventoryAmount)

        val boneArrowsToBuyAmount = 1000
        val boneArrowsInInventoryAmount = 1500
        val boneArrowPrice = 3

        val boneArrow = createTestItem(BoneArrow.id, customerCharacter, boneArrowsInInventoryAmount)

        character.posture = Posture.SITTING
        character.privateStore = PrivateStore.Buy(
            title = "Arrows - cheap and cheerful!",
            items = listOf(
                woodenArrow.toItemInWishList(woodenArrowPrice, woodenArrowsToBuyAmount),
                boneArrow.toItemInWishList(boneArrowPrice, boneArrowsToBuyAmount)
            )
        )

        //Get items for private store
        withContext(context) { withContext(customerContext) { tradeService.showPrivateStoreOf(character) }}

        //Check responses
        val response = assertIs<ShowPrivateStoreBuyResponse>(customerContext.responseChannel.receive())
        assertEquals(character.id, response.ownerId)

        val privateStoreItems = (character.privateStore as? PrivateStore.Buy)?.items?.map { it.templateId }
        assertEquals(privateStoreItems, response.items.map { it.templateId })

        val woodenArrowsInResponse = response.items.find { it.templateId == WoodenArrow.id }
        assertNotNull(woodenArrowsInResponse)
        assertEquals(woodenArrowsInInventoryAmount, woodenArrowsInResponse.amount)

        val boneArrowsInResponse = response.items.find { it.templateId == BoneArrow.id }
        assertNotNull(boneArrowsInResponse)
        assertEquals(boneArrowsToBuyAmount, boneArrowsInResponse.amount)
    }

    @Test
    @Suppress("LongMethod")
    fun shouldSuccessfullySellItemsToPrivateStore(): Unit = runBlocking {
        // Create store owner
        val storeOwnerContext = createTestSessionContext()
        val storeOwner = createTestCharacter(name = "StoreOwner")
        storeOwnerContext.setCharacterId(storeOwner.id)
        val storeOwnerAdenaAmount = 1000000
        createTestItem(Adena.id, storeOwner, storeOwnerAdenaAmount)

        // Create seller
        val sellerContext = createTestSessionContext()
        val seller = createTestCharacter(name = "Seller")
        sellerContext.setCharacterId(seller.id)

        // Create items for sale
        val arrowsAmount = 1000
        val arrowPrice = 2
        val demonSplinterPrice = 50_000
        val woodenArrow = createTestItem(WoodenArrow.id, seller, arrowsAmount)
        val demonSplinter = createTestItem(DemonSplinter.id, seller)

        // Create store
        storeOwner.privateStore = PrivateStore.Buy(
            title = "Sell tour items here",
            items = listOf(
                woodenArrow.toItemInWishList(arrowPrice, arrowsAmount),
                demonSplinter.toItemInWishList(demonSplinterPrice)
            )
        )
        storeOwner.posture = Posture.SITTING

        val arrowsToSellAmount = 500
        val totalPrice = arrowsToSellAmount * arrowPrice + demonSplinterPrice
        // Sell items!
        withContext(sellerContext) {
            tradeService.sellToPrivateStore(SellToPrivateStoreRequest(
                storeOwnerId = storeOwner.id,
                items = listOf(
                    RequestedToSellToPrivateStoreItem(
                        woodenArrow.id,
                        woodenArrow.templateId,
                        0,
                        arrowsToSellAmount,
                        arrowPrice
                    ),
                    RequestedToSellToPrivateStoreItem(
                        demonSplinter.id,
                        demonSplinter.templateId,
                        0,
                        1,
                        demonSplinterPrice
                    )
                )
            ))
        }

        // Check sellers packets

        //System messages
        val soldArrowsSystemMessage = assertIs<SystemMessageResponse.OtherHasPurchasedStackable>(
            sellerContext.responseChannel.receive()
        )
        assertEquals(storeOwner.name, soldArrowsSystemMessage.customerName)
        assertEquals(500, soldArrowsSystemMessage.amount)
        assertEquals(WoodenArrow.id, soldArrowsSystemMessage.item.templateId)

        val soldDemonSplinterSystemMessage = assertIs<SystemMessageResponse.OtherHasPurchasedNonStackable>(
            sellerContext.responseChannel.receive()
        )
        assertEquals(storeOwner.name, soldDemonSplinterSystemMessage.customerName)
        assertEquals(DemonSplinter.id, soldDemonSplinterSystemMessage.item.templateId)

        // UpdateItems response of seller
        val sellerUpdateItems = assertIs<UpdateItemsResponse>(sellerContext.responseChannel.receive())
        assertEquals(3, sellerUpdateItems.operations.size)

        val sellerAdenaOperation = sellerUpdateItems.operations.find { it.item.templateId == Adena.id }
        assertNotNull(sellerAdenaOperation)
        assertEquals(UpdateItemOperation.ADD, sellerAdenaOperation.operation)
        assertEquals(totalPrice, sellerAdenaOperation.item.amount)

        val sellerArrowOperation = sellerUpdateItems.operations.find { it.item.templateId == WoodenArrow.id }
        assertNotNull(sellerArrowOperation)
        assertEquals(UpdateItemOperation.MODIFY, sellerArrowOperation.operation)
        assertEquals(arrowsAmount - arrowsToSellAmount, sellerArrowOperation.item.amount)

        val sellerSplinterOperation = sellerUpdateItems.operations.find { it.item.templateId == DemonSplinter.id }
        assertNotNull(sellerSplinterOperation)
        assertEquals(UpdateItemOperation.REMOVE, sellerSplinterOperation.operation)

        // Update cur load of seller
        val sellerUpdateStatus = assertIs<UpdateStatusResponse>(sellerContext.responseChannel.receive())
        assertEquals(StatusAttribute.CUR_LOAD, sellerUpdateStatus.attributes.keys.first())

        // Check store owner packets
        val boughtArrowsSystemMessage = assertIs<SystemMessageResponse.YouHavePurchasedStackable>(
            storeOwnerContext.responseChannel.receive()
        )
        assertEquals(seller.name, boughtArrowsSystemMessage.sellerName)
        assertEquals(arrowsToSellAmount, boughtArrowsSystemMessage.amount)
        assertEquals(WoodenArrow.id, boughtArrowsSystemMessage.item.templateId)

        val storeOwnerSystemMessage = assertIs<SystemMessageResponse.YouHavePurchasedNonStackable>(
            storeOwnerContext.responseChannel.receive()
        )
        assertEquals(seller.name, storeOwnerSystemMessage.sellerName)
        assertEquals(DemonSplinter.id, storeOwnerSystemMessage.item.templateId)

        // UpdateItems response of seller
        val storeOwnerUpdateItems = assertIs<UpdateItemsResponse>(storeOwnerContext.responseChannel.receive())
        assertEquals(3, storeOwnerUpdateItems.operations.size)

        val storeOwnerAdenaOperation = storeOwnerUpdateItems.operations.find { it.item.templateId == Adena.id }
        assertNotNull(storeOwnerAdenaOperation)
        assertEquals(UpdateItemOperation.MODIFY, storeOwnerAdenaOperation.operation)
        assertEquals(storeOwnerAdenaAmount - totalPrice, storeOwnerAdenaOperation.item.amount)

        val storeOwnerArrowOperation = storeOwnerUpdateItems.operations.find { it.item.templateId == WoodenArrow.id }
        assertNotNull(storeOwnerArrowOperation)
        assertEquals(UpdateItemOperation.ADD, storeOwnerArrowOperation.operation)
        assertEquals(arrowsToSellAmount, storeOwnerArrowOperation.item.amount)

        val storeOwnerSplinterOperation = storeOwnerUpdateItems.operations.find {
            it.item.templateId == DemonSplinter.id
        }
        assertNotNull(storeOwnerSplinterOperation)
        assertEquals(UpdateItemOperation.ADD, storeOwnerSplinterOperation.operation)
        assertEquals(1, storeOwnerSplinterOperation.item.amount)

        val storeOwnerUpdateStatus = assertIs<UpdateStatusResponse>(storeOwnerContext.responseChannel.receive())
        assertEquals(StatusAttribute.CUR_LOAD, storeOwnerUpdateStatus.attributes.keys.first())

        transaction {
            // Check adena in database
            val storeOwnerAdenaInDb = ItemEntity
                .findAllByOwnerIdAndTemplateId(storeOwner.id, Adena.id).firstOrNull()
            assertNotNull(storeOwnerAdenaInDb)
            assertEquals(storeOwnerAdenaAmount - totalPrice, storeOwnerAdenaInDb.amount)

            val sellerAdenaInDb = ItemEntity
                .findAllByOwnerIdAndTemplateId(seller.id, Adena.id).firstOrNull()
            assertNotNull(sellerAdenaInDb)
            assertEquals(totalPrice, sellerAdenaInDb.amount)

            // Check items in database
            val storeOwnerArrow = ItemEntity
                .findAllByOwnerIdAndTemplateId(storeOwner.id, WoodenArrow.id).firstOrNull()
            assertNotNull(storeOwnerArrow)
            assertEquals(arrowsToSellAmount, storeOwnerArrow.amount)

            val sellerArrow = ItemEntity
                .findAllByOwnerIdAndTemplateId(seller.id, WoodenArrow.id).firstOrNull()
            assertNotNull(sellerArrow)
            assertEquals(arrowsAmount - arrowsToSellAmount, sellerArrow.amount)

            val storeOwnerSplinter = ItemEntity
                .findAllByOwnerIdAndTemplateId(storeOwner.id, DemonSplinter.id).firstOrNull()
            assertNotNull(storeOwnerSplinter)
            assertEquals(1, storeOwnerSplinter.amount)

            val sellerSplinter = ItemEntity
                .findAllByOwnerIdAndTemplateId(seller.id, DemonSplinter.id).firstOrNull()
            assertNull(sellerSplinter)
        }
    }

    @Test
    fun shouldFailToSellWhenStoreOwnerNotEnoughAdena(): Unit = runBlocking {
        // Create store owner and customer
        val storeOwnerContext = createTestSessionContext()
        val storeOwner = createTestCharacter(name = "StoreOwner")
        storeOwnerContext.setCharacterId(storeOwner.id)
        val storeOwnerAdenaAmountBefore = 500
        createTestItem(Adena.id, storeOwner, storeOwnerAdenaAmountBefore)

        val sellerContext = createTestSessionContext()
        val seller = createTestCharacter(name = "Seller")
        sellerContext.setCharacterId(seller.id)

        // Create item to sell
        val woodenArrow = createTestItem(WoodenArrow.id, seller, 1000)
        val arrowPrice = storeOwnerAdenaAmountBefore * 2

        // Create store
        storeOwner.privateStore = PrivateStore.Buy(
            title = "Sell tour items here",
            items = listOf(woodenArrow.toItemInWishList(arrowPrice))
        )
        storeOwner.posture = Posture.SITTING

        // Try to sell!
        withContext(sellerContext) {
            tradeService.sellToPrivateStore(SellToPrivateStoreRequest(
                storeOwnerId = storeOwner.id,
                items = listOf(
                    RequestedToSellToPrivateStoreItem(
                        woodenArrow.id,
                        woodenArrow.templateId,
                        0,
                        1,
                        arrowPrice
                    ))
            ))
        }

        // Check action failed response
        assertIs<ActionFailedResponse>(sellerContext.responseChannel.receive())

        // Verify that transaction did not occur by checking database state
        transaction {
            val storeOwnerAdenaAmountAfter = ItemEntity
                .findAllByOwnerIdAndTemplateId(storeOwner.id, Adena.id).firstOrNull()?.amount ?: 0
            assertEquals(
                storeOwnerAdenaAmountBefore,
                storeOwnerAdenaAmountAfter,
                "Store owner's adena should remain unchanged"
            )

            val sellerAdena = ItemEntity
                .findAllByOwnerIdAndTemplateId(seller.id, Adena.id).firstOrNull()
            assertNull(sellerAdena, "Seller should not have received any adena")

            val storeOwnerArrow = ItemEntity
                .findAllByOwnerIdAndTemplateId(storeOwner.id, WoodenArrow.id).firstOrNull()
            assertNull(storeOwnerArrow, "Store owner should not have received any arrows")

            val sellerArrow = ItemEntity
                .findAllByOwnerIdAndTemplateId(seller.id, WoodenArrow.id).firstOrNull()
            assertNotNull(sellerArrow)
            assertEquals(1000, sellerArrow.amount, "Seller's arrows should remain unchanged")
        }
    }

    @Test
    fun shouldFailToSellWhenItemsNotInInventory(): Unit = runBlocking {
        // Create store owner and customer
        val storeOwnerContext = createTestSessionContext()
        val storeOwner = createTestCharacter(name = "StoreOwner")
        storeOwnerContext.setCharacterId(storeOwner.id)
        val storeOwnerAdenaAmountBefore = 1_000_000
        createTestItem(Adena.id, storeOwner, storeOwnerAdenaAmountBefore)

        val sellerContext = createTestSessionContext()
        val seller = createTestCharacter(name = "Seller")
        sellerContext.setCharacterId(seller.id)

        // Create item to sell
        val woodenArrowAmount = 1000
        val woodenArrow = createTestItem(WoodenArrow.id, seller, woodenArrowAmount)
        val arrowPrice = 2

        // Create store
        storeOwner.privateStore = PrivateStore.Buy(
            title = "Sell tour items here",
            items = listOf(woodenArrow.toItemInWishList(arrowPrice))
        )
        storeOwner.posture = Posture.SITTING

        // Try to sell more than we have
        withContext(sellerContext) {
            tradeService.sellToPrivateStore(SellToPrivateStoreRequest(
                storeOwnerId = storeOwner.id,
                items = listOf(
                    RequestedToSellToPrivateStoreItem(
                        woodenArrow.id,
                        woodenArrow.templateId,
                        0,
                        woodenArrowAmount * 2,
                        arrowPrice
                    )
                )
            ))
        }

        // Check action failed
        assertIs<ActionFailedResponse>(sellerContext.responseChannel.receive())

        transaction {
            // Check items in database
            val storeOwnerAdena = ItemEntity.findAllByOwnerIdAndTemplateId(storeOwner.id, Adena.id).firstOrNull()
            assertNotNull(storeOwnerAdena)
            assertEquals(storeOwnerAdenaAmountBefore, storeOwnerAdena.amount)

            val sellerArrow = ItemEntity.findAllByOwnerIdAndTemplateId(seller.id, WoodenArrow.id).firstOrNull()
            assertNotNull(sellerArrow)
            assertEquals(woodenArrowAmount, sellerArrow.amount)

            val storeOwnerArrow = ItemEntity.findAllByOwnerIdAndTemplateId(storeOwner.id, WoodenArrow.id).firstOrNull()
            assertNull(storeOwnerArrow)
        }
    }

    @Test
    fun shouldFailToSellWhenStoreClosed(): Unit = runBlocking {
        // Create store owner and customer
        val storeOwnerContext = createTestSessionContext()
        val storeOwner = createTestCharacter(name = "StoreOwner")
        storeOwnerContext.setCharacterId(storeOwner.id)
        val storeOwnerAdenaAmountBefore = 1_000_000
        createTestItem(Adena.id, storeOwner, storeOwnerAdenaAmountBefore)

        val sellerContext = createTestSessionContext()
        val seller = createTestCharacter(name = "Seller")
        sellerContext.setCharacterId(seller.id)
        val woodenArrowAmount = 1000
        val woodenArrow = createTestItem(WoodenArrow.id, seller, woodenArrowAmount)

        // Try to sell to non-existent store
        withContext(sellerContext) {
            tradeService.sellToPrivateStore(SellToPrivateStoreRequest(
                storeOwnerId = storeOwner.id,
                items = listOf(RequestedToSellToPrivateStoreItem(woodenArrow.id, woodenArrow.templateId, 0, 1, 2))
            ))
        }

        // Check action failed
        assertIs<ActionFailedResponse>(sellerContext.responseChannel.receive())

        transaction {
            // Check items in database
            val storeOwnerAdena = ItemEntity.findAllByOwnerIdAndTemplateId(storeOwner.id, Adena.id).firstOrNull()
            assertNotNull(storeOwnerAdena)
            assertEquals(storeOwnerAdenaAmountBefore, storeOwnerAdena.amount)

            val sellerArrow = ItemEntity.findAllByOwnerIdAndTemplateId(seller.id, WoodenArrow.id).firstOrNull()
            assertNotNull(sellerArrow)
            assertEquals(woodenArrowAmount, sellerArrow.amount)

            val storeOwnerArrow = ItemEntity.findAllByOwnerIdAndTemplateId(storeOwner.id, WoodenArrow.id).firstOrNull()
            assertNull(storeOwnerArrow)
        }
    }

    @Test
    fun shouldFailToSellWhenTooFar(): Unit = runBlocking {
        // Create store owner and customer
        val storeOwnerContext = createTestSessionContext()
        val storeOwner = createTestCharacter(name = "StoreOwner")
        storeOwnerContext.setCharacterId(storeOwner.id)
        val storeOwnerAdenaAmountBefore = 1_000_000
        createTestItem(Adena.id, storeOwner, storeOwnerAdenaAmountBefore)

        val sellerContext = createTestSessionContext()
        val seller = createTestCharacter(name = "Seller")
        sellerContext.setCharacterId(seller.id)
        val arrowPrice = 2
        val woodenArrowAmount = 1000
        val woodenArrow = createTestItem(WoodenArrow.id, seller, woodenArrowAmount)

        // Create store
        storeOwner.privateStore = PrivateStore.Buy(
            title = "Sell tour items here",
            items = listOf(woodenArrow.toItemInWishList(arrowPrice))
        )
        storeOwner.posture = Posture.SITTING

        // Teleport seller to far, far away
        transaction { seller.position = Position(1000, 1000, 0) }

        // Try to sell
        withContext(sellerContext) {
            tradeService.sellToPrivateStore(SellToPrivateStoreRequest(
                storeOwnerId = storeOwner.id,
                items = listOf(RequestedToSellToPrivateStoreItem(woodenArrow.id, woodenArrow.templateId, 0, 1, 2))
            ))
        }

        // Check packets
        val soundResponse = assertIs<PlaySoundResponse>(sellerContext.responseChannel.receive())
        assertEquals(Sound.ITEMSOUND_SYS_SHORTAGE, soundResponse.sound)
        
        assertIs<ActionFailedResponse>(sellerContext.responseChannel.receive())

        transaction {
            // Check items in database
            val storeOwnerAdena = ItemEntity
                .findAllByOwnerIdAndTemplateId(storeOwner.id, Adena.id).firstOrNull()
            assertNotNull(storeOwnerAdena)
            assertEquals(storeOwnerAdenaAmountBefore, storeOwnerAdena.amount)

            val sellerArrow = ItemEntity
                .findAllByOwnerIdAndTemplateId(seller.id, WoodenArrow.id).firstOrNull()
            assertNotNull(sellerArrow)
            assertEquals(woodenArrowAmount, sellerArrow.amount)

            val storeOwnerArrow = ItemEntity
                .findAllByOwnerIdAndTemplateId(storeOwner.id, WoodenArrow.id).firstOrNull()
            assertNull(storeOwnerArrow)
        }
    }

}
