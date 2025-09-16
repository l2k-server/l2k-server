package org.l2kserver.game.service

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.assertNull
import kotlin.test.Test
import org.junit.jupiter.api.assertThrows
import org.l2kserver.game.AbstractTests
import org.l2kserver.game.data.item.armor.LEATHER_SHIELD
import org.l2kserver.game.data.item.arrows.BONE_ARROW
import org.l2kserver.game.data.item.arrows.WOODEN_ARROW
import org.l2kserver.game.data.item.etc.ADENA
import org.l2kserver.game.data.item.weapons.DAGGER
import org.l2kserver.game.data.item.weapons.HEAVENS_DIVIDER
import org.l2kserver.game.data.item.weapons.SQUIRES_SWORD
import org.l2kserver.game.data.item.weapons.WILLOW_STAFF
import org.l2kserver.game.data.item.soulshot.SOULSHOT_NO_GRADE
import org.l2kserver.game.data.item.soulshot.SOULSHOT_S_GRADE
import org.l2kserver.game.handler.dto.request.DeleteItemRequest
import org.l2kserver.game.handler.dto.request.DropItemRequest
import org.l2kserver.game.handler.dto.request.TakeOffItemRequest
import org.l2kserver.game.handler.dto.request.UseItemRequest
import org.l2kserver.game.handler.dto.response.FullCharacterResponse
import org.l2kserver.game.handler.dto.response.DroppedItemResponse
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.handler.dto.response.UpdateItemOperation
import org.l2kserver.game.handler.dto.response.UpdateItemsResponse
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.extensions.toItemOnSale
import org.l2kserver.game.handler.dto.response.DeleteObjectResponse
import org.l2kserver.game.handler.dto.response.PickUpItemResponse
import org.l2kserver.game.handler.dto.response.StatusAttribute
import org.l2kserver.game.domain.ItemEntity
import org.l2kserver.game.domain.ItemTable
import org.l2kserver.game.handler.dto.response.item
import org.l2kserver.game.handler.dto.response.operation
import org.l2kserver.game.model.actor.Posture
import org.l2kserver.game.model.item.template.ItemTemplate
import org.l2kserver.game.model.item.template.Slot
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

        val itemId = createTestItem(2369, character).id

        withContext(context) { itemService.deleteItem(DeleteItemRequest(itemId, 1)) }

        assertFalse(transaction { ItemEntity.existsById(itemId) }, "Item must not exist")

        val updateResponse = assertIs<UpdateItemsResponse>(context.responseChannel.receive())
        val (item, operation) = updateResponse.operations[0]
        assertEquals(itemId, item.id)
        assertEquals(UpdateItemOperation.REMOVE, operation)

        val updateStatusResponse = assertIs<UpdateStatusResponse>(context.responseChannel.receive())
        assertEquals(StatusAttribute.CUR_LOAD, updateStatusResponse.attributes.keys.first())
    }

    @Test
    fun shouldSuccessfullyDeleteEquippedItem(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val itemId = createTestItem(SQUIRES_SWORD.id, owner = character, isEquipped = true).id

        withContext(context) { itemService.deleteItem(DeleteItemRequest(itemId, 1)) }

        newSuspendedTransaction {
            assertFalse(ItemEntity.existsById(itemId), "Deleted item should not exist")

            assertIs<FullCharacterResponse>(context.responseChannel.receive())

            val updateResponse = assertIs<UpdateItemsResponse>(context.responseChannel.receive())
            assertEquals(itemId, updateResponse.operations[0].item.id)
            assertEquals(UpdateItemOperation.MODIFY, updateResponse.operations[0].operation)
            assertEquals(itemId, updateResponse.operations[1].item.id)
            assertEquals(UpdateItemOperation.REMOVE, updateResponse.operations[1].operation)

            val updateStatusResponse = assertIs<UpdateStatusResponse>(context.responseChannel.receive())
            assertEquals(character.id, updateStatusResponse.objectId)
        }

    }

    @Test
    fun shouldSuccessfullyDeleteItemPartially(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val itemId = createTestItem(ADENA.id, owner = character, amount = 10).id

        withContext(context) { itemService.deleteItem(DeleteItemRequest(itemId, 1)) }

        newSuspendedTransaction {
            assertEquals(9, ItemEntity.findById(itemId)?.amount)

            val updatedItems = assertIs<UpdateItemsResponse>(context.responseChannel.receive())
            val (item, operation) = updatedItems.operations[0]
            assertEquals(itemId, item.id)
            assertEquals(UpdateItemOperation.MODIFY, operation)
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
            owner = character,
            amount = testAmount
        ).id

        withContext(context) { itemService.deleteItem(DeleteItemRequest(itemId, 1000)) }

        newSuspendedTransaction {
            assertEquals(testAmount, ItemEntity.findById(itemId)?.amount)
            assertIs<SystemMessageResponse.NotEnoughItems>(context.responseChannel.receive())
        }
    }

    @Test
    fun shouldSuccessfullyTakeOffEquippedItem(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val itemId = createTestItem(SQUIRES_SWORD.id, owner = character, isEquipped = true).id

        withContext(context) { itemService.useItem(UseItemRequest(itemId)) }

        newSuspendedTransaction {
            assertIs<SystemMessageResponse>(context.responseChannel.receive())
            val updateItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.receive())

            val (item, operation) = updateItemsResponse.operations[0]
            assertEquals(itemId, item.id)
            assertEquals(UpdateItemOperation.MODIFY, operation)


            assertIs<FullCharacterResponse>(context.responseChannel.receive())

            assertNull(ItemEntity.findById(itemId)!!.equippedAt)
        }
    }

    @Test
    fun shouldSuccessfullyTakeOffEquippedItemByDragNDrop(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        newSuspendedTransaction { ItemTable.deleteAll() }
        val itemId = createTestItem(SQUIRES_SWORD.id, owner = character, isEquipped = true).id

        character.inventory.reload()

        withContext(context) { itemService.takeOffItem(TakeOffItemRequest(Slot.RIGHT_HAND)) }

        newSuspendedTransaction {
            assertIs<SystemMessageResponse>(context.responseChannel.receive())
            val updateItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.receive())

            val (item, operation) = updateItemsResponse.operations[0]
            assertEquals(itemId, item.id)
            assertEquals(UpdateItemOperation.MODIFY, operation)


            assertIs<FullCharacterResponse>(context.responseChannel.receive())

            assertNull(ItemEntity.findById(itemId)!!.equippedAt)
        }
    }

    @Test
    fun shouldEquipWeaponIfAnotherWeaponIsEquipped(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val items = newSuspendedTransaction {
            ItemTable.deleteAll() // Delete initial items
            listOf(
                createTestItem(SQUIRES_SWORD.id, owner = character, isEquipped = true),
                createTestItem(DAGGER.id, owner = character)
            )
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
            assertEquals(UpdateItemOperation.MODIFY, operation1)

            val (usedItem, operation2) = updatedItemsResponse.operations[1]
            assertEquals(items[1].id, usedItem.id)
            assertEquals(UpdateItemOperation.MODIFY, operation2)

            assertIs<FullCharacterResponse>(context.responseChannel.receive())

            assertEquals(Slot.RIGHT_HAND, ItemEntity.findById(usedItem.id)!!.equippedAt)
        }
    }

    @Test
    fun shouldEquipTwoHandedWeaponIfAnotherWeaponAndShieldIsEquipped(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val items = newSuspendedTransaction {
            ItemTable.deleteAll() // Delete initial items
            listOf(
                createTestItem(SQUIRES_SWORD.id, owner = character, isEquipped = true),
                createTestItem(LEATHER_SHIELD.id, owner = character, isEquipped = true),
                createTestItem(WILLOW_STAFF.id, owner = character)
            )
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
            assertEquals(UpdateItemOperation.MODIFY, operation1)

            val (takenOffItem2, operation2) = updateItemsResponse.operations[1]
            assertEquals(items[1].id, takenOffItem2.id)
            assertFalse(takenOffItem2.isEquipped)
            assertEquals(UpdateItemOperation.MODIFY, operation2)

            val (usedItem, operation3) = updateItemsResponse.operations[2]
            assertEquals(items[2].id, usedItem.id)
            assertEquals(UpdateItemOperation.MODIFY, operation3)

            assertIs<FullCharacterResponse>(context.responseChannel.receive())

            assertEquals(Slot.TWO_HANDS, ItemEntity.findById(usedItem.id)!!.equippedAt)
        }
    }

    @Test
    fun shouldSuccessfullyDropItem(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val item = createTestItem(WILLOW_STAFF.id, character)

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
            assertFalse(ItemEntity.existsById(item.id), "Item must not exist")

            val updateItemResponse = assertIs<UpdateItemsResponse>(context.responseChannel.receive())
            val (updatedItem, operation) = updateItemResponse.operations[0]
            assertEquals(item.id, updatedItem.id)
            assertEquals(UpdateItemOperation.REMOVE, operation)

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

        val itemId = createTestItem(WILLOW_STAFF.id, character, isEquipped = true).id

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
            assertFalse(ItemEntity.existsById(itemId), "Deleted item should not exist")

            val droppedItemResponse = assertIs<DroppedItemResponse>(context.responseChannel.receive())
            assertEquals(character.id, droppedItemResponse.dropperId)
            assertEquals(1, droppedItemResponse.scatteredItem.amount)

            assertIs<FullCharacterResponse>(context.responseChannel.receive())

            val updateResponse = assertIs<UpdateItemsResponse>(context.responseChannel.receive())
            assertEquals(itemId, updateResponse.operations[0].item.id)
            assertEquals(UpdateItemOperation.MODIFY, updateResponse.operations[0].operation)
            assertEquals(itemId, updateResponse.operations[1].item.id)
            assertEquals(UpdateItemOperation.REMOVE, updateResponse.operations[1].operation)

            assertIs<UpdateStatusResponse>(context.responseChannel.receive())
        }

    }

    @Test
    fun shouldSuccessfullyDropItemPartially(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val itemId = createTestItem(57, character, 10).id

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

        gameObjectRepository.loadCharacter(character)

        newSuspendedTransaction {
            assertEquals(8, ItemEntity.findById(itemId)!!.amount)

            val droppedItemResponse = assertIs<DroppedItemResponse>(context.responseChannel.receive())
            assertEquals(character.id, droppedItemResponse.dropperId)
            assertEquals(2, droppedItemResponse.scatteredItem.amount)

            val updatedItems = assertIs<UpdateItemsResponse>(context.responseChannel.receive())
            val (item, operation) = updatedItems.operations[0]
            assertEquals(itemId, item.id)
            assertEquals(UpdateItemOperation.MODIFY, operation)

            assertIs<UpdateStatusResponse>(context.responseChannel.receive())
        }

    }

    @Test
    fun shouldFailIfTryingToDropMoreItemsThanHas(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val testAmount = 10
        val itemId = createTestItem(57, character, testAmount).id

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

        assertEquals(testAmount, newSuspendedTransaction { ItemEntity.findById(itemId)!!.amount })

        assertIs<SystemMessageResponse.NotEnoughItems>(context.responseChannel.receive())
    }

    @Test
    fun shouldGetErrorIfDroppingSomeoneElseItem(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()

        context.setCharacterId(character.id)

        val anotherCharacter = createTestCharacter(name = "${testCharacterName}2")

        val item = createTestItem(8, anotherCharacter)

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

        val item = createTestItem(10, character)

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

        val item = createTestItem(WILLOW_STAFF.id, character)

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
        val woodenArrow = createTestItem(WOODEN_ARROW.id, character, 100)
        character.inventory.reload()

        character.posture = Posture.SITTING
        character.privateStore = PrivateStore.Sell(
            title = "Wooden arrows - cheap and cheerful",
            items = listOf(woodenArrow.toItemOnSale(woodenArrow.amount, 2)),
            packageSale = true
        )
        val boneArrowId = createTestItem(BONE_ARROW.id, character, 100).id

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
        val woodenArrow = createTestItem(WOODEN_ARROW.id, character, 100)
        character.inventory.reload()

        character.posture = Posture.SITTING
        character.privateStore = PrivateStore.Sell(
            title = "Wooden arrows - cheap and cheerful",
            items = listOf(woodenArrow.toItemOnSale(woodenArrow.amount, 2)),
            packageSale = true
        )
        val boneArrowId = createTestItem(BONE_ARROW.id, character, 100).id

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
        val heavensDivider = createTestItem(HEAVENS_DIVIDER.id, character)
        character.inventory.reload()

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
        val existingItem = createTestItem(HEAVENS_DIVIDER.id, character)

        //Create scattered item
        val scatteredItem = createTestScatteredItem(
            character.position, ItemTemplate.Registry.findByIdOrNull(HEAVENS_DIVIDER.id)!!)

        //Pick up item!
        withContext(context) { itemService.launchPickUp(character, scatteredItem).join() }

        //Assert pick up animation
        val pickUpResponse = assertIs<PickUpItemResponse>(context.responseChannel.receive())
        assertEquals(character.id, pickUpResponse.characterId, "Must get PickUpResponse of $character")
        assertEquals(scatteredItem, pickUpResponse.item)

        //Assert deleting scatteredItem notification
        val deleteObjectResponse = assertIs<DeleteObjectResponse>(context.responseChannel.receive())
        assertEquals(scatteredItem.id, deleteObjectResponse.gameObjectId)

        val updateItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.receive())
        assertEquals(UpdateItemOperation.ADD, updateItemsResponse.operations.first().operation)
        assertEquals(HEAVENS_DIVIDER.id, updateItemsResponse.operations.first().first.templateId)
        assertNotEquals(existingItem.id, updateItemsResponse.operations.first().item.id)

        assertIs<UpdateStatusResponse>(context.responseChannel.receive())

        assertIs<SystemMessageResponse.YouHaveObtained>(context.responseChannel.receive())
        assertFalse(gameObjectRepository.existsById(scatteredItem.id), "Picked up item must disappear")
        transaction {
            assertEquals(2, ItemEntity.findAllByOwnerIdAndTemplateId(character.id, HEAVENS_DIVIDER.id).toList().size)
        }
    }

    @Test
    fun shouldPickUpStackableItem(): Unit = runBlocking {
        // Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Create already existing item
        createTestItem(WOODEN_ARROW.id, character, 100)

        //Create scattered item
        val scatteredItem = createTestScatteredItem(
            character.position, ItemTemplate.Registry.findByIdOrNull(WOODEN_ARROW.id)!!, 100)

        //Pick up item!
        withContext(context) {
            itemService.launchPickUp(character, scatteredItem)
        }

        //Assert pick up animation
        val pickUpResponse = assertIs<PickUpItemResponse>(context.responseChannel.receive())
        assertEquals(character.id, pickUpResponse.characterId, "Must get PickUpResponse of $character")
        assertEquals(scatteredItem, pickUpResponse.item)

        //Assert deleting scatteredItem notification
        val deleteObjectResponse = assertIs<DeleteObjectResponse>(context.responseChannel.receive())
        assertEquals(scatteredItem.id, deleteObjectResponse.gameObjectId)

        val updateItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.receive())
        assertEquals(UpdateItemOperation.MODIFY, updateItemsResponse.operations.first().operation)
        assertEquals(WOODEN_ARROW.id, updateItemsResponse.operations.first().first.templateId)

        assertIs<UpdateStatusResponse>(context.responseChannel.receive())

        assertIs<SystemMessageResponse.YouHaveObtained>(context.responseChannel.receive())
        assertFalse(gameObjectRepository.existsById(scatteredItem.id), "Picked up item must disappear")

        val arrows = transaction {
            ItemEntity.findAllByOwnerIdAndTemplateId(character.id, WOODEN_ARROW.id).toList()
        }
        assertEquals(1, arrows.size, "Should add new item to existing item stack")
        assertEquals(200, arrows.first().amount)
    }

    @Test
    fun shouldSuccessfullyUseSoulshot(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        character.inventory
        context.setCharacterId(character.id)

        //Create soulshot (weapon is created with character)
        val soulshotAmount = 10
        val soulshot = createTestItem(SOULSHOT_NO_GRADE.id, character, soulshotAmount)

        withContext(context) { itemService.useItem(UseItemRequest(soulshot.id)) }

        newSuspendedTransaction {
            //Check soulshot is consumed
            assertEquals(soulshotAmount - 1, ItemEntity.findById(soulshot.id)!!.amount)

            //Check weapon is charged
            val weapon = character.inventory.weapon!!
            assertTrue(weapon.soulshotCharged, "Weapon should be charged with soulshot")

            //Check responses
            assertIs<SystemMessageResponse.SoulshotEnabled>(context.responseChannel.receive())
            val updateItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.receive())
            assertEquals(soulshot.id, updateItemsResponse.operations[0].item.id)
            assertEquals(UpdateItemOperation.MODIFY, updateItemsResponse.operations[0].operation)
        }
    }

    @Test
    fun shouldFailUsingSoulshotWithoutWeapon(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Delete weapon
        character.inventory.delete(character.inventory.weapon!!)

        //Create soulshot
        val testSoulshotAmount = 10
        val soulshot = createTestItem(SOULSHOT_NO_GRADE.id, character, testSoulshotAmount)

        withContext(context) { itemService.useItem(UseItemRequest(soulshot.id)) }

        newSuspendedTransaction {
            //Check soulshot amount was not changed
            assertEquals(testSoulshotAmount, ItemEntity.findById(soulshot.id)!!.amount)
        }

        //Check response
        assertIs<SystemMessageResponse.CannotUseSoulshot>(context.responseChannel.receive())
    }

    @Test
    fun shouldFailUsingSoulshotWithWrongGrade(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val soulshot = createTestItem(SOULSHOT_S_GRADE.id, character, 10)

        withContext(context) { itemService.useItem(UseItemRequest(soulshot.id)) }

        newSuspendedTransaction {
            // Check soulshot was not used
            assertEquals(10, ItemEntity.findById(soulshot.id)!!.amount)
            
            // Check that weapon is not charged
            val weapon = character.inventory.weapon!!
            assertFalse(weapon.soulshotCharged, "Weapon should not be charged with wrong grade soulshot")
            
            // Check response
            assertIs<SystemMessageResponse.SoulshotGradeMismatch>(context.responseChannel.receive())
        }
    }


    @Test
    fun shouldNotUseSoulshotWhenWeaponAlreadyCharged(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        // Create weapon and soulshot
        val soulshot = createTestItem(SOULSHOT_NO_GRADE.id, character, 10)

        // Charge weapon
        character.inventory.weapon!!.soulshotCharged = true

        withContext(context) { itemService.useItem(UseItemRequest(soulshot.id)) }

        newSuspendedTransaction {
            // Check soulshot was not used
            assertEquals(10, ItemEntity.findById(soulshot.id)!!.amount)
            
            // Check weapon is still charged
            val weaponInstance = character.inventory.weapon!!
            assertTrue(weaponInstance.soulshotCharged, "Weapon should remain charged")
        }
    }

    @Test
    fun shouldConsumeAllSoulshotWhenUsingLastOne(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val soulshot = createTestItem(SOULSHOT_NO_GRADE.id, character, 1)

        withContext(context) { itemService.useItem(UseItemRequest(soulshot.id)) }

        newSuspendedTransaction {
            // Check soulshot was consumed
            assertFalse(ItemEntity.existsById(soulshot.id), "Soulshot should be completely consumed")

            // Check that weapon is charged
            val weaponInstance = character.inventory.weapon!!
            assertTrue(weaponInstance.soulshotCharged, "Weapon should be charged with soulshot")

            // Check responses
            assertIs<SystemMessageResponse.SoulshotEnabled>(context.responseChannel.receive())
            val updateItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.receive())
            assertEquals(soulshot.id, updateItemsResponse.operations[0].item.id)
            assertEquals(UpdateItemOperation.REMOVE, updateItemsResponse.operations[0].operation)
        }
    }

}
