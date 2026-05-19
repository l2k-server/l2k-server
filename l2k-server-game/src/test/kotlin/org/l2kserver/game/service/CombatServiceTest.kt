package org.l2kserver.game.service

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.Test
import org.l2kserver.game.AbstractTests
import org.l2kserver.game.data.item.arrow.WoodenArrow
import org.l2kserver.game.data.item.soulshot.SoulshotNoGrade
import org.l2kserver.game.data.item.weapon.Bow
import org.l2kserver.game.data.npc.FatDummyGremlin
import org.l2kserver.game.extensions.pullResponse
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
import org.l2kserver.game.extensions.toSpawnPosition
import org.l2kserver.game.handler.dto.request.UseItemRequest
import org.l2kserver.game.handler.dto.response.ActorDiedResponse
import org.l2kserver.game.handler.dto.response.ChangeMoveTypeResponse
import org.l2kserver.game.handler.dto.response.FullCharacterResponse
import org.l2kserver.game.handler.dto.response.NpcInfoResponse
import org.l2kserver.game.handler.dto.response.ShotUsedResponse
import org.l2kserver.game.handler.dto.response.item
import org.l2kserver.game.handler.dto.response.operation
import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.character.PvpState
import org.l2kserver.game.model.actor.npc.NpcRegistry
import org.l2kserver.game.model.item.WeaponInstanceImpl
import org.l2kserver.game.model.skill.effect.TemporalAbnormalEffect
import org.l2kserver.game.model.stats.CombatStats
import org.l2kserver.game.network.session.sessionContextOf
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import kotlin.math.roundToInt
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CombatServiceTest @Autowired constructor(
    private val combatService: CombatService,
    private val itemService: ItemService,
    private val npcService: NpcService
) : AbstractTests() {

    @Test
    fun shouldPerformAttackOnOtherCharacter(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        val targetCharacter = createTestCharacter(name = "PunchingBag")
        val targetContext = sessionContextOf(targetCharacter.id)!!

        //To make sure target won't avoid attack
        targetCharacter.temporalEffects.add(object: TemporalAbnormalEffect(Duration.ofHours(1)) {
            override val effectLevel = 1
            override val skillId = 1000
            override val abnormalType = "EVASION_DOWN"
            override val targetId = targetCharacter.id

            override fun getFixedBonusStats(actor: ActorInstance) = CombatStats(evasion = -1000)
        })

        character.targetId = targetCharacter.id

        //Launch attack
        combatService.attack(character, targetCharacter)

        // Check attacker's responses
        val attackResponse = assertIs<AttackResponse>(context.pullResponse())
        assertEquals(character.id, attackResponse.attacker.id)
        assertEquals(1, attackResponse.attacks.size)

        val hit = attackResponse.attacks[0]
        assertEquals(targetCharacter.id, hit.targetId)

        var systemMessageResponse = assertIs<SystemMessageResponse>(context.pullResponse())
        if (systemMessageResponse is SystemMessageResponse.CriticalHit)
            systemMessageResponse = assertIs<SystemMessageResponse>(context.pullResponse())

        assertContains(
            listOf(SystemMessageResponse.YouMissed::class, SystemMessageResponse.YouHit::class),
            systemMessageResponse::class
        )

        val startFightingResponse = assertIs<StartFightingResponse>(context.pullResponse())
        assertEquals(character.id, startFightingResponse.actorId)

        val pvpStatusResponse = assertIs<PvPStatusResponse>(context.pullResponse())
        assertEquals(character.id, pvpStatusResponse.characterId)
        assertEquals(PvpState.PVP, pvpStatusResponse.pvpState)

        val targetStartFightingResponse = assertIs<StartFightingResponse>(context.pullResponse())
        assertEquals(targetCharacter.id, targetStartFightingResponse.actorId)

        //Check target's responses
        val attackResponseForTarget = assertIs<AttackResponse>(targetContext.pullResponse())
        assertEquals(character.id, attackResponseForTarget.attacker.id)
        assertEquals(1, attackResponse.attacks.size)
        assertEquals(hit, attackResponse.attacks[0])

        val systemMessageResponseForTarget = assertIs<SystemMessageResponse>(
            targetContext.pullResponse(SystemMessageResponse.CriticalHit::class)
        )

        assertContains(
            listOf(SystemMessageResponse.YouWereHitBy::class, SystemMessageResponse.YouHaveAvoidedAttackOf::class),
            systemMessageResponseForTarget::class
        )

        if (systemMessageResponseForTarget is SystemMessageResponse.YouWereHitBy) {
            val updateStatusResponse = assertIs<UpdateStatusResponse>(targetContext.pullResponse())
            assertEquals(targetCharacter.id, updateStatusResponse.objectId)
            assertContains(updateStatusResponse.attributes.keys, StatusAttribute.CUR_CP)
        }

        val startFightingResponseForTarget = assertIs<StartFightingResponse>(targetContext.pullResponse())
        assertEquals(character.id, startFightingResponseForTarget.actorId)

        val attackerPvPStatusResponse = assertIs<PvPStatusResponse>(targetContext.pullResponse())
        assertEquals(character.id, attackerPvPStatusResponse.characterId)

        val targetStartFightingResponseForTarget =
            assertIs<StartFightingResponse>(targetContext.pullResponse())
        assertEquals(targetCharacter.id, targetStartFightingResponseForTarget.actorId)
    }

    @Test
    fun shouldPerformAttackUsingSoulshot(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        val soulshot = character.inventory.createItem(SoulshotNoGrade.id, 10)

        val otherCharacter = createTestCharacter(name = "PunchingBag")
        val otherContext = sessionContextOf(otherCharacter.id)!!

        val target = npcService.spawnAtPosition(
            template = NpcRegistry.register(FatDummyGremlin),
            spawnPosition = character.position.toSpawnPosition()
        )

        // Display monster to attacker and observer
        assertIs<NpcInfoResponse>(context.pullResponse())
        assertIs<NpcInfoResponse>(otherContext.pullResponse())

        withContext(context) { itemService.useItem(UseItemRequest(soulshot.id)) }

        // Display using soulshot - to attacker and observer
        assertIs<SystemMessageResponse.SoulshotEnabled>(context.pullResponse())
        val updateItemsResponse = assertIs<UpdateItemsResponse>(context.pullResponse())
        assertEquals(soulshot.id, updateItemsResponse.operations.first().item.id)
        assertEquals(9, updateItemsResponse.operations.first().item.amount)

        assertIs<ShotUsedResponse>(context.pullResponse())
        assertIs<ShotUsedResponse>(otherContext.pullResponse())

        character.targetId = otherCharacter.id

        // Launch single attack
        combatService.attack(character, target)

        // --- Check attacker's responses ---

        val attackResponse = assertIs<AttackResponse>(context.pullResponse())
        assertEquals(character.id, attackResponse.attacker.id)
        assertEquals(1, attackResponse.attacks.size)
        assertTrue(attackResponse.usedSoulshot)

        val attack = attackResponse.attacks[0]
        assertEquals(target.id, attack.targetId)

        var systemMessageResponse = assertIs<SystemMessageResponse>(context.pullResponse())
        if (systemMessageResponse is SystemMessageResponse.CriticalHit)
            systemMessageResponse = assertIs<SystemMessageResponse>(context.pullResponse())

        assertIs<SystemMessageResponse.YouHit>(systemMessageResponse)

        val startFightingResponse = assertIs<StartFightingResponse>(context.pullResponse())
        assertEquals(character.id, startFightingResponse.actorId)

        // After being attacked NPC changes it's moveType to RUN TODO delete after moving this logic to AI
        assertIs<ChangeMoveTypeResponse>(context.pullResponse())

        val targetStartFightingResponse = assertIs<StartFightingResponse>(context.pullResponse())
        assertEquals(target.id, targetStartFightingResponse.actorId)

        // --- Check other's responses ---

        val attackResponseForTarget = assertIs<AttackResponse>(otherContext.pullResponse())
        assertTrue(attackResponseForTarget.usedSoulshot)
        assertEquals(character.id, attackResponseForTarget.attacker.id)
        assertEquals(1, attackResponse.attacks.size)
        assertEquals(attack, attackResponse.attacks[0])

        val startFightingResponseForTarget =
            assertIs<StartFightingResponse>(otherContext.pullResponse())
        assertEquals(character.id, startFightingResponseForTarget.actorId)

        // After attack NPC changes it's moveType to RUN TODO delete after moving this logic to AI
        assertIs<ChangeMoveTypeResponse>(otherContext.pullResponse())

        val targetStartFightingResponseForTarget =
            assertIs<StartFightingResponse>(otherContext.pullResponse())
        assertEquals(target.id, targetStartFightingResponseForTarget.actorId)
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
        //To make sure target won't avoid attack
        targetCharacter.temporalEffects.add(object: TemporalAbnormalEffect(Duration.ofHours(1)) {
            override val effectLevel = 1
            override val skillId = 1000
            override val abnormalType = "EVASION_DOWN"
            override val targetId = targetCharacter.id

            override fun getFixedBonusStats(actor: ActorInstance) = CombatStats(evasion = -1000)
        })

        character.targetId = targetCharacter.id
        //Launching attacking in parallel is not needed - process must stop after arrows run out
        withContext(context) {
            //Fail if attack process hasn't ended for some reason
            withTimeout(10_000L) { combatService.attack(character, targetCharacter) }
        }

        // Check attacker's responses
        val updateItemsResponse = assertIs<UpdateItemsResponse>(context.pullResponse())
        assertEquals(UpdateItemOperation.REMOVE, updateItemsResponse.operations.first().operation)
        assertEquals(arrowsId, updateItemsResponse.operations.first().item.id)

        val updateStatusResponse = assertIs<UpdateStatusResponse>(context.pullResponse())
        assertEquals(
            character.stats.maxMp.roundToInt() - bow.manaCost,
            updateStatusResponse.attributes[StatusAttribute.CUR_MP]
        )

        assertIs<SystemMessageResponse.YouCarefullyNockAnArrow>(context.pullResponse())
        assertIs<GaugeResponse>(context.pullResponse())

        val attackResponse = assertIs<AttackResponse>(context.pullResponse())
        assertEquals(character.id, attackResponse.attacker.id)
        assertEquals(1, attackResponse.attacks.size)

        val hit = attackResponse.attacks[0]
        assertEquals(targetCharacter.id, hit.targetId)

        var systemMessageResponse = assertIs<SystemMessageResponse>(context.pullResponse())
        if (systemMessageResponse is SystemMessageResponse.CriticalHit)
            systemMessageResponse = assertIs<SystemMessageResponse>(context.pullResponse())

        assertContains(
            listOf(SystemMessageResponse.YouMissed::class, SystemMessageResponse.YouHit::class),
            systemMessageResponse::class
        )

        val startFightingResponse = assertIs<StartFightingResponse>(context.pullResponse())
        assertEquals(character.id, startFightingResponse.actorId)

        val pvpStatusResponse = assertIs<PvPStatusResponse>(context.pullResponse())
        assertEquals(character.id, pvpStatusResponse.characterId)
        assertEquals(PvpState.PVP, pvpStatusResponse.pvpState)

        val targetStartFightingResponse = assertIs<StartFightingResponse>(context.pullResponse())
        assertEquals(targetCharacter.id, targetStartFightingResponse.actorId)

        //Check arrow amount after attack
        val arrows = transaction {
            ItemEntity.findAllByOwnerIdAndTemplateId(character.id, WoodenArrow.id).toList()
        }
        assertTrue(arrows.isEmpty(), "Arrows must be empty")
    }

    @Test
    fun shouldKillPeacefulCharacter(): Unit = runBlocking {
        //Create character
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        //Create target character
        val targetCharacter = createTestCharacter(name = "InnocentLamb")
        val targetContext = sessionContextOf(targetCharacter.id)!!
        //To make sure target won't avoid attack
        targetCharacter.temporalEffects.add(object: TemporalAbnormalEffect(Duration.ofHours(1)) {
            override val effectLevel = 1
            override val skillId = 1000
            override val abnormalType = "EVASION_DOWN"
            override val targetId = targetCharacter.id

            override fun getFixedBonusStats(actor: ActorInstance) = CombatStats(evasion = -1000)
        })

        transaction {
            targetCharacter.currentHp = 1
            targetCharacter.currentCp = 0
        }

        character.targetId = targetCharacter.id

        combatService.attack(character, targetCharacter)

        // Check attacker's responses
        val attackResponse = assertIs<AttackResponse>(context.pullResponse())
        assertEquals(character.id, attackResponse.attacker.id)
        assertEquals(1, attackResponse.attacks.size)

        val hit = attackResponse.attacks[0]
        assertEquals(targetCharacter.id, hit.targetId)

        var systemMessageResponse = assertIs<SystemMessageResponse>(context.pullResponse())
        if (systemMessageResponse is SystemMessageResponse.CriticalHit)
            systemMessageResponse = assertIs<SystemMessageResponse>(context.pullResponse())

        assertContains(
            listOf(SystemMessageResponse.YouMissed::class, SystemMessageResponse.YouHit::class),
            systemMessageResponse::class
        )

        val startFightingResponse = assertIs<StartFightingResponse>(context.pullResponse())
        assertEquals(character.id, startFightingResponse.actorId)

        val pvpStatusResponseToPvp = assertIs<PvPStatusResponse>(context.pullResponse())
        assertEquals(character.id, pvpStatusResponseToPvp.characterId)
        assertEquals(PvpState.PVP, pvpStatusResponseToPvp.pvpState)
        assertEquals(0, pvpStatusResponseToPvp.karma)
        assertEquals(true, pvpStatusResponseToPvp.isEnemy)

        val pvpStatusResponseToPk = assertIs<PvPStatusResponse>(context.pullResponse())
        assertEquals(character.id, pvpStatusResponseToPk.characterId)
        assertNotEquals(0, pvpStatusResponseToPk.karma)
        assertEquals(true, pvpStatusResponseToPk.isEnemy)

        val fullCharacterResponse = assertIs<FullCharacterResponse>(context.pullResponse())
        assertEquals(character.id, fullCharacterResponse.character.id)
        assertEquals(1, fullCharacterResponse.character.pkCount)
        assertNotEquals(0, fullCharacterResponse.character.karma)

        val targetDiedResponse = assertIs<ActorDiedResponse>(context.pullResponse())
        assertEquals(targetCharacter.id, targetDiedResponse.actorId)

        //Check target's responses
        val attackResponseForTarget = assertIs<AttackResponse>(targetContext.pullResponse())
        assertEquals(character.id, attackResponseForTarget.attacker.id)
        assertEquals(1, attackResponse.attacks.size)
        assertEquals(hit, attackResponse.attacks[0])

        val systemMessageResponseForTarget = assertIs<SystemMessageResponse>(
            targetContext.pullResponse(SystemMessageResponse.CriticalHit::class)
        )

        assertContains(
            listOf(SystemMessageResponse.YouWereHitBy::class, SystemMessageResponse.YouHaveAvoidedAttackOf::class),
            systemMessageResponseForTarget::class
        )

        if (systemMessageResponseForTarget is SystemMessageResponse.YouWereHitBy) {
            val updateStatusResponse = assertIs<UpdateStatusResponse>(targetContext.pullResponse())
            assertEquals(targetCharacter.id, updateStatusResponse.objectId)
            assertContains(updateStatusResponse.attributes.keys, StatusAttribute.CUR_CP)
        }

        val startFightingResponseForTarget =
            assertIs<StartFightingResponse>(targetContext.pullResponse())
        assertEquals(character.id, startFightingResponseForTarget.actorId)

        val pvpStatusResponseToPvpForTarget = assertIs<PvPStatusResponse>(targetContext.pullResponse())
        assertEquals(character.id, pvpStatusResponseToPvpForTarget.characterId)
        assertEquals(PvpState.PVP, pvpStatusResponseToPvpForTarget.pvpState)
        assertEquals(0, pvpStatusResponseToPvpForTarget.karma)
        assertEquals(true, pvpStatusResponseToPvpForTarget.isEnemy)

        val pvpStatusResponseToPkForTarget = assertIs<PvPStatusResponse>(targetContext.pullResponse())
        assertEquals(character.id, pvpStatusResponseToPkForTarget.characterId)
        assertEquals(PvpState.PVP, pvpStatusResponseToPkForTarget.pvpState)
        assertNotEquals(0, pvpStatusResponseToPkForTarget.karma)
        assertEquals(true, pvpStatusResponseToPkForTarget.isEnemy)

        val targetDiedResponseForTarget = assertIs<ActorDiedResponse>(targetContext.pullResponse())
        assertEquals(targetCharacter.id, targetDiedResponseForTarget.actorId)
        // Check states

        assertEquals(0, character.pvpCount)
        assertEquals(1, character.pkCount)
        assertNotEquals(0, character.karma)
    }

    @Test
    fun shouldKillChaoticCharacter(): Unit = runBlocking {
        //Create character
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        //Create target character
        val targetCharacter = createTestCharacter(name = "CruelVillain")
        val targetContext = sessionContextOf(targetCharacter.id)!!

        //To make sure target won't avoid attack
        targetCharacter.temporalEffects.add(object: TemporalAbnormalEffect(Duration.ofHours(1)) {
            override val effectLevel = 1
            override val skillId = 1000
            override val abnormalType = "EVASION_DOWN"
            override val targetId = targetCharacter.id

            override fun getFixedBonusStats(actor: ActorInstance) = CombatStats(evasion = -1000)
        })

        transaction {
            targetCharacter.karma = 10_000
            targetCharacter.currentHp = 1
            targetCharacter.currentCp = 0
        }

        character.targetId = targetCharacter.id
        combatService.attack(character, targetCharacter)

        // Check attacker's responses
        val attackResponse = assertIs<AttackResponse>(context.pullResponse())
        assertEquals(character.id, attackResponse.attacker.id)
        assertEquals(1, attackResponse.attacks.size)

        val hit = attackResponse.attacks[0]
        assertEquals(targetCharacter.id, hit.targetId)

        var systemMessageResponse = assertIs<SystemMessageResponse>(context.pullResponse())
        if (systemMessageResponse is SystemMessageResponse.CriticalHit)
            systemMessageResponse = assertIs<SystemMessageResponse>(context.pullResponse())

        assertContains(
            listOf(SystemMessageResponse.YouMissed::class, SystemMessageResponse.YouHit::class),
            systemMessageResponse::class
        )

        val startFightingResponse = assertIs<StartFightingResponse>(context.pullResponse())
        assertEquals(character.id, startFightingResponse.actorId)

        val fullCharacterResponse = assertIs<FullCharacterResponse>(context.pullResponse())
        assertEquals(character.id, fullCharacterResponse.character.id)
        assertEquals(1, fullCharacterResponse.character.pvpCount)

        val targetDiedResponse = assertIs<ActorDiedResponse>(context.pullResponse())
        assertEquals(targetCharacter.id, targetDiedResponse.actorId)

        val targetPvPStatusResponse = assertIs<PvPStatusResponse>(context.pullResponse())
        assertEquals(targetCharacter.id, targetPvPStatusResponse.characterId)
        assertEquals(0, targetPvPStatusResponse.karma)
        assertEquals(false, targetPvPStatusResponse.isEnemy)

        //Check target's responses
        val attackResponseForTarget = assertIs<AttackResponse>(targetContext.pullResponse())
        assertEquals(character.id, attackResponseForTarget.attacker.id)
        assertEquals(1, attackResponse.attacks.size)
        assertEquals(hit, attackResponse.attacks[0])

        val systemMessageResponseForTarget = assertIs<SystemMessageResponse>(
            targetContext.pullResponse(SystemMessageResponse.CriticalHit::class)
        )

        assertContains(
            listOf(SystemMessageResponse.YouWereHitBy::class, SystemMessageResponse.YouHaveAvoidedAttackOf::class),
            systemMessageResponseForTarget::class
        )

        if (systemMessageResponseForTarget is SystemMessageResponse.YouWereHitBy) {
            val updateStatusResponse = assertIs<UpdateStatusResponse>(targetContext.pullResponse())
            assertEquals(targetCharacter.id, updateStatusResponse.objectId)
            assertContains(updateStatusResponse.attributes.keys, StatusAttribute.CUR_CP)
        }

        val startFightingResponseForTarget =
            assertIs<StartFightingResponse>(targetContext.pullResponse())
        assertEquals(character.id, startFightingResponseForTarget.actorId)

        val targetDiedResponseForTarget = assertIs<ActorDiedResponse>(targetContext.pullResponse())
        assertEquals(targetCharacter.id, targetDiedResponseForTarget.actorId)

        val targetPvPStatusResponseForTarget = assertIs<PvPStatusResponse>(targetContext.pullResponse())
        assertEquals(targetCharacter.id, targetPvPStatusResponseForTarget.characterId)
        assertEquals(0, targetPvPStatusResponseForTarget.karma)
        assertEquals(false, targetPvPStatusResponseForTarget.isEnemy)

        val targetFullCharacterResponseForTarget = assertIs<FullCharacterResponse>(targetContext.pullResponse())
        assertEquals(0, targetFullCharacterResponseForTarget.character.karma)
        // Check states

        // After attacking PK should not enable PVP state
        assertEquals(PvpState.NOT_IN_PVP, character.pvpState)
        // After killing PK should give PVP score
        assertEquals(1, character.pvpCount)
        // After killing PK should not give PK score
        assertEquals(0, character.pkCount)
        // After killing PK should not give karma
        assertEquals(0, character.karma)

        //After death karma should be set to 0
        assertEquals(0, targetCharacter.karma)

        //TODO Drop from PK
    }

}

