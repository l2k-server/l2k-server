package org.l2kserver.game.service

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import kotlin.test.Test
import org.l2kserver.game.AbstractTests
import org.l2kserver.game.data.item.scroll.enchant.ScrollEnchantWeaponS
import org.l2kserver.game.data.item.weapon.HeavensDivider
import org.l2kserver.game.data.item.weapon.SquiresSword
import org.l2kserver.game.handler.dto.request.EnchantRequest
import org.l2kserver.game.handler.dto.request.UseItemRequest
import org.l2kserver.game.handler.dto.response.ChooseItemToEnchantResponse
import org.l2kserver.game.handler.dto.response.CloseChooseItemToEnchantResponse
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.handler.dto.response.UpdateItemsResponse
import org.l2kserver.game.domain.ItemEntity
import org.l2kserver.game.domain.ItemTable
import org.l2kserver.game.extensions.pullResponse
import org.l2kserver.game.handler.dto.response.StatusAttribute
import org.l2kserver.game.handler.dto.response.UpdateItemOperation
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.handler.dto.response.item
import org.l2kserver.game.handler.dto.response.operation
import org.l2kserver.game.network.session.sessionContextOf
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ItemServiceEnchantmentTests(
    @param:Autowired private val itemService: ItemService
) : AbstractTests() {

    @Test
    fun shouldSuccessfullyEnchantWeapon(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        suspendTransaction { ItemTable.deleteAll() }
        
        val weaponId = createTestItem(HeavensDivider.id, owner = character).id
        val enchantScrollId = createTestItem(ScrollEnchantWeaponS.id, character).id

        withContext(context) {
            itemService.useItem(UseItemRequest(enchantScrollId))
        }

        assertIs<SystemMessageResponse.SelectItemToEnchant>(context.pullResponse())
        val chooseItemResponse = assertIs<ChooseItemToEnchantResponse>(context.pullResponse())
        assertEquals(ScrollEnchantWeaponS.id, chooseItemResponse.enchantScrollId)

        withContext(context) {
            itemService.enchantItem(EnchantRequest(weaponId))
        }

        // Check responses
        val updateScrollResponse = assertIs<UpdateItemsResponse>(context.pullResponse())
        assertEquals(enchantScrollId, updateScrollResponse.operations[0].item.id)
        assertEquals(UpdateItemOperation.REMOVE, updateScrollResponse.operations[0].operation)

        val updateWeightResponse = assertIs<UpdateStatusResponse>(context.pullResponse())
        assertEquals(character.inventory.weight, updateWeightResponse.attributes[StatusAttribute.CUR_LOAD])

        val updateWeaponResponse = assertIs<UpdateItemsResponse>(context.pullResponse())
        assertEquals(weaponId, updateWeaponResponse.operations[0].item.id)
        assertEquals(UpdateItemOperation.MODIFY, updateWeaponResponse.operations[0].operation)

        assertIs<SystemMessageResponse.YourItemHasBeenSuccessfullyEnchanted>(context.pullResponse())

        suspendTransaction {
            val weapon = character.inventory.findById(weaponId)
            assertEquals(1, weapon.enchantLevel)

            // Check scroll was consumed
            assertNull(character.inventory.findByIdOrNull(enchantScrollId))
        }
    }

    @Test
    fun shouldFailEnchantingWeaponWithWrongScroll(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        suspendTransaction { ItemTable.deleteAll() }

        val weapon = createTestItem(SquiresSword.id, character)
        val enchantScrollId = createTestItem(ScrollEnchantWeaponS.id, character).id

        withContext(context) {
            itemService.useItem(UseItemRequest(enchantScrollId))
        }

        assertIs<SystemMessageResponse.SelectItemToEnchant>(context.pullResponse())
        val chooseItemResponse = assertIs<ChooseItemToEnchantResponse>(context.pullResponse())
        assertEquals(ScrollEnchantWeaponS.id, chooseItemResponse.enchantScrollId)

        withContext(context) {
            itemService.enchantItem(EnchantRequest(weapon.id))
        }

        // Check responses
        assertIs<SystemMessageResponse.InappropriateEnchantConditions>(context.pullResponse())

        suspendTransaction {
            assertEquals(0, weapon.enchantLevel)

            // Check scroll was NOT consumed
            assertNotNull(character.inventory.findById(enchantScrollId))
        }
    }

    @Test
    fun shouldCancelEnchantSession(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        val enchantScrollId = createTestItem(ScrollEnchantWeaponS.id, character).id

        withContext(context) { itemService.useItem(UseItemRequest(enchantScrollId)) }

        assertIs<SystemMessageResponse.SelectItemToEnchant>(context.pullResponse())
        val chooseItemResponse = assertIs<ChooseItemToEnchantResponse>(context.pullResponse())
        assertEquals(ScrollEnchantWeaponS.id, chooseItemResponse.enchantScrollId)

        withContext(context) { itemService.enchantItem(EnchantRequest(-1)) }

        assertIs<SystemMessageResponse.EnchantmentCancelled>(context.pullResponse())
        assertIs<CloseChooseItemToEnchantResponse>(context.pullResponse())
    }

    @Test
    fun shouldFailEnchantingWithoutActiveScroll(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        suspendTransaction { ItemTable.deleteAll() }
        
        val weaponId = createTestItem(SquiresSword.id, owner = character).id

        withContext(context) { itemService.enchantItem(EnchantRequest(weaponId)) }

        // Check responses
        assertIs<SystemMessageResponse.InappropriateEnchantConditions>(context.pullResponse())

        suspendTransaction {
            val weapon = ItemEntity.findById(weaponId)!!
            assertEquals(0, weapon.enchantLevel)
        }
    }

    @Test
    fun shouldFailEnchantingNonExistentItem(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!
        
        val enchantScrollId = createTestItem(ScrollEnchantWeaponS.id, character).id

        withContext(context) { itemService.useItem(UseItemRequest(enchantScrollId)) }

        assertIs<SystemMessageResponse.SelectItemToEnchant>(context.pullResponse())
        val chooseItemResponse = assertIs<ChooseItemToEnchantResponse>(context.pullResponse())
        assertEquals(ScrollEnchantWeaponS.id, chooseItemResponse.enchantScrollId)

        withContext(context) { itemService.enchantItem(EnchantRequest(1)) }

        suspendTransaction {
            // Check scroll was NOT consumed
            assertNotNull(character.inventory.findById(enchantScrollId))
        }
    }
}
