package org.l2kserver.game.service

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.junit.jupiter.api.assertNull
import kotlin.test.Test
import org.junit.jupiter.api.assertThrows
import org.l2kserver.game.AbstractTests
import org.l2kserver.game.data.item.armor.LeatherShield
import org.l2kserver.game.data.item.arrow.BoneArrow
import org.l2kserver.game.data.item.arrow.WoodenArrow
import org.l2kserver.game.data.item.etc.Adena
import org.l2kserver.game.data.item.weapon.Dagger
import org.l2kserver.game.data.item.weapon.HeavensDivider
import org.l2kserver.game.data.item.weapon.SquiresSword
import org.l2kserver.game.data.item.weapon.WillowStaff
import org.l2kserver.game.data.item.soulshot.SoulshotNoGrade
import org.l2kserver.game.data.item.soulshot.SoulshotSGrade
import org.l2kserver.game.data.item.scroll.ScrollOfGuidance
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
import org.l2kserver.game.handler.dto.response.GaugeColor
import org.l2kserver.game.handler.dto.response.GaugeResponse
import org.l2kserver.game.handler.dto.response.SkillUsedResponse
import org.l2kserver.game.handler.dto.response.TemporalEffectsResponse
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.extensions.toItemOnSale
import org.l2kserver.game.handler.dto.response.DeleteObjectResponse
import org.l2kserver.game.handler.dto.response.PickUpItemResponse
import org.l2kserver.game.handler.dto.response.StatusAttribute
import org.l2kserver.game.domain.ItemEntity
import org.l2kserver.game.domain.ItemTable
import org.l2kserver.game.extensions.next
import org.l2kserver.game.handler.dto.response.StartMovingToTargetResponse
import org.l2kserver.game.handler.dto.response.item
import org.l2kserver.game.handler.dto.response.operation
import org.l2kserver.game.model.actor.Posture
import org.l2kserver.game.model.item.template.ItemTemplateRegistry
import org.l2kserver.game.model.item.template.Slot
import org.l2kserver.game.model.skill.effect.AbnormalType
import org.l2kserver.game.model.store.PrivateStore
import org.l2kserver.game.network.session.sessionContextOf
import org.springframework.beans.factory.annotation.Autowired
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ItemServiceTests(
    @param:Autowired private val itemService: ItemService
) : AbstractTests() {

    @Test
    fun shouldSuccessfullyDeleteItem(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        val itemId = createTestItem(2369, character).id

        withContext(context) { itemService.deleteItem(DeleteItemRequest(itemId, 1)) }

        assertFalse(suspendTransaction { ItemEntity.existsById(itemId) }, "Item must not exist")

        val updateResponse = assertIs<UpdateItemsResponse>(context.responseChannel.next())
        val (item, operation) = updateResponse.operations[0]
        assertEquals(itemId, item.id)
        assertEquals(UpdateItemOperation.REMOVE, operation)

        val updateStatusResponse = assertIs<UpdateStatusResponse>(context.responseChannel.next())
        assertEquals(StatusAttribute.CUR_LOAD, updateStatusResponse.attributes.keys.first())
    }

    @Test
    fun shouldSuccessfullyDeleteEquippedItem(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        val itemId = createTestItem(SquiresSword.id, owner = character, isEquipped = true).id

        withContext(context) { itemService.deleteItem(DeleteItemRequest(itemId, 1)) }

        suspendTransaction {
            assertFalse(ItemEntity.existsById(itemId), "Deleted item should not exist")

            assertIs<FullCharacterResponse>(context.responseChannel.next())

            val updateResponse = assertIs<UpdateItemsResponse>(context.responseChannel.next())
            assertEquals(itemId, updateResponse.operations[0].item.id)
            assertEquals(UpdateItemOperation.MODIFY, updateResponse.operations[0].operation)
            assertEquals(itemId, updateResponse.operations[1].item.id)
            assertEquals(UpdateItemOperation.REMOVE, updateResponse.operations[1].operation)

            val updateStatusResponse = assertIs<UpdateStatusResponse>(context.responseChannel.next())
            assertEquals(character.id, updateStatusResponse.objectId)
        }

    }

    @Test
    fun shouldSuccessfullyDeleteItemPartially(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        val itemId = createTestItem(Adena.id, owner = character, amount = 10).id

        withContext(context) { itemService.deleteItem(DeleteItemRequest(itemId, 1)) }

        suspendTransaction {
            assertEquals(9, ItemEntity.findById(itemId)?.amount)

            val updatedItems = assertIs<UpdateItemsResponse>(context.responseChannel.next())
            val (item, operation) = updatedItems.operations[0]
            assertEquals(itemId, item.id)
            assertEquals(UpdateItemOperation.MODIFY, operation)
        }
    }

    @Test
    fun shouldFailIfTryingToDeleteMoreItemsThanHas(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        val testAmount = 10
        val itemId = createTestItem(
            templateId = 57,
            owner = character,
            amount = testAmount
        ).id

        withContext(context) { itemService.deleteItem(DeleteItemRequest(itemId, 1000)) }

        suspendTransaction {
            assertEquals(testAmount, ItemEntity.findById(itemId)?.amount)
            assertIs<SystemMessageResponse.NotEnoughItems>(context.responseChannel.next())
        }
    }

    @Test
    fun shouldSuccessfullyTakeOffEquippedItem(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        val itemId = createTestItem(SquiresSword.id, owner = character, isEquipped = true).id

        withContext(context) { itemService.useItem(UseItemRequest(itemId)) }

        suspendTransaction {
            assertIs<SystemMessageResponse>(context.responseChannel.next())
            val updateItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.next())

            val (item, operation) = updateItemsResponse.operations[0]
            assertEquals(itemId, item.id)
            assertEquals(UpdateItemOperation.MODIFY, operation)


            assertIs<FullCharacterResponse>(context.responseChannel.next())

            assertNull(ItemEntity.findById(itemId)!!.equippedAt)
        }
    }

    @Test
    fun shouldSuccessfullyTakeOffEquippedItemByDragNDrop(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        suspendTransaction { ItemTable.deleteAll() }
        val itemId = createTestItem(SquiresSword.id, owner = character, isEquipped = true).id

        character.inventory.reload()

        withContext(context) { itemService.takeOffItem(TakeOffItemRequest(Slot.RIGHT_HAND)) }

        suspendTransaction {
            assertIs<SystemMessageResponse>(context.responseChannel.next())
            val updateItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.next())

            val (item, operation) = updateItemsResponse.operations[0]
            assertEquals(itemId, item.id)
            assertEquals(UpdateItemOperation.MODIFY, operation)


            assertIs<FullCharacterResponse>(context.responseChannel.next())

            assertNull(ItemEntity.findById(itemId)!!.equippedAt)
        }
    }

    @Test
    fun shouldEquipWeaponIfAnotherWeaponIsEquipped(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        val items = suspendTransaction {
            ItemTable.deleteAll() // Delete initial items
            listOf(
                createTestItem(SquiresSword.id, owner = character, isEquipped = true),
                createTestItem(Dagger.id, owner = character)
            )
        }

        withContext(context) { itemService.useItem(UseItemRequest(items[1].id)) }

        suspendTransaction {
            assertIs<SystemMessageResponse>(context.responseChannel.next())
            assertIs<SystemMessageResponse>(context.responseChannel.next())
            assertIs<FullCharacterResponse>(context.responseChannel.next())
            val updatedItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.next())

            val (takenOffItem, operation1) = updatedItemsResponse.operations[0]
            assertEquals(items[0].id, takenOffItem.id)
            assertFalse(takenOffItem.isEquipped)
            assertEquals(UpdateItemOperation.MODIFY, operation1)

            val (usedItem, operation2) = updatedItemsResponse.operations[1]
            assertEquals(items[1].id, usedItem.id)
            assertEquals(UpdateItemOperation.MODIFY, operation2)

            assertIs<FullCharacterResponse>(context.responseChannel.next())

            assertEquals(Slot.RIGHT_HAND, ItemEntity.findById(usedItem.id)!!.equippedAt)
        }
    }

    @Test
    fun shouldEquipTwoHandedWeaponIfAnotherWeaponAndShieldIsEquipped(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        val items = suspendTransaction {
            ItemTable.deleteAll() // Delete initial items
            listOf(
                createTestItem(SquiresSword.id, owner = character, isEquipped = true),
                createTestItem(LeatherShield.id, owner = character, isEquipped = true),
                createTestItem(WillowStaff.id, owner = character)
            )
        }

        withContext(context) { itemService.useItem(UseItemRequest(items[2].id)) }

        suspendTransaction {
            assertIs<SystemMessageResponse>(context.responseChannel.next())
            assertIs<SystemMessageResponse>(context.responseChannel.next())
            assertIs<SystemMessageResponse>(context.responseChannel.next())
            assertIs<FullCharacterResponse>(context.responseChannel.next())

            val updateItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.next())
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

            assertIs<FullCharacterResponse>(context.responseChannel.next())

            assertEquals(Slot.TWO_HANDS, ItemEntity.findById(usedItem.id)!!.equippedAt)
        }
    }

    @Test
    fun shouldSuccessfullyDropItem(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        val item = createTestItem(WillowStaff.id, character)

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

        suspendTransaction {
            val droppedItemResponse = assertIs<DroppedItemResponse>(context.responseChannel.next())
            assertEquals(character.id, droppedItemResponse.dropperId)
            assertFalse(ItemEntity.existsById(item.id), "Item must not exist")

            val updateItemResponse = assertIs<UpdateItemsResponse>(context.responseChannel.next())
            val (updatedItem, operation) = updateItemResponse.operations[0]
            assertEquals(item.id, updatedItem.id)
            assertEquals(UpdateItemOperation.REMOVE, operation)

            val updateStatusResponse = assertIs<UpdateStatusResponse>(context.responseChannel.next())
            assertEquals(character.id, updateStatusResponse.objectId)

            assertNotNull(gameObjectRepository.findByIdOrNull(droppedItemResponse.scatteredItem.id))
        }
    }

    @Test
    fun shouldSuccessfullyDropEquippedItem(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        val itemId = createTestItem(WillowStaff.id, character, isEquipped = true).id

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


        suspendTransaction {
            assertFalse(ItemEntity.existsById(itemId), "Deleted item should not exist")

            val droppedItemResponse = assertIs<DroppedItemResponse>(context.responseChannel.next())
            assertEquals(character.id, droppedItemResponse.dropperId)
            assertEquals(1, droppedItemResponse.scatteredItem.amount)

            assertIs<FullCharacterResponse>(context.responseChannel.next())

            val updateResponse = assertIs<UpdateItemsResponse>(context.responseChannel.next())
            assertEquals(itemId, updateResponse.operations[0].item.id)
            assertEquals(UpdateItemOperation.MODIFY, updateResponse.operations[0].operation)
            assertEquals(itemId, updateResponse.operations[1].item.id)
            assertEquals(UpdateItemOperation.REMOVE, updateResponse.operations[1].operation)

            assertIs<UpdateStatusResponse>(context.responseChannel.next())
        }

    }

    @Test
    fun shouldSuccessfullyDropItemPartially(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

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

        gameObjectRepository.save(character)

        suspendTransaction {
            assertEquals(8, ItemEntity.findById(itemId)!!.amount)

            val droppedItemResponse = assertIs<DroppedItemResponse>(context.responseChannel.next())
            assertEquals(character.id, droppedItemResponse.dropperId)
            assertEquals(2, droppedItemResponse.scatteredItem.amount)

            val updatedItems = assertIs<UpdateItemsResponse>(context.responseChannel.next())
            val (item, operation) = updatedItems.operations[0]
            assertEquals(itemId, item.id)
            assertEquals(UpdateItemOperation.MODIFY, operation)

            assertIs<UpdateStatusResponse>(context.responseChannel.next())
        }

    }

    @Test
    fun shouldFailIfTryingToDropMoreItemsThanHas(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

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

        assertEquals(testAmount, suspendTransaction { ItemEntity.findById(itemId)!!.amount })

        assertIs<SystemMessageResponse.NotEnoughItems>(context.responseChannel.next())
    }

    @Test
    fun shouldGetErrorIfDroppingSomeoneElseItem(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

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
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

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
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

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

        assertIs<SystemMessageResponse.CannotDiscardItem>(context.responseChannel.next())
    }

    @Test
    fun shouldGetErrorWhenDroppingItemTooFar(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        val item = createTestItem(WillowStaff.id, character)

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

        assertIs<SystemMessageResponse.TooFarToDiscard>(context.responseChannel.next())
    }

    @Test
    fun shouldFailDeletingItemWhileInPrivateStore(): Unit = runBlocking {
        //Create our character
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        //Create store
        val woodenArrow = createTestItem(WoodenArrow.id, character, 100)
        character.inventory.reload()

        character.posture = Posture.SITTING
        character.privateStore = PrivateStore.Sell(
            title = "Wooden arrows - cheap and cheerful",
            items = listOf(woodenArrow.toItemOnSale(woodenArrow.amount, 2)),
            packageSale = true
        )
        val boneArrowId = createTestItem(BoneArrow.id, character, 100).id

        //Then
        withContext(context) { itemService.deleteItem(DeleteItemRequest(boneArrowId, 1)) }

        // Check responses
        assertIs<SystemMessageResponse.CannotDiscardDestroyOrTradeWhileInShop>(context.responseChannel.next())
    }

    @Test
    fun shouldFailDroppingItemWhileInPrivateStore(): Unit = runBlocking {
        // Create our character
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        // Create store
        val woodenArrow = createTestItem(WoodenArrow.id, character, 100)
        character.inventory.reload()

        character.posture = Posture.SITTING
        character.privateStore = PrivateStore.Sell(
            title = "Wooden arrows - cheap and cheerful",
            items = listOf(woodenArrow.toItemOnSale(woodenArrow.amount, 2)),
            packageSale = true
        )
        val boneArrowId = createTestItem(BoneArrow.id, character, 100).id

        // Then
        withContext(context) { itemService.dropItem(DropItemRequest(boneArrowId, 1, character.position)) }

        // Check responses
        assertIs<SystemMessageResponse.CannotDiscardDestroyOrTradeWhileInShop>(context.responseChannel.next())
    }

    @Test
    fun shouldFailUsingItemWhichIsUsedInPrivateStore(): Unit = runBlocking {
        // Create our character
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        // Create store
        val heavensDivider = createTestItem(HeavensDivider.id, character)
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
        val response = assertIs<SystemMessageResponse.ItemCannotBeUsed>(context.responseChannel.next())
        assertEquals(heavensDivider.id, response.item.id, "Used and failed to use item ids must be equal")
    }

    @Test
    fun shouldPickUpItem(): Unit = runBlocking {
        // Create our character
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        //Create already existing item
        val existingItem = createTestItem(HeavensDivider.id, character)

        //Create scattered item
        val scatteredItem = createTestScatteredItem(
            character.position, ItemTemplateRegistry.findByIdOrNull(HeavensDivider.id)!!)

        //Pick up item!
        runBlocking(context) { itemService.pickUp(character, scatteredItem) }

        //Assert pick up animation
        val pickUpResponse = assertIs<PickUpItemResponse>(context.responseChannel.next())
        assertEquals(character.id, pickUpResponse.characterId, "Must get PickUpResponse of $character")
        assertEquals(scatteredItem, pickUpResponse.item)

        //Assert deleting scatteredItem notification
        val deleteObjectResponse = assertIs<DeleteObjectResponse>(context.responseChannel.next())
        assertEquals(scatteredItem.id, deleteObjectResponse.gameObjectId)

        val updateItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.next())
        assertEquals(UpdateItemOperation.ADD, updateItemsResponse.operations.first().operation)
        assertEquals(HeavensDivider.id, updateItemsResponse.operations.first().first.templateId)
        assertNotEquals(existingItem.id, updateItemsResponse.operations.first().item.id)

        assertIs<UpdateStatusResponse>(context.responseChannel.next())

        assertIs<SystemMessageResponse.YouHaveObtained>(context.responseChannel.next())
        assertFalse(gameObjectRepository.existsById(scatteredItem.id), "Picked up item must disappear")
        suspendTransaction {
            assertEquals(2, ItemEntity.findAllByOwnerIdAndTemplateId(character.id, HeavensDivider.id).toList().size)
        }
    }

    @Test
    fun shouldPickUpStackableItem(): Unit = runBlocking {
        // Create our character
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        //Create already existing item
        createTestItem(WoodenArrow.id, character, 100)

        //Create scattered item
        val scatteredItem = createTestScatteredItem(
            character.position, ItemTemplateRegistry.findByIdOrNull(WoodenArrow.id)!!, 100)

        //Pick up item!
        withContext(context) {
            itemService.pickUp(character, scatteredItem)
        }

        //Assert pick up animation
        val pickUpResponse = assertIs<PickUpItemResponse>(context.responseChannel.next())
        assertEquals(character.id, pickUpResponse.characterId, "Must get PickUpResponse of $character")
        assertEquals(scatteredItem, pickUpResponse.item)

        //Assert deleting scatteredItem notification
        val deleteObjectResponse = assertIs<DeleteObjectResponse>(context.responseChannel.next())
        assertEquals(scatteredItem.id, deleteObjectResponse.gameObjectId)

        val updateItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.next())
        assertEquals(UpdateItemOperation.MODIFY, updateItemsResponse.operations.first().operation)
        assertEquals(WoodenArrow.id, updateItemsResponse.operations.first().first.templateId)

        assertIs<UpdateStatusResponse>(context.responseChannel.next())

        assertIs<SystemMessageResponse.YouHaveObtained>(context.responseChannel.next())
        assertFalse(gameObjectRepository.existsById(scatteredItem.id), "Picked up item must disappear")

        val arrows = suspendTransaction {
            ItemEntity.findAllByOwnerIdAndTemplateId(character.id, WoodenArrow.id).toList()
        }
        assertEquals(1, arrows.size, "Should add new item to existing item stack")
        assertEquals(200, arrows.first().amount)
    }

    @Test
    fun shouldSuccessfullyUseSoulshot(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        //Create soulshot (weapon is created with character)
        val soulshotAmount = 10
        val soulshot = createTestItem(SoulshotNoGrade.id, character, soulshotAmount)

        withContext(context) { itemService.useItem(UseItemRequest(soulshot.id)) }

        suspendTransaction {
            //Check soulshot is consumed
            assertEquals(soulshotAmount - 1, ItemEntity.findById(soulshot.id)!!.amount)

            //Check weapon is charged
            val weapon = character.inventory.weapon!!
            assertTrue(weapon.soulshotCharged, "Weapon should be charged with soulshot")

            //Check responses
            assertIs<SystemMessageResponse.SoulshotEnabled>(context.responseChannel.next())
            val updateItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.next())
            assertEquals(soulshot.id, updateItemsResponse.operations[0].item.id)
            assertEquals(UpdateItemOperation.MODIFY, updateItemsResponse.operations[0].operation)
        }
    }

    @Test
    fun shouldFailUsingSoulshotWithoutWeapon(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        //Delete weapon
        character.inventory.delete(character.inventory.weapon!!)

        //Create soulshot
        val testSoulshotAmount = 10
        val soulshot = createTestItem(SoulshotNoGrade.id, character, testSoulshotAmount)

        withContext(context) { itemService.useItem(UseItemRequest(soulshot.id)) }

        suspendTransaction {
            //Check soulshot amount was not changed
            assertEquals(testSoulshotAmount, ItemEntity.findById(soulshot.id)!!.amount)
        }

        //Check response
        assertIs<SystemMessageResponse.CannotUseSoulshot>(context.responseChannel.next())
    }

    @Test
    fun shouldFailUsingSoulshotWithWrongGrade(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        val soulshot = createTestItem(SoulshotSGrade.id, character, 10)

        withContext(context) { itemService.useItem(UseItemRequest(soulshot.id)) }

        suspendTransaction {
            // Check soulshot was not used
            assertEquals(10, ItemEntity.findById(soulshot.id)!!.amount)
            
            // Check that weapon is not charged
            val weapon = character.inventory.weapon!!
            assertFalse(weapon.soulshotCharged, "Weapon should not be charged with wrong grade soulshot")
            
            // Check response
            assertIs<SystemMessageResponse.SoulshotGradeMismatch>(context.responseChannel.next())
        }
    }


    @Test
    fun shouldNotUseSoulshotWhenWeaponAlreadyCharged(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        // Create weapon and soulshot
        val soulshot = createTestItem(SoulshotNoGrade.id, character, 10)

        // Charge weapon
        character.inventory.weapon!!.soulshotCharged = true

        withContext(context) { itemService.useItem(UseItemRequest(soulshot.id)) }

        suspendTransaction {
            // Check soulshot was not used
            assertEquals(10, ItemEntity.findById(soulshot.id)!!.amount)
            
            // Check weapon is still charged
            val weaponInstance = character.inventory.weapon!!
            assertTrue(weaponInstance.soulshotCharged, "Weapon should remain charged")
        }
    }

    @Test
    fun shouldConsumeAllSoulshotWhenUsingLastOne(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        val soulshot = createTestItem(SoulshotNoGrade.id, character, 1)

        withContext(context) { itemService.useItem(UseItemRequest(soulshot.id)) }

        suspendTransaction {
            // Check soulshot was consumed
            assertFalse(ItemEntity.existsById(soulshot.id), "Soulshot should be completely consumed")

            // Check that weapon is charged
            val weaponInstance = character.inventory.weapon!!
            assertTrue(weaponInstance.soulshotCharged, "Weapon should be charged with soulshot")

            // Check responses
            assertIs<SystemMessageResponse.SoulshotEnabled>(context.responseChannel.next())
            val updateItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.next())
            assertEquals(soulshot.id, updateItemsResponse.operations[0].item.id)
            assertEquals(UpdateItemOperation.REMOVE, updateItemsResponse.operations[0].operation)
        }
    }

    @Test
    fun shouldSuccessfullyUseMagicItem(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        // Create ScrollOfGuidance
        val scroll = createTestItem(ScrollOfGuidance.id, character, 1)

        // Use the scroll
        withContext(context) { itemService.useItem(UseItemRequest(scroll.id)) }

        // Check responses - item consumed at start of cast
        val updateItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.next())
        assertEquals(scroll.id, updateItemsResponse.operations[0].item.id)
        assertEquals(UpdateItemOperation.REMOVE, updateItemsResponse.operations[0].operation)

        // Check scroll is consumed (consumesToStart is consumed at the start of casting)
        val scrollAfterUsage = character.inventory.findAllByTemplateId(scroll.templateId).firstOrNull()
        assertNull(scrollAfterUsage, "Scroll should be consumed")

        // Check skill casting responses
        assertIs<SystemMessageResponse.YouUse>(context.responseChannel.next())
        val gaugeResponse = assertIs<GaugeResponse>(context.responseChannel.next())
        assertEquals(GaugeColor.BLUE, gaugeResponse.gaugeColor)

        val skillUsedResponse = assertIs<SkillUsedResponse>(context.responseChannel.next())
        assertEquals(character.id, skillUsedResponse.casterId)
        assertEquals(character.id, skillUsedResponse.targetId)
        assertEquals(ScrollOfGuidance.skill.id, skillUsedResponse.skillId)

        assertIs<FullCharacterResponse>(context.responseChannel.next())
        val temporalEffectsResponse = assertIs<TemporalEffectsResponse>(context.responseChannel.next())
        assertEquals(1, temporalEffectsResponse.abnormals.size)

        // Check that character has the effect applied
        assertEquals(1, character.temporalEffects.size)
        val effect = character.temporalEffects.firstOrNull()
        assertNotNull(effect, "Character should have temporal effect")
        assertEquals(AbnormalType.HIT_UP, effect.abnormalType)
        val bonusStats = effect.getFixedBonusStats(character)
        assertNotNull(bonusStats, "Effect should provide fixed bonus stats")
        assertEquals(4, bonusStats.accuracy, "Effect should give +4 accuracy")
    }

}
