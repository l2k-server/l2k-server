package org.l2kserver.game.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.Test
import org.l2kserver.game.AbstractTests
import org.l2kserver.game.data.item.arrow.WoodenArrow
import org.l2kserver.game.data.item.soulshot.SoulshotNoGrade
import org.l2kserver.game.data.item.weapon.Bow
import org.l2kserver.game.extensions.receiveIgnoring
import org.l2kserver.game.handler.dto.response.AttackResponse
import org.l2kserver.game.handler.dto.response.GaugeResponse
import org.l2kserver.game.handler.dto.response.PvPStatusResponse
import org.l2kserver.game.handler.dto.response.StartFightingResponse
import org.l2kserver.game.handler.dto.response.StatusAttribute
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.handler.dto.response.UpdateItemOperation
import org.l2kserver.game.handler.dto.response.UpdateItemsResponse
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.domain.ItemEntity
import org.l2kserver.game.extensions.next
import org.l2kserver.game.handler.dto.request.UseItemRequest
import org.l2kserver.game.handler.dto.response.SkillUsedResponse
import org.l2kserver.game.handler.dto.response.item
import org.l2kserver.game.handler.dto.response.operation
import org.l2kserver.game.model.actor.character.PvpState
import org.l2kserver.game.model.item.WeaponInstanceImpl
import org.l2kserver.game.network.session.sessionContextOf
import org.springframework.beans.factory.annotation.Autowired
import kotlin.math.roundToInt
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CombatServiceTest @Autowired constructor(
    private val combatService: CombatService,
    private val itemService: ItemService
) : AbstractTests() {

    @Test
    fun shouldPerformAttackOnOtherCharacter(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        val targetCharacter = createTestCharacter(name = "PunchingBag")
        val targetContext = sessionContextOf(targetCharacter.id)!!
        character.targetId = targetCharacter.id

        //Launch attacking in parallel
        CoroutineScope(Dispatchers.Default).launch(context) {
            //Fail if attack process hasn't ended for some reason
            withTimeout(10_000L) { combatService.attack(character, targetCharacter) }
        }

        // Check attacker's responses
        val attackResponse = assertIs<AttackResponse>(context.responseChannel.next())
        assertEquals(character.id, attackResponse.attacker.id)
        assertEquals(1, attackResponse.attacks.size)

        val hit = attackResponse.attacks[0]
        assertEquals(targetCharacter.id, hit.targetId)

        var systemMessageResponse = assertIs<SystemMessageResponse>(context.responseChannel.next())
        if (systemMessageResponse is SystemMessageResponse.CriticalHit)
            systemMessageResponse = assertIs<SystemMessageResponse>(context.responseChannel.next())

        assertContains(
            listOf(SystemMessageResponse.YouMissed::class, SystemMessageResponse.YouHit::class),
            systemMessageResponse::class
        )

        val startFightingResponse = assertIs<StartFightingResponse>(context.responseChannel.next())
        assertEquals(character.id, startFightingResponse.actorId)

        val targetStartFightingResponse = assertIs<StartFightingResponse>(context.responseChannel.next())
        assertEquals(targetCharacter.id, targetStartFightingResponse.actorId)

        val pvpStatusResponse = assertIs<PvPStatusResponse>(context.responseChannel.next())
        assertEquals(character.id, pvpStatusResponse.characterId)
        assertEquals(PvpState.PVP, pvpStatusResponse.pvpState)

        //Check target's responses
        val attackResponseForTarget = assertIs<AttackResponse>(targetContext.responseChannel.next())
        assertEquals(character.id, attackResponseForTarget.attacker.id)
        assertEquals(1, attackResponse.attacks.size)
        assertEquals(hit, attackResponse.attacks[0])

        val systemMessageResponseForTarget = assertIs<SystemMessageResponse>(
            targetContext.responseChannel.receiveIgnoring(SystemMessageResponse.CriticalHit::class)
        )

        assertContains(
            listOf(SystemMessageResponse.YouWereHitBy::class, SystemMessageResponse.YouHaveAvoidedAttackOf::class),
            systemMessageResponseForTarget::class
        )

        if (systemMessageResponseForTarget is SystemMessageResponse.YouWereHitBy) {
            val updateStatusResponse = assertIs<UpdateStatusResponse>(targetContext.responseChannel.next())
            assertEquals(targetCharacter.id, updateStatusResponse.objectId)
            assertContains(updateStatusResponse.attributes.keys, StatusAttribute.CUR_CP)
        }

        val startFightingResponseForTarget = assertIs<StartFightingResponse>(targetContext.responseChannel.next())
        assertEquals(character.id, startFightingResponseForTarget.actorId)

        val targetStartFightingResponseForTarget =
            assertIs<StartFightingResponse>(targetContext.responseChannel.next())
        assertEquals(targetCharacter.id, targetStartFightingResponseForTarget.actorId)

        val attackerPvPStatusResponse = assertIs<PvPStatusResponse>(targetContext.responseChannel.next())
        assertEquals(character.id, attackerPvPStatusResponse.characterId)
    }

    @Test
    fun shouldPerformAttackUsingSoulshot(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        val soulshot = character.inventory.createItem(SoulshotNoGrade.id, 10)

        val targetCharacter = createTestCharacter(name = "PunchingBag")
        val targetContext = sessionContextOf(targetCharacter.id)!!

        withContext(context) { itemService.useItem(UseItemRequest(soulshot.id)) }

        //Soulshot used response
        assertIs<SystemMessageResponse.SoulshotEnabled>(context.responseChannel.next())
        val updateItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.next())
        assertEquals(soulshot.id, updateItemsResponse.operations.first().item.id)
        assertEquals(9, updateItemsResponse.operations.first().item.amount)
        assertIs<SkillUsedResponse>(context.responseChannel.next())

        character.targetId = targetCharacter.id

        //Launch attacking in parallel
        CoroutineScope(Dispatchers.Default).launch(context) {
            //Fail if attack process hasn't ended for some reason
            withTimeout(10_000L) {
                withTimeout(10_000L) { combatService.attack(character, targetCharacter) }
            }
        }

        // Check attacker's responses
        val attackResponse = assertIs<AttackResponse>(context.responseChannel.next())
        assertEquals(character.id, attackResponse.attacker.id)
        assertEquals(1, attackResponse.attacks.size)
        assertTrue(attackResponse.usedSoulshot)

        val attack = attackResponse.attacks[0]
        assertEquals(targetCharacter.id, attack.targetId)

        var systemMessageResponse = assertIs<SystemMessageResponse>(context.responseChannel.next())
        if (systemMessageResponse is SystemMessageResponse.CriticalHit)
            systemMessageResponse = assertIs<SystemMessageResponse>(context.responseChannel.next())

        assertContains(
            listOf(SystemMessageResponse.YouMissed::class, SystemMessageResponse.YouHit::class),
            systemMessageResponse::class
        )

        val startFightingResponse = assertIs<StartFightingResponse>(context.responseChannel.next())
        assertEquals(character.id, startFightingResponse.actorId)

        val targetStartFightingResponse = assertIs<StartFightingResponse>(context.responseChannel.next())
        assertEquals(targetCharacter.id, targetStartFightingResponse.actorId)

        val pvpStatusResponse = assertIs<PvPStatusResponse>(context.responseChannel.next())
        assertEquals(character.id, pvpStatusResponse.characterId)
        assertEquals(PvpState.PVP, pvpStatusResponse.pvpState)

        //Check target's responses
        assertIs<SkillUsedResponse>(targetContext.responseChannel.next()) //target sees using soulshot too
        val attackResponseForTarget = assertIs<AttackResponse>(targetContext.responseChannel.next())
        assertTrue(attackResponseForTarget.usedSoulshot)
        assertEquals(character.id, attackResponseForTarget.attacker.id)
        assertEquals(1, attackResponse.attacks.size)
        assertEquals(attack, attackResponse.attacks[0])

        val systemMessageResponseForTarget = assertIs<SystemMessageResponse>(
            targetContext.responseChannel.receiveIgnoring(SystemMessageResponse.CriticalHit::class)
        )

        assertContains(
            listOf(SystemMessageResponse.YouWereHitBy::class, SystemMessageResponse.YouHaveAvoidedAttackOf::class),
            systemMessageResponseForTarget::class
        )

        if (systemMessageResponseForTarget is SystemMessageResponse.YouWereHitBy) {
            val updateStatusResponse = assertIs<UpdateStatusResponse>(targetContext.responseChannel.next())
            assertEquals(targetCharacter.id, updateStatusResponse.objectId)
            assertContains(updateStatusResponse.attributes.keys, StatusAttribute.CUR_CP)
        }

        val startFightingResponseForTarget = assertIs<StartFightingResponse>(targetContext.responseChannel.next())
        assertEquals(character.id, startFightingResponseForTarget.actorId)

        val targetStartFightingResponseForTarget =
            assertIs<StartFightingResponse>(targetContext.responseChannel.next())
        assertEquals(targetCharacter.id, targetStartFightingResponseForTarget.actorId)

        val attackerPvPStatusResponse = assertIs<PvPStatusResponse>(targetContext.responseChannel.next())
        assertEquals(character.id, attackerPvPStatusResponse.characterId)
    }

    @Test
    fun shouldConsumeManaAndArrowsOnShooting(): Unit = runBlocking {
        //Create attacker
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        transaction { ItemEntity.deleteAllByOwnerId(character.id) }
        val arrowsId = createTestItem(WoodenArrow.id, character).id
        val bow = createTestItem(Bow.id, character, isEquipped = true) as WeaponInstanceImpl
        character.inventory.reload()

        //Create target
        val targetCharacter = createTestCharacter(name = "PunchingBag")
        character.targetId = targetCharacter.id
        //Launching attacking in parallel is not needed - process must stop after arrows run out
        withContext(context) {
            //Fail if attack process hasn't ended for some reason
            withTimeout(10_000L) { combatService.attack(character, targetCharacter) }
        }

        // Check attacker's responses
        val updateItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.next())
        assertEquals(UpdateItemOperation.REMOVE, updateItemsResponse.operations.first().operation)
        assertEquals(arrowsId, updateItemsResponse.operations.first().item.id)

        val updateStatusResponse = assertIs<UpdateStatusResponse>(context.responseChannel.next())
        assertEquals(
            character.stats.maxMp.roundToInt() - bow.manaCost,
            updateStatusResponse.attributes[StatusAttribute.CUR_MP]
        )

        assertIs<SystemMessageResponse.YouCarefullyNockAnArrow>(context.responseChannel.next())
        assertIs<GaugeResponse>(context.responseChannel.next())

        val attackResponse = assertIs<AttackResponse>(context.responseChannel.next())
        assertEquals(character.id, attackResponse.attacker.id)
        assertEquals(1, attackResponse.attacks.size)

        val hit = attackResponse.attacks[0]
        assertEquals(targetCharacter.id, hit.targetId)

        var systemMessageResponse = assertIs<SystemMessageResponse>(context.responseChannel.next())
        if (systemMessageResponse is SystemMessageResponse.CriticalHit)
            systemMessageResponse = assertIs<SystemMessageResponse>(context.responseChannel.next())

        assertContains(
            listOf(SystemMessageResponse.YouMissed::class, SystemMessageResponse.YouHit::class),
            systemMessageResponse::class
        )

        val startFightingResponse = assertIs<StartFightingResponse>(context.responseChannel.next())
        assertEquals(character.id, startFightingResponse.actorId)

        val targetStartFightingResponse = assertIs<StartFightingResponse>(context.responseChannel.next())
        assertEquals(targetCharacter.id, targetStartFightingResponse.actorId)

        val pvpStatusResponse = assertIs<PvPStatusResponse>(context.responseChannel.next())
        assertEquals(character.id, pvpStatusResponse.characterId)
        assertEquals(PvpState.PVP, pvpStatusResponse.pvpState)

        //Check arrow amount after attack
        val arrows = transaction {
            ItemEntity.findAllByOwnerIdAndTemplateId(character.id, WoodenArrow.id).toList()
        }
        assertTrue(arrows.isEmpty(), "Arrows must be empty")
    }

}

