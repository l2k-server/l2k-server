package org.l2kserver.game.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.test.Test
import org.l2kserver.game.AbstractTests
import org.l2kserver.game.data.item.arrows.WOODEN_ARROW
import org.l2kserver.game.data.item.weapons.BOW
import org.l2kserver.game.extensions.model.item.delete
import org.l2kserver.game.extensions.model.item.findAllByOwnerIdAndTemplateId
import org.l2kserver.game.extensions.model.item.findAllEquippedByOwnerId
import org.l2kserver.game.extensions.receiveIgnoring
import org.l2kserver.game.handler.dto.response.AttackResponse
import org.l2kserver.game.handler.dto.response.GaugeResponse
import org.l2kserver.game.handler.dto.response.PvPStatusResponse
import org.l2kserver.game.handler.dto.response.StartFightingResponse
import org.l2kserver.game.handler.dto.response.StatusAttribute
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.handler.dto.response.UpdateItemOperationType
import org.l2kserver.game.handler.dto.response.UpdateItemsResponse
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.model.PaperDoll
import org.l2kserver.game.model.actor.character.PvpState
import org.l2kserver.game.model.item.Item
import org.l2kserver.game.model.item.Weapon
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CombatServiceTest(
    @Autowired private val combatService: CombatService
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
            combatService.launchAttack(character, targetCharacter)
        }

        // Check attacker's responses
        val startFightingResponse = assertIs<StartFightingResponse>(context.responseChannel.receive())
        assertEquals(character.id, startFightingResponse.actorId)

        val pvpStatusResponse = assertIs<PvPStatusResponse>(context.responseChannel.receive())
        assertEquals(character.id, pvpStatusResponse.characterId)
        assertEquals(PvpState.PVP, pvpStatusResponse.pvpState)

        val attackResponse = assertIs<AttackResponse>(context.responseChannel.receive())
        assertEquals(character.id, attackResponse.attackerId)
        assertEquals(1, attackResponse.hits.size)

        val hit = attackResponse.hits[0]
        assertEquals(targetCharacter.id, hit.targetId)

        val targetStartFightingResponse = assertIs<StartFightingResponse>(context.responseChannel.receive())
        assertEquals(targetCharacter.id, targetStartFightingResponse.actorId)

        var systemMessageResponse = assertIs<SystemMessageResponse>(context.responseChannel.receive())
        if (systemMessageResponse is SystemMessageResponse.CriticalHit)
            systemMessageResponse = assertIs<SystemMessageResponse>(context.responseChannel.receive())

        assertContains(
            listOf(SystemMessageResponse.YouMissed::class, SystemMessageResponse.YouHit::class),
            systemMessageResponse::class
        )

        //Check target's responses
        val startFightingResponseForTarget = assertIs<StartFightingResponse>(targetContext.responseChannel.receive())
        assertEquals(character.id, startFightingResponseForTarget.actorId)

        val attackerPvPStatusResponse = assertIs<PvPStatusResponse>(targetContext.responseChannel.receive())
        assertEquals(character.id, attackerPvPStatusResponse.characterId)

        val attackResponseForTarget = assertIs<AttackResponse>(targetContext.responseChannel.receive())
        assertEquals(character.id, attackResponseForTarget.attackerId)
        assertEquals(1, attackResponse.hits.size)
        assertEquals(hit, attackResponse.hits[0])

        val targetStartFightingResponseForTarget =
            assertIs<StartFightingResponse>(targetContext.responseChannel.receive())
        assertEquals(targetCharacter.id, targetStartFightingResponseForTarget.actorId)

        var systemMessageResponseForTarget = assertIs<SystemMessageResponse>(
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
    }

    @Test
    fun shouldConsumeManaAndArrowsOnShooting(): Unit = runBlocking {
        //Create attacker
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        newSuspendedTransaction { Item.findAllEquippedByOwnerId(character.id).forEach { it.delete() } }
        val arrowsId = createTestItem(WOODEN_ARROW.id, character.id).id
        val bow = createTestItem(BOW.id, character.id, isEquipped = true) as Weapon
        newSuspendedTransaction { character.paperDoll = PaperDoll(Item.findAllEquippedByOwnerId(character.id)) }

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
        val startFightingResponse = assertIs<StartFightingResponse>(context.responseChannel.receive())
        assertEquals(character.id, startFightingResponse.actorId)

        val pvpStatusResponse = assertIs<PvPStatusResponse>(context.responseChannel.receive())
        assertEquals(character.id, pvpStatusResponse.characterId)
        assertEquals(PvpState.PVP, pvpStatusResponse.pvpState)

        val updateItemsResponse = assertIs<UpdateItemsResponse>(context.responseChannel.receive())
        assertEquals(UpdateItemOperationType.REMOVE, updateItemsResponse.operations.first().operationType)
        assertEquals(arrowsId, updateItemsResponse.operations.first().item.id)

        val updateStatusResponse = assertIs<UpdateStatusResponse>(context.responseChannel.receive())
        assertEquals(character.stats.maxMp - bow.manaCost, updateStatusResponse.attributes[StatusAttribute.CUR_MP])

        assertIs<SystemMessageResponse.YouCarefullyNockAnArrow>(context.responseChannel.receive())
        assertIs<GaugeResponse>(context.responseChannel.receive())

        val attackResponse = assertIs<AttackResponse>(context.responseChannel.receive())
        assertEquals(character.id, attackResponse.attackerId)
        assertEquals(1, attackResponse.hits.size)

        val hit = attackResponse.hits[0]
        assertEquals(targetCharacter.id, hit.targetId)

        val targetStartFightingResponse = assertIs<StartFightingResponse>(context.responseChannel.receive())
        assertEquals(targetCharacter.id, targetStartFightingResponse.actorId)

        var systemMessageResponse = assertIs<SystemMessageResponse>(context.responseChannel.receive())
        if (systemMessageResponse is SystemMessageResponse.CriticalHit)
            systemMessageResponse = assertIs<SystemMessageResponse>(context.responseChannel.receive())

        assertContains(
            listOf(SystemMessageResponse.YouMissed::class, SystemMessageResponse.YouHit::class),
            systemMessageResponse::class
        )

        //Check arrow amount after attack
        val arrows = newSuspendedTransaction { Item.findAllByOwnerIdAndTemplateId(character.id, WOODEN_ARROW.id) }
        assertTrue(arrows.isEmpty(), "Arrows must be empty")

        assertIs<SystemMessageResponse.NotEnoughArrows>(
            context.responseChannel.receiveIgnoring(UpdateStatusResponse::class))
    }

}
