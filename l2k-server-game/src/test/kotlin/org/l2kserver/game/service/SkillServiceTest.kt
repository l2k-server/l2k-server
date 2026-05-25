package org.l2kserver.game.service

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import org.l2kserver.game.AbstractTests
import org.l2kserver.game.data.characterclass.HumanMystic
import org.l2kserver.game.data.skill.MortalBlow
import org.l2kserver.game.data.skill.LifeScavenge
import org.l2kserver.game.data.skill.PowerStrike
import org.l2kserver.game.data.skill.SelfHeal
import org.l2kserver.game.domain.SkillTable
import org.l2kserver.game.extensions.pullResponse
import org.l2kserver.game.extensions.toSpawnPosition
import org.l2kserver.game.handler.dto.request.UseSkillRequest
import org.l2kserver.game.handler.dto.response.ActionFailedResponse
import org.l2kserver.game.handler.dto.response.ChangeMoveTypeResponse
import org.l2kserver.game.handler.dto.response.PlaySoundResponse
import org.l2kserver.game.handler.dto.response.GaugeColor
import org.l2kserver.game.handler.dto.response.GaugeResponse
import org.l2kserver.game.handler.dto.response.SkillListResponse
import org.l2kserver.game.handler.dto.response.SkillUsedResponse
import org.l2kserver.game.handler.dto.response.Sound
import org.l2kserver.game.handler.dto.response.StartFightingResponse
import org.l2kserver.game.handler.dto.response.StartMovingToTargetResponse
import org.l2kserver.game.handler.dto.response.StatusAttribute
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.model.actor.npc.NpcRegistry
import org.l2kserver.game.data.item.weapon.SquiresSword
import org.l2kserver.game.data.npc.FatDummyGremlin
import org.l2kserver.game.data.skill.DefenseAura
import org.l2kserver.game.data.skill.Might
import org.l2kserver.game.data.skill.PowerShot
import org.l2kserver.game.data.skill.Resurrection
import org.l2kserver.game.handler.dto.response.ConfirmDialogResponse
import org.l2kserver.game.handler.dto.response.TemporalEffectsResponse
import org.l2kserver.game.handler.dto.response.FullCharacterResponse
import org.l2kserver.game.handler.dto.response.NpcInfoResponse
import org.l2kserver.game.handler.dto.response.PvPStatusResponse
import org.l2kserver.game.model.actor.character.PvpState
import org.l2kserver.game.model.skill.effect.AbnormalType
import org.l2kserver.game.network.session.sessionContextOf
import org.springframework.beans.factory.annotation.Autowired
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SkillServiceTest @Autowired constructor(
    private val skillService: SkillService,
    private val npcService: NpcService
) : AbstractTests() {

    @Test
    fun shouldSuccessfullyGetSkillList(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        withContext(context) { skillService.getSkillList() }

        val skillListResponse = assertIs<SkillListResponse>(context.pullResponse())
        assertEquals(0, skillListResponse.skills.size, "Skill list must be empty")
    }

    @Test
    fun shouldThrowExceptionIfCharacterUsesNonLearntSkill(): Unit = runBlocking {
        // Create our character
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        assertThrows<IllegalArgumentException> {
            withContext(context) { skillService.useSkill(UseSkillRequest(MortalBlow.id)) }
        }
    }

    @Test
    fun shouldFailUsingSkillDueToUnsuitableTerms(): Unit = runBlocking {
        // Create our character
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        //Learn skill
        character.skillsAndMagic.learn(MortalBlow.id, 1)

        // Create our target
        val target = npcService.spawnAtPosition(
            template = NpcRegistry.register(FatDummyGremlin),
            spawnPosition = character.position.toSpawnPosition()
        )
        context.pullResponse() //Skip NpcInfoResponse
        character.targetId = target.id

        withContext(context) { skillService.useSkill(UseSkillRequest(MortalBlow.id)) }
        val playSoundResponse = assertIs<PlaySoundResponse>(context.pullResponse())
        assertEquals(Sound.ITEMSOUND_SYS_IMPOSSIBLE, playSoundResponse.sound)
        assertIs<ActionFailedResponse>(context.pullResponse())
    }

    @Test
    fun shouldSuccessfullyUseSingleTargetPhysicalDamageSkill(): Unit = runBlocking {
        // Create our character
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        //Learn skill
        character.skillsAndMagic.learn(PowerStrike.id, 1)

        // Create our target
        val target = npcService.spawnAtPosition(
            template = NpcRegistry.register(FatDummyGremlin),
            spawnPosition = character.position.toSpawnPosition()
        )

        context.pullResponse() //Skip NpcInfoResponse

        character.targetId = target.id
        target.targetedBy.add(character)

        withContext(context) {
            skillService.useSkill(UseSkillRequest(PowerStrike.id))
        }

        // Check results
        assertIs<StartMovingToTargetResponse>(context.pullResponse())

        assertIs<SystemMessageResponse.YouUse>(context.pullResponse())
        val gaugeResponse = assertIs<GaugeResponse>(context.pullResponse())
        assertEquals(GaugeColor.BLUE, gaugeResponse.gaugeColor)

        val calculatedReuseDelay = 10406
        val skillUsedResponse = assertIs<SkillUsedResponse>(context.pullResponse())
        assertEquals(character.id, skillUsedResponse.casterId)
        assertEquals(target.id, skillUsedResponse.targetId)
        assertEquals(PowerStrike.id, skillUsedResponse.skillId)
        assertEquals(calculatedReuseDelay, skillUsedResponse.reuseDelay)

        val updateStatusResponse = assertIs<UpdateStatusResponse>(
            context.pullResponse(SystemMessageResponse.OverHit::class)
        )

        assertEquals(character.id, updateStatusResponse.objectId)

        val characterFightingResponse = assertIs<StartFightingResponse>(context.pullResponse())
        assertEquals(character.id, characterFightingResponse.actorId)

        // After being attacked NPC changes it's moveType to RUN TODO delete after moving this logic to AI
        assertIs<ChangeMoveTypeResponse>(context.pullResponse())

        val targetFightingResponse = assertIs<StartFightingResponse>(context.pullResponse())
        assertEquals(target.id, targetFightingResponse.actorId)

        val damageResponse = assertIs<SystemMessageResponse.YouHit>(
            context.pullResponse(
                SystemMessageResponse.CriticalHit::class
            )
        )

        val updateCharacterStatusResponse = assertIs<UpdateStatusResponse>(context.pullResponse())
        assertEquals(target.id, updateCharacterStatusResponse.objectId)

        assertEquals(
            damageResponse.damage,
            target.stats.maxHp.roundToInt() - (updateCharacterStatusResponse.attributes[StatusAttribute.CUR_HP] ?: 0)
        )
    }

    @Test
    fun shouldFailUsingSkillOnCooldown(): Unit = runBlocking {
        // Create our character
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        //Learn skill
        character.skillsAndMagic.learn(PowerStrike.id, 1)

        // Create our target
        val target = npcService.spawnAtPosition(
            template = FatDummyGremlin,
            spawnPosition = character.position.toSpawnPosition()
        )
        context.pullResponse() //Skip NpcInfoResponse
        character.targetId = target.id

        // First skill usage
        withContext(context) { skillService.useSkill(UseSkillRequest(PowerStrike.id)) }

        assertIs<StartMovingToTargetResponse>(context.pullResponse())
        assertIs<SystemMessageResponse.YouUse>(context.pullResponse())
        assertIs<GaugeResponse>(context.pullResponse())
        assertIs<SkillUsedResponse>(context.pullResponse())
        assertIs<UpdateStatusResponse>(context.pullResponse())

        //Consume target stance responses
        val attackerStartedFighting = assertIs<StartFightingResponse>(context.pullResponse())
        assertEquals(character.id, attackerStartedFighting.actorId)

        val moveTypeChanged = assertIs<ChangeMoveTypeResponse>(context.pullResponse())
        assertEquals(target.id, moveTypeChanged.actorId)

        val targetStartedFighting = assertIs<StartFightingResponse>(context.pullResponse())
        assertEquals(target.id, targetStartedFighting.actorId)

        assertIs<SystemMessageResponse.YouHit>(
            context.pullResponse(SystemMessageResponse.CriticalHit::class)
        )

        delay(1000)
        // Second skill usage
        withContext(context) { skillService.useSkill(UseSkillRequest(PowerStrike.id)) }

        val cooldownResponse = assertIs<SystemMessageResponse.IsBeingPreparedForReuse>(
            context.pullResponse(
                SystemMessageResponse.OverHit::class,
                SystemMessageResponse.YouHaveAcquiredExpForOverHit::class
            )
        )

        assertEquals(PowerStrike.id, cooldownResponse.skill.skillId)
        assertIs<ActionFailedResponse>(context.pullResponse())
    }

    @Test
    fun shouldThrowExceptionIfCharacterUsesSkillLearntByAnotherSubclass(): Unit = runBlocking {
        // Create our character
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        //Learn skill
        transaction {
            SkillTable.insert {
                it[characterId] = character.id
                it[subclassIndex] = 1
                it[skillId] = MortalBlow.id
                it[skillLevel] = 1
            }
        }

        assertThrows<IllegalArgumentException> {
            withContext(context) { skillService.useSkill(UseSkillRequest(MortalBlow.id)) }
        }
    }

    @Test
    fun shouldFailUsingAttackSkillOnHimself(): Unit = runBlocking {
        // Create our character
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        //Learn skill
        character.skillsAndMagic.learn(PowerStrike.id, 1)
        character.targetId = character.id

        withContext(context) { skillService.useSkill(UseSkillRequest(PowerStrike.id)) }

        // Check results
        assertIs<SystemMessageResponse.CannotUseThisOnYourself>(context.pullResponse())
    }

    @Test
    fun shouldFailUsingAttackSkillWithNoTargetSelected(): Unit = runBlocking {
        // Create our character
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        //Learn skill
        character.skillsAndMagic.learn(PowerStrike.id, 1)

        withContext(context) { skillService.useSkill(UseSkillRequest(PowerStrike.id)) }

        // Check results
        assertIs<SystemMessageResponse.YouMustSelectTarget>(context.pullResponse())
    }

    @Test
    fun shouldFailUsingAttackSkillOnNonExistingTarget(): Unit = runBlocking {
        // Create our character
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        //Learn skill
        character.skillsAndMagic.learn(PowerStrike.id, 1)

        character.targetId = Random.nextInt()

        withContext(context) { skillService.useSkill(UseSkillRequest(PowerStrike.id)) }

        // Check results
        assertIs<SystemMessageResponse.TargetCannotBeFound>(context.pullResponse())
    }

    @Test
    fun shouldHealSelfWhenHpIsLow(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        // Learn self-heal
        character.skillsAndMagic.learn(SelfHeal.id, 1)
        transaction { character.currentHp = 1 }

        withContext(context) {
            skillService.useSkill(UseSkillRequest(SelfHeal.id))
        }

        val updateAfterManaSpentToStart = assertIs<UpdateStatusResponse>(context.pullResponse())
        assertEquals(
            character.stats.maxMp.roundToInt() - SelfHeal.consumesToStart.mp!![0],
            updateAfterManaSpentToStart.attributes[StatusAttribute.CUR_MP])

        assertIs<SystemMessageResponse.YouUse>(context.pullResponse())
        assertIs<GaugeResponse>(context.pullResponse())
        assertIs<SkillUsedResponse>(context.pullResponse())

        delay(10000) //Wait for casting completes

        val updateAfterManaSpentAfterCast = assertIs<UpdateStatusResponse>(context.pullResponse())
        assertEquals(
            character.stats.maxMp.roundToInt() - SelfHeal.consumesToStart.mp!![0] - SelfHeal.consumes.mp!![0],
            updateAfterManaSpentAfterCast.attributes[StatusAttribute.CUR_MP]
        )

        val updateAfterHeal = assertIs<UpdateStatusResponse>(context.pullResponse())
        assertEquals(43, updateAfterHeal.attributes[StatusAttribute.CUR_HP] ?: 0)

        assertIs<SystemMessageResponse.HpRestored>(context.pullResponse())
        transaction { assertEquals(43, character.currentHp) }
    }

    @Test
    fun shouldNotExceedMaxHpWhenHealing(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        // Learn self-heal
        character.skillsAndMagic.learn(SelfHeal.id, 1)
        transaction { character.currentHp = character.stats.maxHp.roundToInt() - 10 }

        withContext(context) {
            skillService.useSkill(UseSkillRequest(SelfHeal.id))
        }

        // MP consumption update
        val updateAfterManaSpentToStart = assertIs<UpdateStatusResponse>(context.pullResponse())
        assertEquals(
            character.stats.maxMp.roundToInt() - SelfHeal.consumesToStart.mp!![0],
            updateAfterManaSpentToStart.attributes[StatusAttribute.CUR_MP])

        assertIs<SystemMessageResponse.YouUse>(context.pullResponse())
        assertIs<GaugeResponse>(context.pullResponse())
        assertIs<SkillUsedResponse>(context.pullResponse())

        delay(10000) //Wait for casting completes

        val updateAfterManaSpentAfterCast = assertIs<UpdateStatusResponse>(context.pullResponse())
        assertEquals(
            character.stats.maxMp.roundToInt() - SelfHeal.consumesToStart.mp!![0] - SelfHeal.consumes.mp!![0],
            updateAfterManaSpentAfterCast.attributes[StatusAttribute.CUR_MP]
        )

        val updateAfterHeal = assertIs<UpdateStatusResponse>(context.pullResponse())
        assertEquals(character.stats.maxHp.roundToInt(), updateAfterHeal.attributes[StatusAttribute.CUR_HP] ?: 0)

        assertIs<SystemMessageResponse.HpRestored>(context.pullResponse())
        transaction { assertEquals(character.stats.maxHp.roundToInt(), character.currentHp) }
    }

    @Test
    fun shouldFailUsingSkillWhenNotEnoughMp(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        // Learn skill and create target
        character.skillsAndMagic.learn(PowerStrike.id, 1)
        val target = npcService.spawnAtPosition(
            template = NpcRegistry.register(FatDummyGremlin),
            spawnPosition = character.position.toSpawnPosition()
        )
        context.pullResponse() // Skip NpcInfoResponse

        // Not enough MP to cast
        transaction { character.currentMp = 0 }
        character.targetId = target.id

        withContext(context) { skillService.useSkill(UseSkillRequest(PowerStrike.id)) }

        val playSoundResponse = assertIs<PlaySoundResponse>(context.pullResponse())
        assertEquals(Sound.ITEMSOUND_SYS_IMPOSSIBLE, playSoundResponse.sound)
        assertIs<SystemMessageResponse.NotEnoughMp>(context.pullResponse())
        assertIs<ActionFailedResponse>(context.pullResponse())
    }

    @Test
    fun shouldFailUsingSkillWhenCharacterIsDead(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        // Learn self skill
        character.skillsAndMagic.learn(SelfHeal.id, 1)
        transaction { character.currentHp = 0 }

        withContext(context) { skillService.useSkill(UseSkillRequest(SelfHeal.id)) }

        // Character is dead: only ActionFailed expected
        assertIs<ActionFailedResponse>(context.pullResponse())
    }

    @Test
    fun shouldReportTargetOutOfRangeWhenHoldPosition(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        character.skillsAndMagic.learn(PowerStrike.id, 1)

        // Spawn target far away so that castRange is insufficient
        val target = npcService.spawnAtPosition(
            template = NpcRegistry.register(FatDummyGremlin),
            spawnPosition = character.position.copy(x = character.position.x + 10_000).toSpawnPosition()
        )

        character.targetId = target.id

        withContext(context) { skillService.useSkill(UseSkillRequest(PowerStrike.id, holdPosition = true)) }

        // No movement occurs, should receive out of range message
        assertIs<SystemMessageResponse.TargetOutOfRange>(context.pullResponse())
    }

    @Test
    fun shouldFailUsingAttackSkillOnDeadTarget(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        character.skillsAndMagic.learn(PowerStrike.id, 1)

        val target = npcService.spawnAtPosition(
            template = NpcRegistry.register(FatDummyGremlin),
            spawnPosition = character.position.toSpawnPosition()
        )
        context.pullResponse() // Skip NpcInfoResponse

        // Kill target
        transaction { target.currentHp = 0 }
        character.targetId = target.id

        withContext(context) { skillService.useSkill(UseSkillRequest(PowerStrike.id)) }

        assertIs<SystemMessageResponse.IncorrectTarget>(context.pullResponse())
        assertIs<ActionFailedResponse>(context.pullResponse())
    }

    @Test
    fun shouldFailUsingCorpseSkillOnAliveTarget(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        // Learn corpse skill
        character.skillsAndMagic.learn(LifeScavenge.id, 1)

        // Spawn alive target
        val target = npcService.spawnAtPosition(
            template = NpcRegistry.register(FatDummyGremlin),
            spawnPosition = character.position.toSpawnPosition()
        )
        context.pullResponse() // Skip NpcInfoResponse

        character.targetId = target.id

        withContext(context) { skillService.useSkill(UseSkillRequest(LifeScavenge.id)) }

        assertIs<SystemMessageResponse.IncorrectTarget>(context.pullResponse())
        assertIs<ActionFailedResponse>(context.pullResponse())
    }

    @Test
    fun shouldFailUsingSkillDueToWrongWeaponType(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        // Learn bow-only skill
        character.skillsAndMagic.learn(PowerShot.id, 1)

        // Equip sword (not a bow)
        createTestItem(templateId = SquiresSword.id, owner = character, isEquipped = true)

        // Create target
        val target = npcService.spawnAtPosition(
            template = NpcRegistry.register(FatDummyGremlin),
            spawnPosition = character.position.toSpawnPosition()
        )
        context.pullResponse() // Skip NpcInfoResponse
        character.targetId = target.id

        withContext(context) { skillService.useSkill(UseSkillRequest(PowerShot.id)) }

        val playSoundResponse = assertIs<PlaySoundResponse>(context.pullResponse())
        assertEquals(Sound.ITEMSOUND_SYS_IMPOSSIBLE, playSoundResponse.sound)
        assertIs<ActionFailedResponse>(context.pullResponse())
    }

    @Test
    fun shouldSuccessfullyUseBuff(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        character.skillsAndMagic.learn(DefenseAura.id, 1)
        withContext(context) { skillService.useSkill(UseSkillRequest(DefenseAura.id)) }

        assertIs<UpdateStatusResponse>(context.pullResponse()) //mana spent to start casting
        assertIs<SystemMessageResponse.YouUse>(context.pullResponse())

        val gaugeResponse = assertIs<GaugeResponse>(context.pullResponse())
        assertEquals(GaugeColor.BLUE, gaugeResponse.gaugeColor)

        val calculatedReuseDelay = 9380
        val skillUsedResponse = assertIs<SkillUsedResponse>(context.pullResponse())
        assertEquals(character.id, skillUsedResponse.casterId)
        assertEquals(character.id, skillUsedResponse.targetId)
        assertEquals(DefenseAura.id, skillUsedResponse.skillId)
        assertEquals(calculatedReuseDelay, skillUsedResponse.reuseDelay)
        assertIs<UpdateStatusResponse>(context.pullResponse(timeout = 10000)) //mana spent after casting

        assertIs<FullCharacterResponse>(context.pullResponse())
        val temporalEffectsResponse = assertIs<TemporalEffectsResponse>(context.pullResponse())
        assertEquals(1, temporalEffectsResponse.abnormals.size)

        assertEquals(1, character.temporalEffects.size)
        assertEquals(AbnormalType.PD_UP, character.temporalEffects.firstOrNull()?.abnormalType)
    }

    @Test
    fun shouldHaveOnlyOneEffectAfterSeveralBuffUsages(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        character.skillsAndMagic.learn(DefenseAura.id, 1)

        //First usage of buff
        withContext(context) { skillService.useSkill(UseSkillRequest(DefenseAura.id)) }

        val updateManaBeforeCasting = assertIs<UpdateStatusResponse>(context.pullResponse())
        assertEquals(character.id, updateManaBeforeCasting.objectId)
        assertEquals(
            character.stats.maxMp.toInt() - DefenseAura.consumesToStart.mp!![0],
            updateManaBeforeCasting.attributes[StatusAttribute.CUR_MP]
        )

        assertIs<SystemMessageResponse.YouUse>(context.pullResponse())
        assertIs<GaugeResponse>(context.pullResponse())
        val skillUsedResponse = assertIs<SkillUsedResponse>(context.pullResponse())

        delay(10000) //Wait for casting completes

        val updateManaAfterCasting = assertIs<UpdateStatusResponse>(context.pullResponse())
        assertEquals(character.id, updateManaAfterCasting.objectId)
        assertEquals(
            expected =
                character.stats.maxMp.toInt() - DefenseAura.consumesToStart.mp!![0] - DefenseAura.consumes.mp!![0],
            actual = updateManaAfterCasting.attributes[StatusAttribute.CUR_MP]
        )

        assertIs<FullCharacterResponse>(context.pullResponse())
        assertIs<TemporalEffectsResponse>(context.pullResponse())

        //Wait for skill cooldown
        delay(skillUsedResponse.reuseDelay + 1L)

        //Second usage of buff
        withContext(context) { skillService.useSkill(UseSkillRequest(DefenseAura.id)) }

        assertIs<UpdateStatusResponse>(context.pullResponse())
        assertIs<SystemMessageResponse.YouUse>(context.pullResponse())
        assertIs<GaugeResponse>(context.pullResponse())
        assertIs<SkillUsedResponse>(context.pullResponse())

        delay(10000) //Wait for casting completes

        assertIs<UpdateStatusResponse>(context.pullResponse())
        assertIs<FullCharacterResponse>(context.pullResponse())
        val temporalEffectsResponse = assertIs<TemporalEffectsResponse>(context.pullResponse())
        assertEquals(1, temporalEffectsResponse.abnormals.size)

        assertEquals(1, character.temporalEffects.size)
    }

    @Test
    fun shouldSuccessfullyUseTargetBuff(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        // Spawn alive target
        val targetCharacter = createTestCharacter(name = "FriendlyFriend")
        val targetContext = sessionContextOf(targetCharacter.id)!!

        character.skillsAndMagic.learn(Might.id, 1)
        character.targetId = targetCharacter.id
        withContext(context) { skillService.useSkill(UseSkillRequest(Might.id)) }

        //Check caster's responses
        assertIs<StartMovingToTargetResponse>(context.pullResponse())
        assertIs<UpdateStatusResponse>(context.pullResponse()) //mana spent to start casting
        assertIs<SystemMessageResponse.YouUse>(context.pullResponse())

        val gaugeResponse = assertIs<GaugeResponse>(context.pullResponse())
        assertEquals(GaugeColor.BLUE, gaugeResponse.gaugeColor)

        val calculatedReuseDelay = 9380
        val skillUsedResponse = assertIs<SkillUsedResponse>(context.pullResponse())
        assertEquals(character.id, skillUsedResponse.casterId)
        assertEquals(targetCharacter.id, skillUsedResponse.targetId)
        assertEquals(Might.id, skillUsedResponse.skillId)
        assertEquals(calculatedReuseDelay, skillUsedResponse.reuseDelay)

        delay(10000) //Wait for casting completes

        assertIs<UpdateStatusResponse>(context.pullResponse()) //mana spent after casting

        //Check target's responses
        val skillUsedResponseForTarget = assertIs<SkillUsedResponse>(targetContext.pullResponse())
        assertEquals(character.id, skillUsedResponseForTarget.casterId)
        assertEquals(targetCharacter.id, skillUsedResponseForTarget.targetId)
        assertEquals(Might.id, skillUsedResponseForTarget.skillId)
        assertEquals(calculatedReuseDelay, skillUsedResponseForTarget.reuseDelay)

        assertIs<FullCharacterResponse>(targetContext.pullResponse())
        val temporalEffectsResponse = assertIs<TemporalEffectsResponse>(targetContext.pullResponse())
        assertEquals(1, temporalEffectsResponse.abnormals.size)

        assertEquals(1, targetCharacter.temporalEffects.size)
        assertEquals(AbnormalType.PA_UP, targetCharacter.temporalEffects.firstOrNull()?.abnormalType)
    }

    @Test
    fun shouldFailUsingTargetBuffOnEnemy(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        // Create our target
        val target = npcService.spawnAtPosition(
            template = NpcRegistry.register(FatDummyGremlin),
            spawnPosition = character.position.toSpawnPosition()
        )

        assertIs<NpcInfoResponse>(context.pullResponse())

        character.skillsAndMagic.learn(Might.id, 1)
        character.targetId = target.id
        withContext(context) { skillService.useSkill(UseSkillRequest(Might.id)) }

        //Check caster's responses
        assertIs<SystemMessageResponse.IncorrectTarget>(context.pullResponse())
        assertIs<ActionFailedResponse>(context.pullResponse())
    }

    @Test
    fun shouldFailUsingTargetBuffOnDeadTarget(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        // Spawn alive target
        val targetCharacter = createTestCharacter(name = "FriendlyFriend")

        transaction { targetCharacter.currentHp = 0 }

        character.skillsAndMagic.learn(Might.id, 1)
        character.targetId = targetCharacter.id
        withContext(context) { skillService.useSkill(UseSkillRequest(Might.id)) }

        //Check caster's responses
        assertIs<SystemMessageResponse.IncorrectTarget>(context.pullResponse())
        assertIs<ActionFailedResponse>(context.pullResponse())
    }

    @Test
    fun shouldSuccessfullyUseForcedTargetBuffOnEnemy(): Unit = runBlocking {
        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        // Create our target
        val target = npcService.spawnAtPosition(
            template = NpcRegistry.register(FatDummyGremlin),
            spawnPosition = character.position.toSpawnPosition()
        )

        assertIs<NpcInfoResponse>(context.pullResponse())

        character.skillsAndMagic.learn(Might.id, 1)
        character.targetId = target.id
        withContext(context) { skillService.useSkill(UseSkillRequest(Might.id, forced = true)) }

        //Check caster's responses
        assertIs<StartMovingToTargetResponse>(context.pullResponse())
        assertIs<UpdateStatusResponse>(context.pullResponse()) //mana spent to start casting
        assertIs<SystemMessageResponse.YouUse>(context.pullResponse())

        val gaugeResponse = assertIs<GaugeResponse>(context.pullResponse())
        assertEquals(GaugeColor.BLUE, gaugeResponse.gaugeColor)

        val calculatedReuseDelay = 9380
        val skillUsedResponse = assertIs<SkillUsedResponse>(context.pullResponse())
        assertEquals(character.id, skillUsedResponse.casterId)
        assertEquals(target.id, skillUsedResponse.targetId)
        assertEquals(Might.id, skillUsedResponse.skillId)
        assertEquals(calculatedReuseDelay, skillUsedResponse.reuseDelay)

        delay(10000) //Wait for casting ends

        assertIs<UpdateStatusResponse>(context.pullResponse()) //mana spent after casting

        val pvpStatusResponse = assertIs<PvPStatusResponse>(context.pullResponse())
        assertEquals(PvpState.PVP, pvpStatusResponse.pvpState)

        //Check target
        assertEquals(1, target.temporalEffects.size)
        assertEquals(AbnormalType.PA_UP, target.temporalEffects.firstOrNull()?.abnormalType)
    }

    @Test
    fun shouldSuccessfullyResurrectCharacter(): Unit = runBlocking {
        // Spawn alive target
        val targetCharacter = createTestCharacter(name = "FriendlyFriend")
        transaction { targetCharacter.currentHp = 0 }
        val targetContext = sessionContextOf(targetCharacter.id)!!

        //Create character and learn Resurrection
        val character = createTestCharacter(classId = HumanMystic.id)
        val context = sessionContextOf(character.id)!!
        transaction { character.position = targetCharacter.position }

        character.skillsAndMagic.learn(Resurrection.id, 1)
        character.targetId = targetCharacter.id

        withContext(context) { skillService.useSkill(UseSkillRequest(Resurrection.id)) }

        //Check caster's responses
        assertIs<StartMovingToTargetResponse>(context.pullResponse())
        assertIs<UpdateStatusResponse>(context.pullResponse()) //mana spent to start casting
        assertIs<SystemMessageResponse.YouUse>(context.pullResponse())

        val gaugeResponse = assertIs<GaugeResponse>(context.pullResponse())
        assertEquals(GaugeColor.BLUE, gaugeResponse.gaugeColor)

        val calculatedReuseDelay = 120000
        val skillUsedResponse = assertIs<SkillUsedResponse>(context.pullResponse())
        assertEquals(character.id, skillUsedResponse.casterId)
        assertEquals(targetCharacter.id, skillUsedResponse.targetId)
        assertEquals(Resurrection.id, skillUsedResponse.skillId)
        assertEquals(calculatedReuseDelay, skillUsedResponse.reuseDelay)

        delay(10000) //Wait for casting ends

        assertIs<UpdateStatusResponse>(context.pullResponse()) //mana spent after casting

        //Check target
        val skillUsedResponseForTarget = assertIs<SkillUsedResponse>(targetContext.pullResponse())
        assertEquals(character.id, skillUsedResponseForTarget.casterId)

        val resurrectionResponse = assertIs<ConfirmDialogResponse.Resurrection>(targetContext.pullResponse())
        assertEquals(character.name, resurrectionResponse.resurrectedBy)

        assertTrue(targetCharacter.resurrectionIsPending)
        assertNotNull(targetCharacter.expRestoredByResurrection)
    }

    @Test
    fun shouldFailResurrectingTargetIsAlreadyResurrected(): Unit = runBlocking {
        // Spawn alive target
        val targetCharacter = createTestCharacter(name = "FriendlyFriend")
        transaction { targetCharacter.currentHp = 0 }
        targetCharacter.expRestoredByResurrection = 1000

        //Create character and learn Resurrection
        val character = createTestCharacter(classId = HumanMystic.id)
        val context = sessionContextOf(character.id)!!
        transaction { character.position = targetCharacter.position }

        character.skillsAndMagic.learn(Resurrection.id, 1)
        character.targetId = targetCharacter.id

        withContext(context) { skillService.useSkill(UseSkillRequest(Resurrection.id)) }

        //Check caster's responses
        assertIs<SystemMessageResponse.ResurrectionAlreadyProposed>(context.pullResponse())
    }

}
