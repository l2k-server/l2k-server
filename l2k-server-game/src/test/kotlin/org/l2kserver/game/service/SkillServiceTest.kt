package org.l2kserver.game.service

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.assertThrows
import org.l2kserver.game.AbstractTests
import org.l2kserver.game.data.npc.GREMLIN
import org.l2kserver.game.data.skill.MORTAL_BLOW
import org.l2kserver.game.data.skill.LIFE_SCAVENGE
import org.l2kserver.game.data.skill.POWER_STRIKE
import org.l2kserver.game.data.skill.SELF_HEAL
import org.l2kserver.game.domain.SkillTable
import org.l2kserver.game.extensions.receiveIgnoring
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
import org.l2kserver.game.model.actor.npc.NpcTemplateRegistry
import org.l2kserver.game.data.item.weapons.SQUIRES_SWORD
import org.l2kserver.game.data.skill.DEFENSE_AURA
import org.l2kserver.game.data.skill.MIGHT
import org.l2kserver.game.data.skill.POWER_SHOT
import org.l2kserver.game.handler.dto.response.AbnormalsListResponse
import org.l2kserver.game.handler.dto.response.FullCharacterResponse
import org.l2kserver.game.handler.dto.response.NpcInfoResponse
import org.l2kserver.game.handler.dto.response.PvPStatusResponse
import org.l2kserver.game.model.actor.character.PvpState
import org.l2kserver.game.model.skill.effect.AbnormalType
import org.springframework.beans.factory.annotation.Autowired
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SkillServiceTest(
    @Autowired private val skillService: SkillService,
    @Autowired private val npcService: NpcService
) : AbstractTests() {

    @Test
    fun shouldSuccessfullyGetSkillList(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        withContext(context) { skillService.getSkillList() }

        val skillListResponse = assertIs<SkillListResponse>(context.responseChannel.receive())
        assertEquals(0, skillListResponse.skills.size, "Skill list must be empty")
    }

    @Test
    fun shouldThrowExceptionIfCharacterUsesNonLearntSkill(): Unit = runBlocking {
        // Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        assertThrows<IllegalArgumentException> {
            withContext(context) { skillService.useSkill(UseSkillRequest(MORTAL_BLOW.id)) }
        }
    }

    @Test
    fun shouldFailUsingSkillDueToUnsuitableTerms(): Unit = runBlocking {
        // Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Learn skill
        character.skillsAndMagic.learn(MORTAL_BLOW.id, 1)

        // Create our target
        val target = npcService.spawnAtPosition(
            template = NpcTemplateRegistry.register(GREMLIN.copy(ai = null)),
            spawnPosition = character.position.toSpawnPosition()
        )
        context.responseChannel.receive() //Skip NpcInfoResponse
        character.targetId = target.id

        withContext(context) { skillService.useSkill(UseSkillRequest(MORTAL_BLOW.id)) }
        val playSoundResponse = assertIs<PlaySoundResponse>(context.responseChannel.receive())
        assertEquals(Sound.ITEMSOUND_SYS_IMPOSSIBLE, playSoundResponse.sound)
        assertIs<ActionFailedResponse>(context.responseChannel.receive())
    }

    @Test
    fun shouldSuccessfullyUseSingleTargetPhysicalDamageSkill(): Unit = runBlocking {
        // Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Learn skill
        character.skillsAndMagic.learn(POWER_STRIKE.id, 1)

        // Create our target
        val target = npcService.spawnAtPosition(
            template = NpcTemplateRegistry.register(GREMLIN.copy(ai = null)),
            spawnPosition = character.position.toSpawnPosition()
        )

        context.responseChannel.receive() //Skip NpcInfoResponse

        character.targetId = target.id
        target.targetedBy.add(character)

        withContext(context) {
            skillService.useSkill(UseSkillRequest(POWER_STRIKE.id))
        }

        // Check results
        assertIs<StartMovingToTargetResponse>(context.responseChannel.receive())

        assertIs<SystemMessageResponse.YouUse>(context.responseChannel.receive())
        val gaugeResponse = assertIs<GaugeResponse>(context.responseChannel.receive())
        assertEquals(GaugeColor.BLUE, gaugeResponse.gaugeColor)

        val calculatedReuseDelay = 10406
        val skillUsedResponse = assertIs<SkillUsedResponse>(context.responseChannel.receive())
        assertEquals(character.id, skillUsedResponse.casterId)
        assertEquals(target.id, skillUsedResponse.targetId)
        assertEquals(POWER_STRIKE.id, skillUsedResponse.skillId)
        assertEquals(calculatedReuseDelay, skillUsedResponse.reuseDelay)

        val damageResponse = assertIs<SystemMessageResponse.YouHit>(
            context.responseChannel.receiveIgnoring(
                SystemMessageResponse.CriticalHit::class
            )
        )

        val updateStatusResponse = assertIs<UpdateStatusResponse>(
            context.responseChannel.receiveIgnoring(
                SystemMessageResponse.OverHit::class
            )
        )

        assertEquals(
            damageResponse.damage,
            target.stats.maxHp - (updateStatusResponse.attributes[StatusAttribute.CUR_HP] ?: 0)
        )

        val updateCharacterStatusResponse = assertIs<UpdateStatusResponse>(context.responseChannel.receive())
        assertEquals(character.id, updateCharacterStatusResponse.objectId)

        val characterFightingResponse = assertIs<StartFightingResponse>(context.responseChannel.receive())
        assertEquals(character.id, characterFightingResponse.actorId)

        assertIs<ChangeMoveTypeResponse>(context.responseChannel.receive())

        val targetFightingResponse = assertIs<StartFightingResponse>(context.responseChannel.receive())
        assertEquals(target.id, targetFightingResponse.actorId)
    }

    @Test
    fun shouldFailUsingSkillOnCooldown(): Unit = runBlocking {
        // Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Learn skill
        character.skillsAndMagic.learn(POWER_STRIKE.id, 1)

        val fatDummyGremlinTemplate = NpcTemplateRegistry.register(
            GREMLIN.copy(
                stats = GREMLIN.stats.copy(maxHp = 1000), ai = null
            )
        )

        // Create our target
        val target = npcService.spawnAtPosition(
            template = fatDummyGremlinTemplate,
            spawnPosition = character.position.toSpawnPosition()
        )
        context.responseChannel.receive() //Skip NpcInfoResponse
        character.targetId = target.id

        // First skill usage
        withContext(context) { skillService.useSkill(UseSkillRequest(POWER_STRIKE.id)) }

        assertIs<StartMovingToTargetResponse>(context.responseChannel.receive())
        assertIs<SystemMessageResponse.YouUse>(context.responseChannel.receive())
        assertIs<GaugeResponse>(context.responseChannel.receive())
        assertIs<SkillUsedResponse>(context.responseChannel.receive())

        assertIs<SystemMessageResponse.YouHit>(
            context.responseChannel.receiveIgnoring(
                SystemMessageResponse.CriticalHit::class
            )
        )

        assertIs<UpdateStatusResponse>(context.responseChannel.receive())

        //Consume target stance responses
        assertIs<StartFightingResponse>(context.responseChannel.receive())
        assertIs<ChangeMoveTypeResponse>(context.responseChannel.receive())

        assertIs<StartFightingResponse>(context.responseChannel.receive()) //attacker started fighting

        delay(1000)
        // Second skill usage
        withContext(context) { skillService.useSkill(UseSkillRequest(POWER_STRIKE.id)) }

        val cooldownResponse = assertIs<SystemMessageResponse.IsBeingPreparedForReuse>(
            context.responseChannel.receiveIgnoring(
                SystemMessageResponse.OverHit::class,
                SystemMessageResponse.YouHaveAcquiredExpForOverHit::class
            )
        )

        assertEquals(POWER_STRIKE.id, cooldownResponse.skill.skillId)
        assertIs<ActionFailedResponse>(context.responseChannel.receive())
    }

    @Test
    fun shouldThrowExceptionIfCharacterUsesSkillLearntByAnotherSubclass(): Unit = runBlocking {
        // Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Learn skill
        newSuspendedTransaction {
            SkillTable.insert {
                it[characterId] = character.id
                it[subclassIndex] = 1
                it[skillId] = MORTAL_BLOW.id
                it[skillLevel] = 1
            }
        }

        assertThrows<IllegalArgumentException> {
            withContext(context) { skillService.useSkill(UseSkillRequest(MORTAL_BLOW.id)) }
        }
    }

    @Test
    fun shouldFailUsingAttackSkillOnHimself(): Unit = runBlocking {
        // Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Learn skill
        character.skillsAndMagic.learn(POWER_STRIKE.id, 1)
        character.targetId = character.id

        withContext(context) { skillService.useSkill(UseSkillRequest(POWER_STRIKE.id)) }

        // Check results
        assertIs<SystemMessageResponse.CannotUseThisOnYourself>(context.responseChannel.receive())
    }

    @Test
    fun shouldFailUsingAttackSkillWithNoTargetSelected(): Unit = runBlocking {
        // Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Learn skill
        character.skillsAndMagic.learn(POWER_STRIKE.id, 1)

        withContext(context) { skillService.useSkill(UseSkillRequest(POWER_STRIKE.id)) }

        // Check results
        assertIs<SystemMessageResponse.YouMustSelectTarget>(context.responseChannel.receive())
    }

    @Test
    fun shouldFailUsingAttackSkillOnNonExistingTarget(): Unit = runBlocking {
        // Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Learn skill
        character.skillsAndMagic.learn(POWER_STRIKE.id, 1)

        character.targetId = Random.nextInt()

        withContext(context) { skillService.useSkill(UseSkillRequest(POWER_STRIKE.id)) }

        // Check results
        assertIs<SystemMessageResponse.TargetCannotBeFound>(context.responseChannel.receive())
    }

    @Test
    fun shouldHealSelfWhenHpIsLow(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        // Learn self-heal
        character.skillsAndMagic.learn(SELF_HEAL.id, 1)
        transaction { character.currentHp = 1 }

        withContext(context) {
            skillService.useSkill(UseSkillRequest(SELF_HEAL.id))
        }


        assertIs<UpdateStatusResponse>(context.responseChannel.receive())

        // MP consumption update
        assertIs<SystemMessageResponse.YouUse>(context.responseChannel.receive())
        assertIs<GaugeResponse>(context.responseChannel.receive())
        assertIs<SkillUsedResponse>(context.responseChannel.receive())

        val updateAfterHeal = assertIs<UpdateStatusResponse>(context.responseChannel.receive())
        assertEquals(43, updateAfterHeal.attributes[StatusAttribute.CUR_HP] ?: 0)

        assertIs<SystemMessageResponse.HpRestored>(context.responseChannel.receive())
        transaction { assertEquals(43, character.currentHp) }
    }

    @Test
    fun shouldNotExceedMaxHpWhenHealing(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        // Learn self-heal
        character.skillsAndMagic.learn(SELF_HEAL.id, 1)
        transaction { character.currentHp = character.stats.maxHp - 10 }

        withContext(context) {
            skillService.useSkill(UseSkillRequest(SELF_HEAL.id))
        }

        // MP consumption update
        assertIs<UpdateStatusResponse>(context.responseChannel.receive())
        assertIs<SystemMessageResponse.YouUse>(context.responseChannel.receive())
        assertIs<GaugeResponse>(context.responseChannel.receive())
        assertIs<SkillUsedResponse>(context.responseChannel.receive())

        val updateAfterHeal = assertIs<UpdateStatusResponse>(context.responseChannel.receive())
        val hpAfterHeal = updateAfterHeal.attributes[StatusAttribute.CUR_HP] ?: 0
        assertEquals(character.stats.maxHp, hpAfterHeal)
        assertIs<SystemMessageResponse.HpRestored>(context.responseChannel.receive())
        transaction { assertEquals(character.stats.maxHp, character.currentHp) }
    }

    @Test
    fun shouldFailUsingSkillWhenNotEnoughMp(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        // Learn skill and create target
        character.skillsAndMagic.learn(POWER_STRIKE.id, 1)
        val target = npcService.spawnAtPosition(
            template = NpcTemplateRegistry.register(GREMLIN.copy(ai = null)),
            spawnPosition = character.position.toSpawnPosition()
        )
        context.responseChannel.receive() // Skip NpcInfoResponse

        // Not enough MP to cast
        transaction { character.currentMp = 0 }
        character.targetId = target.id

        withContext(context) { skillService.useSkill(UseSkillRequest(POWER_STRIKE.id)) }

        val playSoundResponse = assertIs<PlaySoundResponse>(context.responseChannel.receive())
        assertEquals(Sound.ITEMSOUND_SYS_IMPOSSIBLE, playSoundResponse.sound)
        assertIs<SystemMessageResponse.NotEnoughMp>(context.responseChannel.receive())
        assertIs<ActionFailedResponse>(context.responseChannel.receive())
    }

    @Test
    fun shouldFailUsingSkillWhenCharacterIsDead(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        // Learn self skill
        character.skillsAndMagic.learn(SELF_HEAL.id, 1)
        transaction { character.currentHp = 0 }

        withContext(context) { skillService.useSkill(UseSkillRequest(SELF_HEAL.id)) }

        // Character is dead: only ActionFailed expected
        assertIs<ActionFailedResponse>(context.responseChannel.receive())
    }

    @Test
    fun shouldReportTargetOutOfRangeWhenHoldPosition(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        character.skillsAndMagic.learn(POWER_STRIKE.id, 1)

        // Spawn target far away so that castRange is insufficient
        val target = npcService.spawnAtPosition(
            template = NpcTemplateRegistry.register(GREMLIN.copy(ai = null)),
            spawnPosition = character.position.copy(x = character.position.x + 10_000).toSpawnPosition()
        )

        character.targetId = target.id

        withContext(context) { skillService.useSkill(UseSkillRequest(POWER_STRIKE.id, holdPosition = true)) }

        // No movement occurs, should receive out of range message
        assertIs<SystemMessageResponse.TargetOutOfRange>(context.responseChannel.receive())
    }

    @Test
    fun shouldFailUsingAttackSkillOnDeadTarget(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        character.skillsAndMagic.learn(POWER_STRIKE.id, 1)

        val target = npcService.spawnAtPosition(
            template = NpcTemplateRegistry.register(GREMLIN.copy(ai = null)),
            spawnPosition = character.position.toSpawnPosition()
        )
        context.responseChannel.receive() // Skip NpcInfoResponse

        // Kill target
        transaction { target.currentHp = 0 }
        character.targetId = target.id

        withContext(context) { skillService.useSkill(UseSkillRequest(POWER_STRIKE.id)) }

        assertIs<SystemMessageResponse.IncorrectTarget>(context.responseChannel.receive())
        assertIs<ActionFailedResponse>(context.responseChannel.receive())
    }

    @Test
    fun shouldFailUsingCorpseSkillOnAliveTarget(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        // Learn corpse skill
        character.skillsAndMagic.learn(LIFE_SCAVENGE.id, 1)

        // Spawn alive target
        val target = npcService.spawnAtPosition(
            template = NpcTemplateRegistry.register(GREMLIN.copy(ai = null)),
            spawnPosition = character.position.toSpawnPosition()
        )
        context.responseChannel.receive() // Skip NpcInfoResponse

        character.targetId = target.id

        withContext(context) { skillService.useSkill(UseSkillRequest(LIFE_SCAVENGE.id)) }

        assertIs<SystemMessageResponse.IncorrectTarget>(context.responseChannel.receive())
        assertIs<ActionFailedResponse>(context.responseChannel.receive())
    }

    @Test
    fun shouldFailUsingSkillDueToWrongWeaponType(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        // Learn bow-only skill
        character.skillsAndMagic.learn(POWER_SHOT.id, 1)

        // Equip sword (not a bow)
        createTestItem(templateId = SQUIRES_SWORD.id, owner = character, isEquipped = true)

        // Create target
        val target = npcService.spawnAtPosition(
            template = NpcTemplateRegistry.register(GREMLIN.copy(ai = null)),
            spawnPosition = character.position.toSpawnPosition()
        )
        context.responseChannel.receive() // Skip NpcInfoResponse
        character.targetId = target.id

        withContext(context) { skillService.useSkill(UseSkillRequest(POWER_SHOT.id)) }

        val playSoundResponse = assertIs<PlaySoundResponse>(context.responseChannel.receive())
        assertEquals(Sound.ITEMSOUND_SYS_IMPOSSIBLE, playSoundResponse.sound)
        assertIs<ActionFailedResponse>(context.responseChannel.receive())
    }

    @Test
    fun shouldSuccessfullyUseBuff(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        character.skillsAndMagic.learn(DEFENSE_AURA.id, 1)
        withContext(context) { skillService.useSkill(UseSkillRequest(DEFENSE_AURA.id)) }

        assertIs<UpdateStatusResponse>(context.responseChannel.receive()) //mana spent to start casting
        assertIs<SystemMessageResponse.YouUse>(context.responseChannel.receive())

        val gaugeResponse = assertIs<GaugeResponse>(context.responseChannel.receive())
        assertEquals(GaugeColor.BLUE, gaugeResponse.gaugeColor)

        val calculatedReuseDelay = 4802
        val skillUsedResponse = assertIs<SkillUsedResponse>(context.responseChannel.receive())
        assertEquals(character.id, skillUsedResponse.casterId)
        assertEquals(character.id, skillUsedResponse.targetId)
        assertEquals(DEFENSE_AURA.id, skillUsedResponse.skillId)
        assertEquals(calculatedReuseDelay, skillUsedResponse.reuseDelay)

        assertIs<FullCharacterResponse>(context.responseChannel.receive())
        val abnormalsListResponse = assertIs<AbnormalsListResponse>(context.responseChannel.receive())
        assertEquals(1, abnormalsListResponse.abnormals.size)
        assertIs<UpdateStatusResponse>(context.responseChannel.receive()) //mana spent after casting

        assertEquals(1, character.abnormalEffects.size)
        assertEquals(AbnormalType.PD_UP, character.abnormalEffects.firstOrNull()?.abnormalType)
    }

    @Test
    fun shouldHaveOnlyOneEffectAfterSeveralBuffUsages(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        character.skillsAndMagic.learn(DEFENSE_AURA.id, 1)

        //First usage of buff
        withContext(context) { skillService.useSkill(UseSkillRequest(DEFENSE_AURA.id)) }

        assertIs<UpdateStatusResponse>(context.responseChannel.receive()) //mana spent to start casting
        assertIs<SystemMessageResponse.YouUse>(context.responseChannel.receive())
        assertIs<GaugeResponse>(context.responseChannel.receive())
        val skillUsedResponse = assertIs<SkillUsedResponse>(context.responseChannel.receive())
        assertIs<FullCharacterResponse>(context.responseChannel.receive())
        assertIs<AbnormalsListResponse>(context.responseChannel.receive())
        assertIs<UpdateStatusResponse>(context.responseChannel.receive()) //mana spent after casting

        //Wait for skill cooldown
        delay(skillUsedResponse.reuseDelay + 1L)

        //Second usage of buff
        withContext(context) { skillService.useSkill(UseSkillRequest(DEFENSE_AURA.id)) }

        assertIs<UpdateStatusResponse>(context.responseChannel.receive())
        assertIs<SystemMessageResponse.YouUse>(context.responseChannel.receive())
        assertIs<GaugeResponse>(context.responseChannel.receive())
        assertIs<SkillUsedResponse>(context.responseChannel.receive())
        assertIs<FullCharacterResponse>(context.responseChannel.receive())
        val abnormalsListResponse = assertIs<AbnormalsListResponse>(context.responseChannel.receive())
        assertEquals(1, abnormalsListResponse.abnormals.size)
        assertIs<UpdateStatusResponse>(context.responseChannel.receive())

        assertEquals(1, character.abnormalEffects.size)
    }

    @Test
    fun shouldSuccessfullyUseTargetBuff(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        // Spawn alive target
        val targetContext = createTestSessionContext()
        val targetCharacter = createTestCharacter(name = "FriendlyFriend")
        targetContext.setCharacterId(targetCharacter.id)

        character.skillsAndMagic.learn(MIGHT.id, 1)
        character.targetId = targetCharacter.id
        withContext(context) { skillService.useSkill(UseSkillRequest(MIGHT.id)) }

        //Check caster's responses
        assertIs<StartMovingToTargetResponse>(context.responseChannel.receive())
        assertIs<UpdateStatusResponse>(context.responseChannel.receive()) //mana spent to start casting
        assertIs<SystemMessageResponse.YouUse>(context.responseChannel.receive())

        val gaugeResponse = assertIs<GaugeResponse>(context.responseChannel.receive())
        assertEquals(GaugeColor.BLUE, gaugeResponse.gaugeColor)

        val calculatedReuseDelay = 4802
        val skillUsedResponse = assertIs<SkillUsedResponse>(context.responseChannel.receive())
        assertEquals(character.id, skillUsedResponse.casterId)
        assertEquals(targetCharacter.id, skillUsedResponse.targetId)
        assertEquals(MIGHT.id, skillUsedResponse.skillId)
        assertEquals(calculatedReuseDelay, skillUsedResponse.reuseDelay)

        assertIs<UpdateStatusResponse>(context.responseChannel.receive()) //mana spent after casting

        //Check target's responses
        val skillUsedResponseForTarget = assertIs<SkillUsedResponse>(targetContext.responseChannel.receive())
        assertEquals(character.id, skillUsedResponseForTarget.casterId)
        assertEquals(targetCharacter.id, skillUsedResponseForTarget.targetId)
        assertEquals(MIGHT.id, skillUsedResponseForTarget.skillId)
        assertEquals(calculatedReuseDelay, skillUsedResponseForTarget.reuseDelay)

        assertIs<FullCharacterResponse>(targetContext.responseChannel.receive())
        val abnormalsListResponse = assertIs<AbnormalsListResponse>(targetContext.responseChannel.receive())
        assertEquals(1, abnormalsListResponse.abnormals.size)

        assertEquals(1, targetCharacter.abnormalEffects.size)
        assertEquals(AbnormalType.PA_UP, targetCharacter.abnormalEffects.firstOrNull()?.abnormalType)
    }

    @Test
    fun shouldFailUsingTargetBuffOnEnemy(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        // Create our target
        val target = npcService.spawnAtPosition(
            template = NpcTemplateRegistry.register(GREMLIN.copy(ai = null)),
            spawnPosition = character.position.toSpawnPosition()
        )

        assertIs<NpcInfoResponse>(context.responseChannel.receive())

        character.skillsAndMagic.learn(MIGHT.id, 1)
        character.targetId = target.id
        withContext(context) { skillService.useSkill(UseSkillRequest(MIGHT.id)) }

        //Check caster's responses
        assertIs<SystemMessageResponse.IncorrectTarget>(context.responseChannel.receive())
        assertIs<ActionFailedResponse>(context.responseChannel.receive())
    }

    @Test
    fun shouldFailUsingTargetBuffOnDeadTarget(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        // Spawn alive target
        val targetContext = createTestSessionContext()
        val targetCharacter = createTestCharacter(name = "FriendlyFriend")
        targetContext.setCharacterId(targetCharacter.id)

        transaction { targetCharacter.currentHp = 0 }

        character.skillsAndMagic.learn(MIGHT.id, 1)
        character.targetId = targetCharacter.id
        withContext(context) { skillService.useSkill(UseSkillRequest(MIGHT.id)) }

        //Check caster's responses
        assertIs<SystemMessageResponse.IncorrectTarget>(context.responseChannel.receive())
        assertIs<ActionFailedResponse>(context.responseChannel.receive())
    }

    @Test
    fun shouldSuccessfullyUseForcedTargetBuffOnEnemy(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        // Create our target
        val target = npcService.spawnAtPosition(
            template = NpcTemplateRegistry.register(GREMLIN.copy(ai = null)),
            spawnPosition = character.position.toSpawnPosition()
        )

        assertIs<NpcInfoResponse>(context.responseChannel.receive())

        character.skillsAndMagic.learn(MIGHT.id, 1)
        character.targetId = target.id
        withContext(context) { skillService.useSkill(UseSkillRequest(MIGHT.id, forced = true)) }

        //Check caster's responses
        assertIs<StartMovingToTargetResponse>(context.responseChannel.receive())
        assertIs<UpdateStatusResponse>(context.responseChannel.receive()) //mana spent to start casting
        assertIs<SystemMessageResponse.YouUse>(context.responseChannel.receive())

        val gaugeResponse = assertIs<GaugeResponse>(context.responseChannel.receive())
        assertEquals(GaugeColor.BLUE, gaugeResponse.gaugeColor)

        val calculatedReuseDelay = 4802
        val skillUsedResponse = assertIs<SkillUsedResponse>(context.responseChannel.receive())
        assertEquals(character.id, skillUsedResponse.casterId)
        assertEquals(target.id, skillUsedResponse.targetId)
        assertEquals(MIGHT.id, skillUsedResponse.skillId)
        assertEquals(calculatedReuseDelay, skillUsedResponse.reuseDelay)

        assertIs<UpdateStatusResponse>(context.responseChannel.receive()) //mana spent after casting

        val pvpStatusResponse = assertIs<PvPStatusResponse>(context.responseChannel.receive())
        assertEquals(PvpState.PVP, pvpStatusResponse.pvpState)

        //Check target
        assertEquals(1, target.abnormalEffects.size)
        assertEquals(AbnormalType.PA_UP, target.abnormalEffects.firstOrNull()?.abnormalType)
    }

}
