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
import org.l2kserver.game.handler.dto.response.StartMovingToAttackResponse
import org.l2kserver.game.handler.dto.response.StatusAttribute
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.model.actor.npc.NpcTemplateRegistry
import org.springframework.beans.factory.annotation.Autowired
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SkillServiceTest(
    @Autowired private val skillService: SkillService,
    @Autowired private val npcService: NpcService
): AbstractTests() {

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
            withContext(context) { skillService.useSkill(UseSkillRequest(MORTAL_BLOW.id, false, false)) }
        }
    }

    @Test
    fun shouldFailUsingSkillDueToUnsuitableTerms(): Unit = runBlocking {
        // Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Learn skill
        newSuspendedTransaction {
            SkillTable.insert {
                it[characterId] = character.id
                it[subclassIndex] = 0
                it[skillId] = MORTAL_BLOW.id
                it[skillLevel] = 1
            }
        }

        // Create our target
        val target = npcService.spawnAtPosition(
            template = NpcTemplateRegistry.register(GREMLIN.copy(ai = null)),
            spawnPosition = character.position.toSpawnPosition()
        )
        context.responseChannel.receive() //Skip NpcInfoResponse
        character.targetId = target.id

        withContext(context) { skillService.useSkill(UseSkillRequest(MORTAL_BLOW.id, false, false)) }
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
        newSuspendedTransaction {
            SkillTable.insert {
                it[characterId] = character.id
                it[subclassIndex] = 0
                it[skillId] = POWER_STRIKE.id
                it[skillLevel] = 1
            }
        }

        // Create our target
        val target = npcService.spawnAtPosition(
            template = NpcTemplateRegistry.register(GREMLIN.copy(ai = null)),
            spawnPosition = character.position.toSpawnPosition()
        )

        context.responseChannel.receive() //Skip NpcInfoResponse

        character.targetId = target.id
        target.targetedBy.add(character)

        withContext(context) {
            skillService.useSkill(UseSkillRequest(
                skillId = POWER_STRIKE.id,
                forced = false,
                holdPosition = false
            ))
        }

        // Check results
        assertIs<StartMovingToAttackResponse>(context.responseChannel.receive())
        val updateCharacterStatusResponse = assertIs<UpdateStatusResponse>(context.responseChannel.receive())
        assertEquals(character.id, updateCharacterStatusResponse.objectId)

        assertIs<SystemMessageResponse.YouUse>(context.responseChannel.receive())

        val gaugeResponse = assertIs<GaugeResponse>(context.responseChannel.receive())
        assertEquals(GaugeColor.BLUE, gaugeResponse.gaugeColor)

        val calculatedReuseDelay = 10406
        val skillUsedResponse = assertIs<SkillUsedResponse>(context.responseChannel.receive())
        assertEquals(character.id, skillUsedResponse.casterId)
        assertEquals(target.id, skillUsedResponse.targetId)
        assertEquals(POWER_STRIKE.id, skillUsedResponse.skillId)
        assertEquals(calculatedReuseDelay, skillUsedResponse.reuseDelay)

        val characterFightingResponse = assertIs<StartFightingResponse>(context.responseChannel.receive())
        assertEquals(character.id, characterFightingResponse.actorId)

        assertIs<ChangeMoveTypeResponse>(context.responseChannel.receive())

        val targetFightingResponse = assertIs<StartFightingResponse>(context.responseChannel.receive())
        assertEquals(target.id, targetFightingResponse.actorId)

        val damageResponse = assertIs<SystemMessageResponse.YouHit>(context.responseChannel.receiveIgnoring(
            SystemMessageResponse.CriticalHit::class))
        val updateStatusResponse = assertIs<UpdateStatusResponse>(context.responseChannel.receive())

        assertEquals(
            damageResponse.damage,
            target.stats.maxHp - (updateStatusResponse.attributes[StatusAttribute.CUR_HP] ?: 0)
        )
    }

    @Test
    fun shouldFailUsingSkillOnCooldown(): Unit = runBlocking {
        // Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Learn skill
        newSuspendedTransaction {
            SkillTable.insert {
                it[characterId] = character.id
                it[subclassIndex] = 0
                it[skillId] = POWER_STRIKE.id
                it[skillLevel] = 1
            }
        }

        // Create our target
        val target = npcService.spawnAtPosition(
            template = NpcTemplateRegistry.register(GREMLIN.copy(ai = null)),
            spawnPosition = character.position.toSpawnPosition()
        )
        context.responseChannel.receive() //Skip NpcInfoResponse
        character.targetId = target.id

        // First skill usage
        withContext(context) { skillService.useSkill(UseSkillRequest(POWER_STRIKE.id, false, false)) }

        assertIs<StartMovingToAttackResponse>(context.responseChannel.receive())
        assertIs<UpdateStatusResponse>(context.responseChannel.receive())
        assertIs<SystemMessageResponse.YouUse>(context.responseChannel.receive())
        assertIs<GaugeResponse>(context.responseChannel.receive())
        assertIs<SkillUsedResponse>(context.responseChannel.receive())

        //Consume target stance responses
        assertIs<StartFightingResponse>(context.responseChannel.receive())
        assertIs<ChangeMoveTypeResponse>(context.responseChannel.receive())

        assertIs<StartFightingResponse>(context.responseChannel.receive()) //attacker started fighting
        assertIs<SystemMessageResponse.YouHit>(context.responseChannel.receiveIgnoring(
            SystemMessageResponse.CriticalHit::class))


        delay(1000)
        // Second skill usage
        withContext(context) { skillService.useSkill(UseSkillRequest(POWER_STRIKE.id, false, false)) }

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
            withContext(context) { skillService.useSkill(UseSkillRequest(MORTAL_BLOW.id, false, false)) }
        }
    }

    @Test
    fun shouldFailUsingAttackSkillOnHimself(): Unit = runBlocking {
        // Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Learn skill
        newSuspendedTransaction {
            SkillTable.insert {
                it[characterId] = character.id
                it[subclassIndex] = 0
                it[skillId] = POWER_STRIKE.id
                it[skillLevel] = 1
            }
        }

        character.targetId = character.id

        withContext(context) { skillService.useSkill(UseSkillRequest(
            skillId = POWER_STRIKE.id,
            forced = false,
            holdPosition = false
        )) }

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
        newSuspendedTransaction {
            SkillTable.insert {
                it[characterId] = character.id
                it[subclassIndex] = 0
                it[skillId] = POWER_STRIKE.id
                it[skillLevel] = 1
            }
        }

        withContext(context) { skillService.useSkill(UseSkillRequest(POWER_STRIKE.id, false, false)) }

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
        newSuspendedTransaction {
            SkillTable.insert {
                it[characterId] = character.id
                it[subclassIndex] = 0
                it[skillId] = POWER_STRIKE.id
                it[skillLevel] = 1
            }
        }

        character.targetId = Random.nextInt()

        withContext(context) { skillService.useSkill(UseSkillRequest(POWER_STRIKE.id, false, false)) }

        // Check results
        assertIs<SystemMessageResponse.TargetCannotBeFound>(context.responseChannel.receive())
    }

    @Test
    fun shouldHealSelfWhenHpIsLow(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        // Learn self-heal
        newSuspendedTransaction {
            SkillTable.insert {
                it[characterId] = character.id
                it[subclassIndex] = 0
                it[skillId] = SELF_HEAL.id
                it[skillLevel] = 1
            }
            character.currentHp = 1
        }

        withContext(context) {
            skillService.useSkill(UseSkillRequest(
                skillId = SELF_HEAL.id,
                forced = false,
                holdPosition = false
            ))
        }

        // MP consumption update
        assertIs<UpdateStatusResponse>(context.responseChannel.receive())
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
        newSuspendedTransaction {
            SkillTable.insert {
                it[characterId] = character.id
                it[subclassIndex] = 0
                it[skillId] = SELF_HEAL.id
                it[skillLevel] = 1
            }

            character.currentHp = character.stats.maxHp - 10
        }

        withContext(context) {
            skillService.useSkill(UseSkillRequest(
                skillId = SELF_HEAL.id,
                forced = false,
                holdPosition = false
            ))
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

}
