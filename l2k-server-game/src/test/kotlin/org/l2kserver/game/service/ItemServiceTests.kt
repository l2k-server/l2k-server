package org.l2kserver.game.service

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.test.Test
import org.junit.jupiter.api.assertThrows
import org.l2kserver.game.AbstractTests
import org.l2kserver.game.data.item.armor.LEATHER_SHIELD
import org.l2kserver.game.data.item.arrows.BONE_ARROW
import org.l2kserver.game.data.item.arrows.WOODEN_ARROW
import org.l2kserver.game.data.item.weapons.DAGGER
import org.l2kserver.game.data.item.weapons.HEAVENS_DIVIDER
import org.l2kserver.game.data.item.weapons.SQUIRES_SWORD
import org.l2kserver.game.data.item.weapons.WILLOW_STAFF
import org.l2kserver.game.extensions.model.item.createAllFrom
import org.l2kserver.game.handler.dto.request.DeleteItemRequest
import org.l2kserver.game.handler.dto.request.DropItemRequest
import org.l2kserver.game.handler.dto.request.TakeOffItemRequest
import org.l2kserver.game.handler.dto.request.UseItemRequest
import org.l2kserver.game.handler.dto.response.FullCharacterResponse
import org.l2kserver.game.handler.dto.response.DroppedItemResponse
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.handler.dto.response.UpdateItemOperationType
import org.l2kserver.game.handler.dto.response.UpdateItemsResponse
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.item.Item
import org.l2kserver.game.domain.ItemTable
import org.l2kserver.game.extensions.model.item.existsById
import org.l2kserver.game.extensions.model.item.findAllByOwnerIdAndTemplateId
import org.l2kserver.game.extensions.model.item.findAllEquippedByOwnerId
import org.l2kserver.game.extensions.toItemOnSale
import org.l2kserver.game.handler.dto.response.DeleteObjectResponse
import org.l2kserver.game.handler.dto.response.PickUpItemResponse
import org.l2kserver.game.handler.dto.response.StatusAttribute
import org.l2kserver.game.model.PaperDoll
import org.l2kserver.game.model.actor.Posture
import org.l2kserver.game.model.actor.character.InitialItem
import org.l2kserver.game.model.item.ItemTemplate
import org.l2kserver.game.model.item.Slot
import org.l2kserver.game.model.store.PrivateStore
import org.springframework.beans.factory.annotation.Autowired
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ItemServiceTests(
    @Autowired private val itemService: ItemService
) : AbstractTests() {

    @Test
    fun shouldSuccessfullyDeleteItem(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val itemId = createTestItem(2369, character.id).id

        withContext(context) { itemService.deleteItem(DeleteItemRequest(itemId, 1)) }

        assertFalse(Item.existsById(itemId), "Item must not exist")

        val updateResponse = assertIs<UpdateItemsResponse>(context.responseChannel.receive())
        val (item, operation) = updateResponse.operations[0]
        assertEquals(itemId, item.id)
        assertEquals(UpdateItemOperationType.REMOVE, operation)

        val updateStatusResponse = assertIs<UpdateStatusResponse>(context.responseChannel.receive())
        assertEquals(StatusAttribute.CUR_LOAD, updateStatusResponse.attributes.keys.first())
    }

    @Test
    fun shouldSuccessfullyDeleteEquippedItem(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val itemId = createTestItem(
            templateId = 2369,
            ownerId = character.id,
            isEquipped = true
        ).id

        withContext(context) { itemService.deleteItem(DeleteItemRequest(itemId, 1)) }

        newSuspendedTransaction {
            assertFalse(Item.existsById(itemId), "Deleted item should not exist")

            assertIs<FullCharacterResponse>(context.responseChannel.receive())

            val updateResponse = assertIs<UpdateItemsResponse>(context.responseChannel.receive())
            assertEquals(itemId, updateResponse.operations[0].item.id)
            assertEquals(UpdateItemOperationType.MODIFY, updateResponse.operations[0].operationType)
            assertEquals(itemId, updateResponse.operations[1].item.id)
            assertEquals(UpdateItemOperationType.REMOVE, updateResponse.operations[1].operationType)

            val updateStatusResponse = assertIs<UpdateStatusResponse>(context.responseChannel.receive())
            assertEquals(character.id, updateStatusResponse.objectId)
        }

    }

    @Test
    fun shouldSuccessfullyDeleteItemPartially(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val itemId = createTestItem(
            templateId = 57,
            ownerId = character.id,
            amount = 10
        ).id

        withContext(context) { itemService.deleteItem(DeleteItemRequest(itemId, 1)) }

        newSuspendedTransaction {
            assertEquals(9, Item.findById(itemId).amount)

            val updatedItems = assertIs<UpdateItemsResponse>(context.responseChannel.receive())
            val (item, operation) = updatedItems.operations[0]
            assertEquals(itemId, item.id)
            assertEquals(UpdateItemOperationType.MODIFY, operation)
        }
    }

    @Test
    fun shouldFailIfTryingToDeleteMoreItemsThanHas(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val testAmount = 10
        val itemId = createTestItem(
            templateId = 57,
            ownerId = character.id,
            amount = testAmount
        ).id

        withContext(context) { itemService.deleteItem(DeleteItemRequest(itemId, 1000)) }

        newSuspendedTransaction {
            assertEquals(testAmount, Item.findById(itemId).amount)

            assertIs<SystemMessageResponse.NotEnoughItems>(context.responseChannel.receive())
        }
    }

    @Test
    fun shouldSuccessfullyTakeOffEquippedItem(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val itemId = createTestItem(
            templateId = 2369,
            ownerId = character.id,
            isEquipped = true
        ).id

        withContext(context) { itemService.useItem(UseItemRequest(itemId)) }

        newSuspendedTransaction {
            assertIs<SystemMessageResponse>(context.responseChannel.receive())
            val updateItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.receive())

            val (item, operation) = updateItemsResponse.operations[0]
            assertEquals(itemId, item.id)
            assertEquals(UpdateItemOperationType.MODIFY, operation)


            assertIs<FullCharacterResponse>(context.responseChannel.receive())

            assertFalse(Item.findById(itemId).isEquipped)
        }
    }

    @Test
    fun shouldSuccessfullyTakeOffEquippedItemByDragNDrop(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        newSuspendedTransaction { ItemTable.deleteAll() }
        val itemId = createTestItem(
            templateId = 2369,
            ownerId = character.id,
            isEquipped = true
        ).id

        newSuspendedTransaction { character.paperDoll = PaperDoll(Item.findAllEquippedByOwnerId(character.id)) }

        withContext(context) { itemService.takeOffItem(TakeOffItemRequest(Slot.RIGHT_HAND)) }

        newSuspendedTransaction {
            assertIs<SystemMessageResponse>(context.responseChannel.receive())
            val updateItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.receive())

            val (item, operation) = updateItemsResponse.operations[0]
            assertEquals(itemId, item.id)
            assertEquals(UpdateItemOperationType.MODIFY, operation)


            assertIs<FullCharacterResponse>(context.responseChannel.receive())

            assertFalse(Item.findById(itemId).isEquipped)
        }
    }

    @Test
    fun shouldEquipWeaponIfAnotherWeaponIsEquipped(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val items = newSuspendedTransaction {
            ItemTable.deleteAll() // Delete initial items

            val items = Item.createAllFrom(
                ownerId = character.id,
                initialItems = listOf(
                    InitialItem(SQUIRES_SWORD.id,isEquipped = true),
                    InitialItem(DAGGER.id)
                )
            ).toList()

            character.paperDoll = PaperDoll(Item.findAllEquippedByOwnerId(character.id))
            items
        }
        withContext(context) { itemService.useItem(UseItemRequest(items[1].id)) }

        newSuspendedTransaction {
            assertIs<SystemMessageResponse>(context.responseChannel.receive())
            assertIs<SystemMessageResponse>(context.responseChannel.receive())
            assertIs<FullCharacterResponse>(context.responseChannel.receive())
            val updatedItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.receive())

            val (takenOffItem, operation1) = updatedItemsResponse.operations[0]
            assertEquals(items[0].id, takenOffItem.id)
            assertFalse(takenOffItem.isEquipped)
            assertEquals(UpdateItemOperationType.MODIFY, operation1)

            val (usedItem, operation2) = updatedItemsResponse.operations[1]
            assertEquals(items[1].id, usedItem.id)
            assertEquals(UpdateItemOperationType.MODIFY, operation2)

            assertIs<FullCharacterResponse>(context.responseChannel.receive())

            assertTrue(Item.findById(usedItem.id).isEquipped)
        }
    }

    @Test
    fun shouldEquipTwoHandedWeaponIfAnotherWeaponAndShieldIsEquipped(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val items = newSuspendedTransaction {
            ItemTable.deleteAll() // Delete initial items

            val items = Item.createAllFrom(
                ownerId = character.id,
                initialItems = listOf(
                    InitialItem(SQUIRES_SWORD.id, isEquipped = true),
                    InitialItem(LEATHER_SHIELD.id,isEquipped = true),
                    InitialItem(id = WILLOW_STAFF.id)
                )
            )
            character.paperDoll = PaperDoll(Item.findAllEquippedByOwnerId(character.id))
            items
        }


        withContext(context) { itemService.useItem(UseItemRequest(items[2].id)) }

        newSuspendedTransaction {
            assertIs<SystemMessageResponse>(context.responseChannel.receive())
            assertIs<SystemMessageResponse>(context.responseChannel.receive())
            assertIs<SystemMessageResponse>(context.responseChannel.receive())
            assertIs<FullCharacterResponse>(context.responseChannel.receive())

            val updateItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.receive())
            assertEquals(3, updateItemsResponse.operations.size)

            val (takenOffItem1, operation1) = updateItemsResponse.operations[0]
            assertEquals(items[0].id, takenOffItem1.id)
            assertFalse(takenOffItem1.isEquipped)
            assertEquals(UpdateItemOperationType.MODIFY, operation1)

            val (takenOffItem2, operation2) = updateItemsResponse.operations[1]
            assertEquals(items[1].id, takenOffItem2.id)
            assertFalse(takenOffItem2.isEquipped)
            assertEquals(UpdateItemOperationType.MODIFY, operation2)

            val (usedItem, operation3) = updateItemsResponse.operations[2]
            assertEquals(items[2].id, usedItem.id)
            assertEquals(UpdateItemOperationType.MODIFY, operation3)

            assertIs<FullCharacterResponse>(context.responseChannel.receive())

            assertTrue(Item.findById(usedItem.id).isEquipped)
        }
    }

    @Test
    fun shouldSuccessfullyDropItem(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val item = createTestItem(WILLOW_STAFF.id, character.id)

        withContext(context) {
            itemService.dropItem(
                DropItemRequest(
                    itemId = item.id,
                    amount = item.amount,
                    position = Position(
                        x = character.position.x,
                        y = character.position.y,
                        z = character.position.z
                    )
                )
            )
        }

        newSuspendedTransaction {
            val droppedItemResponse = assertIs<DroppedItemResponse>(context.responseChannel.receive())
            assertEquals(character.id, droppedItemResponse.dropperId)
            assertFalse(Item.existsById(item.id), "Item must not exist")

            val updateItemResponse = assertIs<UpdateItemsResponse>(context.responseChannel.receive())
            val (updatedItem, operation) = updateItemResponse.operations[0]
            assertEquals(item.id, updatedItem.id)
            assertEquals(UpdateItemOperationType.REMOVE, operation)

            val updateStatusResponse = assertIs<UpdateStatusResponse>(context.responseChannel.receive())
            assertEquals(character.id, updateStatusResponse.objectId)

            assertNotNull(gameObjectRepository.findByIdOrNull(droppedItemResponse.scatteredItem.id))
        }
    }

    @Test
    fun shouldSuccessfullyDropEquippedItem(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val itemId = createTestItem(WILLOW_STAFF.id, character.id, isEquipped = true).id

        withContext(context) {
            itemService.dropItem(
                DropItemRequest(
                    itemId = itemId,
                    amount = 1,
                    position = Position(
                        x = character.position.x,
                        y = character.position.y,
                        z = character.position.z
                    )
                )
            )
        }


        newSuspendedTransaction {
            assertFalse(Item.existsById(itemId), "Deleted item should not exist")

            val droppedItemResponse = assertIs<DroppedItemResponse>(context.responseChannel.receive())
            assertEquals(character.id, droppedItemResponse.dropperId)
            assertEquals(1, droppedItemResponse.scatteredItem.amount)

            assertIs<FullCharacterResponse>(context.responseChannel.receive())

            val updateResponse = assertIs<UpdateItemsResponse>(context.responseChannel.receive())
            assertEquals(itemId, updateResponse.operations[0].item.id)
            assertEquals(UpdateItemOperationType.MODIFY, updateResponse.operations[0].operationType)
            assertEquals(itemId, updateResponse.operations[1].item.id)
            assertEquals(UpdateItemOperationType.REMOVE, updateResponse.operations[1].operationType)

            assertIs<UpdateStatusResponse>(context.responseChannel.receive())
        }

    }

    @Test
    fun shouldSuccessfullyDropItemPartially(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val itemId = createTestItem(57, character.id, 10).id

        withContext(context) {
            itemService.dropItem(
                DropItemRequest(
                    itemId = itemId,
                    amount = 2,
                    position = Position(
                        x = character.position.x,
                        y = character.position.y,
                        z = character.position.z
                    )
                )
            )
        }

        newSuspendedTransaction {
            assertEquals(8, Item.findById(itemId).amount)

            val droppedItemResponse = assertIs<DroppedItemResponse>(context.responseChannel.receive())
            assertEquals(character.id, droppedItemResponse.dropperId)
            assertEquals(2, droppedItemResponse.scatteredItem.amount)

            val updatedItems = assertIs<UpdateItemsResponse>(context.responseChannel.receive())
            val (item, operation) = updatedItems.operations[0]
            assertEquals(itemId, item.id)
            assertEquals(UpdateItemOperationType.MODIFY, operation)

            assertIs<UpdateStatusResponse>(context.responseChannel.receive())
        }

    }

    @Test
    fun shouldFailIfTryingToDropMoreItemsThanHas(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val testAmount = 10
        val itemId = createTestItem(57, character.id, testAmount).id

        withContext(context) {
            itemService.dropItem(
                DropItemRequest(
                    itemId = itemId,
                    amount = 1000,
                    position = Position(
                        x = character.position.x,
                        y = character.position.y,
                        z = character.position.z
                    )
                )
            )
        }

        assertEquals(testAmount, newSuspendedTransaction { Item.findById(itemId).amount })

        assertIs<SystemMessageResponse.NotEnoughItems>(context.responseChannel.receive())
    }

    @Test
    fun shouldGetErrorIfDroppingSomeoneElseItem(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()

        context.setCharacterId(character.id)

        val anotherCharacter = createTestCharacter(name = "${testCharacterName}2")

        val item = createTestItem(8, anotherCharacter.id)

        assertThrows<IllegalArgumentException> {
            withContext(context) {
                itemService.dropItem(
                    DropItemRequest(
                        itemId = item.id,
                        amount = item.amount,
                        position = Position(
                            x = character.position.x,
                            y = character.position.y,
                            z = character.position.z
                        )
                    )
                )
            }
        }
    }

    @Test
    fun shouldGetErrorWhenDroppingNonExistentItem(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        assertThrows<IllegalArgumentException> {
            withContext(context) {
                itemService.dropItem(
                    DropItemRequest(
                        itemId = Random.nextInt(),
                        amount = 1,
                        position = Position(
                            x = character.position.x,
                            y = character.position.y,
                            z = character.position.z
                        )
                    )
                )
            }
        }
        //TODO Ban?
    }

    @Test
    fun shouldGetErrorWhenDroppingUndroppableItem(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val item = createTestItem(10, character.id)

        withContext(context) {
            itemService.dropItem(
                DropItemRequest(
                    itemId = item.id,
                    amount = item.amount,
                    position = Position(
                        x = character.position.x,
                        y = character.position.y,
                        z = character.position.z
                    )
                )
            )
        }

        assertIs<SystemMessageResponse.CannotDiscardItem>(context.responseChannel.receive())
    }

    @Test
    fun shouldGetErrorWhenDroppingItemTooFar(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val item = createTestItem(WILLOW_STAFF.id, character.id)

        withContext(context) {
            itemService.dropItem(
                DropItemRequest(
                    itemId = item.id,
                    amount = item.amount,
                    position = Position(
                        x = character.position.x + 2000,
                        y = character.position.y,
                        z = character.position.z
                    )
                )
            )
        }

        assertIs<SystemMessageResponse.TooFarToDiscard>(context.responseChannel.receive())
    }

    @Test
    fun shouldFailDeletingItemWhileInPrivateStore(): Unit = runBlocking {
        //Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Create store
        val woodenArrow = createTestItem(WOODEN_ARROW.id, character.id, 100)
        character.posture = Posture.SITTING
        character.privateStore = PrivateStore.Sell(
            title = "Wooden arrows - cheap and cheerful",
            items = listOf(woodenArrow.toItemOnSale(woodenArrow.amount, 2)),
            packageSale = true
        )
        val boneArrowId = createTestItem(BONE_ARROW.id, character.id, 100).id

        //Then
        withContext(context) { itemService.deleteItem(DeleteItemRequest(boneArrowId, 1)) }

        // Check responses
        assertIs<SystemMessageResponse.CannotDiscardDestroyOrTradeWhileInShop>(context.responseChannel.receive())
    }

    @Test
    fun shouldFailDroppingItemWhileInPrivateStore(): Unit = runBlocking {
        // Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        // Create store
        val woodenArrow = createTestItem(WOODEN_ARROW.id, character.id, 100)
        character.posture = Posture.SITTING
        character.privateStore = PrivateStore.Sell(
            title = "Wooden arrows - cheap and cheerful",
            items = listOf(woodenArrow.toItemOnSale(woodenArrow.amount, 2)),
            packageSale = true
        )
        val boneArrowId = createTestItem(BONE_ARROW.id, character.id, 100).id

        // Then
        withContext(context) { itemService.dropItem(DropItemRequest(boneArrowId, 1, character.position)) }

        // Check responses
        assertIs<SystemMessageResponse.CannotDiscardDestroyOrTradeWhileInShop>(context.responseChannel.receive())
    }

    @Test
    fun shouldFailUsingItemWhichIsUsedInPrivateStore(): Unit = runBlocking {
        // Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        // Create store
        val heavensDivider = createTestItem(HEAVENS_DIVIDER.id, character.id, 1)
        character.posture = Posture.SITTING
        character.privateStore = PrivateStore.Sell(
            title = "Wooden arrows - cheap and cheerful",
            items = listOf(heavensDivider.toItemOnSale(heavensDivider.price)),
            packageSale = true
        )

        // Then
        withContext(context) { itemService.useItem(UseItemRequest(heavensDivider.id)) }

        // Check responses
        val response = assertIs<SystemMessageResponse.ItemCannotBeUsed>(context.responseChannel.receive())
        assertEquals(heavensDivider.id, response.item.id, "Used and failed to use item ids must be equal")
    }

    @Test
    fun shouldPickUpItem(): Unit = runBlocking {
        // Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Create already existing item
        val existingItem = createTestItem(HEAVENS_DIVIDER.id, character.id)

        //Create scattered item
        val scatteredItem = createTestScatteredItem(
            character.position, ItemTemplate.Registry.findById(HEAVENS_DIVIDER.id)!!)

        //Pick up item!
        withContext(context) {
            itemService.launchPickUp(character, scatteredItem).join()
        }

        //Assert pick up animation
        val pickUpResponse = assertIs<PickUpItemResponse>(context.responseChannel.receive())
        assertEquals(character.id, pickUpResponse.characterId, "Must get PickUpResponse of $character")
        assertEquals(scatteredItem, pickUpResponse.item)

        //Assert deleting scatteredItem notification
        val deleteObjectResponse = assertIs<DeleteObjectResponse>(context.responseChannel.receive())
        assertEquals(scatteredItem.id, deleteObjectResponse.gameObjectId)

        val updateItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.receive())
        assertEquals(UpdateItemOperationType.ADD, updateItemsResponse.operations.first().operationType)
        assertEquals(HEAVENS_DIVIDER.id, updateItemsResponse.operations.first().item.templateId)
        assertNotEquals(existingItem.id, updateItemsResponse.operations.first().item.id)

        assertIs<UpdateStatusResponse>(context.responseChannel.receive())

        assertIs<SystemMessageResponse.YouHaveObtained>(context.responseChannel.receive())
        assertFalse(gameObjectRepository.existsById(scatteredItem.id), "Picked up item must disappear")
        assertEquals(2, Item.findAllByOwnerIdAndTemplateId(character.id, HEAVENS_DIVIDER.id).size)
    }

    @Test
    fun shouldPickUpStackableItem(): Unit = runBlocking {
        // Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Create already existing item
        createTestItem(WOODEN_ARROW.id, character.id, 100)

        //Create scattered item
        val scatteredItem = createTestScatteredItem(
            character.position, ItemTemplate.Registry.findById(WOODEN_ARROW.id)!!, 100)

        //Pick up item!
        withContext(context) {
            itemService.launchPickUp(character, scatteredItem).join()
        }

        //Assert pick up animation
        val pickUpResponse = assertIs<PickUpItemResponse>(context.responseChannel.receive())
        assertEquals(character.id, pickUpResponse.characterId, "Must get PickUpResponse of $character")
        assertEquals(scatteredItem, pickUpResponse.item)

        //Assert deleting scatteredItem notification
        val deleteObjectResponse = assertIs<DeleteObjectResponse>(context.responseChannel.receive())
        assertEquals(scatteredItem.id, deleteObjectResponse.gameObjectId)

        val updateItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.receive())
        assertEquals(UpdateItemOperationType.MODIFY, updateItemsResponse.operations.first().operationType)
        assertEquals(WOODEN_ARROW.id, updateItemsResponse.operations.first().item.templateId)

        assertIs<UpdateStatusResponse>(context.responseChannel.receive())

        assertIs<SystemMessageResponse.YouHaveObtained>(context.responseChannel.receive())
        assertFalse(gameObjectRepository.existsById(scatteredItem.id), "Picked up item must disappear")

        val arrows = Item.findAllByOwnerIdAndTemplateId(character.id, WOODEN_ARROW.id)
        assertEquals(1, arrows.size, "Should add new item to existing item stack")
        assertEquals(200, arrows.first().amount)
    }

}
