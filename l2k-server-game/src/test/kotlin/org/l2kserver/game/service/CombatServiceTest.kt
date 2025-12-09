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
import org.l2kserver.game.data.item.arrows.WoodenArrow
import org.l2kserver.game.data.item.soulshot.SoulshotNoGrade
import org.l2kserver.game.data.item.weapons.Bow
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
import org.l2kserver.game.handler.dto.request.UseItemRequest
import org.l2kserver.game.handler.dto.response.SkillUsedResponse
import org.l2kserver.game.handler.dto.response.StartMovingToTargetResponse
import org.l2kserver.game.handler.dto.response.item
import org.l2kserver.game.handler.dto.response.operation
import org.l2kserver.game.model.actor.character.PvpState
import org.l2kserver.game.model.item.Weapon
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CombatServiceTest(
    @param:Autowired private val combatService: CombatService,
    @param:Autowired private val itemService: ItemService
) : AbstractTests() {

    @Test
    fun shouldPerformAttackOnOtherCharacter(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val targetContext = createTestSessionContext()
        val targetCharacter = createTestCharacter(name = "PunchingBag")
        targetContext.setCharacterId(targetCharacter.id)

        //Launch attacking in parallel
        CoroutineScope(Dispatchers.Default).launch(context) {
            combatService.attack(character, targetCharacter)
        }

        // Check attacker's responses
        assertIs<StartMovingToTargetResponse>(context.responseChannel.receive())
        val attackResponse = assertIs<AttackResponse>(context.responseChannel.receive())
        assertEquals(character.id, attackResponse.attacker.id)
        assertEquals(1, attackResponse.attacks.size)

        val hit = attackResponse.attacks[0]
        assertEquals(targetCharacter.id, hit.targetId)

        var systemMessageResponse = assertIs<SystemMessageResponse>(context.responseChannel.receive())
        if (systemMessageResponse is SystemMessageResponse.CriticalHit)
            systemMessageResponse = assertIs<SystemMessageResponse>(context.responseChannel.receive())

        assertContains(
            listOf(SystemMessageResponse.YouMissed::class, SystemMessageResponse.YouHit::class),
            systemMessageResponse::class
        )

        val startFightingResponse = assertIs<StartFightingResponse>(context.responseChannel.receive())
        assertEquals(character.id, startFightingResponse.actorId)

        val targetStartFightingResponse = assertIs<StartFightingResponse>(context.responseChannel.receive())
        assertEquals(targetCharacter.id, targetStartFightingResponse.actorId)

        val pvpStatusResponse = assertIs<PvPStatusResponse>(context.responseChannel.receive())
        assertEquals(character.id, pvpStatusResponse.characterId)
        assertEquals(PvpState.PVP, pvpStatusResponse.pvpState)

        //Check target's responses
        val attackResponseForTarget = assertIs<AttackResponse>(targetContext.responseChannel.receive())
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
            val updateStatusResponse = assertIs<UpdateStatusResponse>(targetContext.responseChannel.receive())
            assertEquals(targetCharacter.id, updateStatusResponse.objectId)
            assertContains(updateStatusResponse.attributes.keys, StatusAttribute.CUR_CP)
        }

        val startFightingResponseForTarget = assertIs<StartFightingResponse>(targetContext.responseChannel.receive())
        assertEquals(character.id, startFightingResponseForTarget.actorId)

        val targetStartFightingResponseForTarget =
            assertIs<StartFightingResponse>(targetContext.responseChannel.receive())
        assertEquals(targetCharacter.id, targetStartFightingResponseForTarget.actorId)

        val attackerPvPStatusResponse = assertIs<PvPStatusResponse>(targetContext.responseChannel.receive())
        assertEquals(character.id, attackerPvPStatusResponse.characterId)
    }

    @Test
    fun shouldPerformAttackUsingSoulshot(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val soulshot = character.inventory.createItem(SoulshotNoGrade.id, 10)

        val targetContext = createTestSessionContext()
        val targetCharacter = createTestCharacter(name = "PunchingBag")
        targetContext.setCharacterId(targetCharacter.id)

        withContext(context) { itemService.useItem(UseItemRequest(soulshot.id)) }

        //Soulshot used response
        assertIs<SystemMessageResponse.SoulshotEnabled>(context.responseChannel.receive())
        val updateItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.receive())
        assertEquals(soulshot.id, updateItemsResponse.operations.first().item.id)
        assertEquals(9, updateItemsResponse.operations.first().item.amount)
        assertIs<SkillUsedResponse>(context.responseChannel.receive())

        //Launch attacking in parallel
        CoroutineScope(Dispatchers.Default).launch(context) {
            combatService.attack(character, targetCharacter)
        }

        // Check attacker's responses
        assertIs<StartMovingToTargetResponse>(context.responseChannel.receive())
        val attackResponse = assertIs<AttackResponse>(context.responseChannel.receive())
        assertEquals(character.id, attackResponse.attacker.id)
        assertEquals(1, attackResponse.attacks.size)
        assertTrue(attackResponse.usedSoulshot)

        val attack = attackResponse.attacks[0]
        assertEquals(targetCharacter.id, attack.targetId)

        var systemMessageResponse = assertIs<SystemMessageResponse>(context.responseChannel.receive())
        if (systemMessageResponse is SystemMessageResponse.CriticalHit)
            systemMessageResponse = assertIs<SystemMessageResponse>(context.responseChannel.receive())

        assertContains(
            listOf(SystemMessageResponse.YouMissed::class, SystemMessageResponse.YouHit::class),
            systemMessageResponse::class
        )

        val startFightingResponse = assertIs<StartFightingResponse>(context.responseChannel.receive())
        assertEquals(character.id, startFightingResponse.actorId)

        val targetStartFightingResponse = assertIs<StartFightingResponse>(context.responseChannel.receive())
        assertEquals(targetCharacter.id, targetStartFightingResponse.actorId)

        val pvpStatusResponse = assertIs<PvPStatusResponse>(context.responseChannel.receive())
        assertEquals(character.id, pvpStatusResponse.characterId)
        assertEquals(PvpState.PVP, pvpStatusResponse.pvpState)

        //Check target's responses
        assertIs<SkillUsedResponse>(targetContext.responseChannel.receive()) //target sees using soulshot too
        val attackResponseForTarget = assertIs<AttackResponse>(targetContext.responseChannel.receive())
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
            val updateStatusResponse = assertIs<UpdateStatusResponse>(targetContext.responseChannel.receive())
            assertEquals(targetCharacter.id, updateStatusResponse.objectId)
            assertContains(updateStatusResponse.attributes.keys, StatusAttribute.CUR_CP)
        }

        val startFightingResponseForTarget = assertIs<StartFightingResponse>(targetContext.responseChannel.receive())
        assertEquals(character.id, startFightingResponseForTarget.actorId)

        val targetStartFightingResponseForTarget =
            assertIs<StartFightingResponse>(targetContext.responseChannel.receive())
        assertEquals(targetCharacter.id, targetStartFightingResponseForTarget.actorId)

        val attackerPvPStatusResponse = assertIs<PvPStatusResponse>(targetContext.responseChannel.receive())
        assertEquals(character.id, attackerPvPStatusResponse.characterId)
    }

    @Test
    fun shouldConsumeManaAndArrowsOnShooting(): Unit = runBlocking {
        //Create attacker
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        transaction { ItemEntity.deleteAllByOwnerId(character.id) }
        val arrowsId = createTestItem(WoodenArrow.id, character).id
        val bow = createTestItem(Bow.id, character, isEquipped = true) as Weapon
        character.inventory.reload()

        //Create target
        val targetContext = createTestSessionContext()
        val targetCharacter = createTestCharacter(name = "PunchingBag")
        targetContext.setCharacterId(targetCharacter.id)

        //Launching attacking in parallel is not needed - process must stop after arrows run out
        withContext(context) {
            //Fail if attack process hasn't ended for some reason
            withTimeout(10_000L) {
                combatService.launchAttack(character, targetCharacter)
            }
        }

        // Check attacker's responses
        assertIs<StartMovingToTargetResponse>(context.responseChannel.receive())
        val updateItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.receive())
        assertEquals(UpdateItemOperation.REMOVE, updateItemsResponse.operations.first().operation)
        assertEquals(arrowsId, updateItemsResponse.operations.first().item.id)

        val updateStatusResponse = assertIs<UpdateStatusResponse>(context.responseChannel.receive())
        assertEquals(character.stats.maxMp - bow.manaCost, updateStatusResponse.attributes[StatusAttribute.CUR_MP])

        assertIs<SystemMessageResponse.YouCarefullyNockAnArrow>(context.responseChannel.receive())
        assertIs<GaugeResponse>(context.responseChannel.receive())

        val attackResponse = assertIs<AttackResponse>(context.responseChannel.receive())
        assertEquals(character.id, attackResponse.attacker.id)
        assertEquals(1, attackResponse.attacks.size)

        val hit = attackResponse.attacks[0]
        assertEquals(targetCharacter.id, hit.targetId)

        var systemMessageResponse = assertIs<SystemMessageResponse>(context.responseChannel.receive())
        if (systemMessageResponse is SystemMessageResponse.CriticalHit)
            systemMessageResponse = assertIs<SystemMessageResponse>(context.responseChannel.receive())

        assertContains(
            listOf(SystemMessageResponse.YouMissed::class, SystemMessageResponse.YouHit::class),
            systemMessageResponse::class
        )

        val startFightingResponse = assertIs<StartFightingResponse>(context.responseChannel.receive())
        assertEquals(character.id, startFightingResponse.actorId)

        val targetStartFightingResponse = assertIs<StartFightingResponse>(context.responseChannel.receive())
        assertEquals(targetCharacter.id, targetStartFightingResponse.actorId)

        val pvpStatusResponse = assertIs<PvPStatusResponse>(context.responseChannel.receive())
        assertEquals(character.id, pvpStatusResponse.characterId)
        assertEquals(PvpState.PVP, pvpStatusResponse.pvpState)

        //Check arrow amount after attack
        val arrows = transaction {
            ItemEntity.findAllByOwnerIdAndTemplateId(character.id, WoodenArrow.id).toList()
        }
        assertTrue(arrows.isEmpty(), "Arrows must be empty")

        assertIs<SystemMessageResponse.NotEnoughArrows>(
            context.responseChannel.receiveIgnoring(
                StartMovingToTargetResponse::class,
                UpdateStatusResponse::class
            )
        )
    }

}
