package org.l2kserver.game.service

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.assertThrows
import org.l2kserver.game.AbstractTests
import org.l2kserver.game.data.npc.GREMLIN
import org.l2kserver.game.data.skill.MORTAL_BLOW
import org.l2kserver.game.data.skill.POWER_STRIKE
import org.l2kserver.game.domain.SkillsTable
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
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.model.actor.npc.NpcTemplate
import org.l2kserver.game.model.actor.position.toSpawnPosition
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
            SkillsTable.insert {
                it[characterId] = character.id
                it[subclassIndex] = 0
                it[skillId] = MORTAL_BLOW.id
                it[skillLevel] = 1
            }
        }

        // Create our target
        val target = npcService.spawnAtPosition(
            template = NpcTemplate.Registry.register(GREMLIN),
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
    fun shouldSuccessfullyUseSkill(): Unit = runBlocking {
        // Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Learn skill
        newSuspendedTransaction {
            SkillsTable.insert {
                it[characterId] = character.id
                it[subclassIndex] = 0
                it[skillId] = POWER_STRIKE.id
                it[skillLevel] = 1
            }
        }

        // Create our target
        val target = npcService.spawnAtPosition(
            template = NpcTemplate.Registry.register(GREMLIN),
            spawnPosition = character.position.toSpawnPosition()
        )
        context.responseChannel.receive() //Skip NpcInfoResponse
        character.targetId = target.id

        withContext(context) { skillService.useSkill(UseSkillRequest(POWER_STRIKE.id, false, false)) }

        // Check results
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

        //TODO Check damage
    }

    @Test
    fun shouldFailUsingSkillOnCooldown(): Unit = runBlocking {
        // Create our character
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Learn skill
        newSuspendedTransaction {
            SkillsTable.insert {
                it[characterId] = character.id
                it[subclassIndex] = 0
                it[skillId] = POWER_STRIKE.id
                it[skillLevel] = 1
            }
        }

        // Create our target
        val target = npcService.spawnAtPosition(
            template = NpcTemplate.Registry.register(GREMLIN),
            spawnPosition = character.position.toSpawnPosition()
        )
        context.responseChannel.receive() //Skip NpcInfoResponse
        character.targetId = target.id

        // First skill usage
        withContext(context) { skillService.useSkill(UseSkillRequest(POWER_STRIKE.id, false, false)) }

        assertIs<UpdateStatusResponse>(context.responseChannel.receive())
        assertIs<SystemMessageResponse.YouUse>(context.responseChannel.receive())
        assertIs<GaugeResponse>(context.responseChannel.receive())
        assertIs<SkillUsedResponse>(context.responseChannel.receive())

        //Consume target stance responses
        assertIs<ChangeMoveTypeResponse>(context.responseChannel.receive())
        assertIs<StartFightingResponse>(context.responseChannel.receive())
        assertIs<SystemMessageResponse.YouHit>(context.responseChannel.receive())

        delay(1000)
        // Second skill usage
        withContext(context) { skillService.useSkill(UseSkillRequest(POWER_STRIKE.id, false, false)) }

        val cooldownResponse = assertIs<SystemMessageResponse.IsBeingPreparedForReuse>(context.responseChannel.receive())
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
            SkillsTable.insert {
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
            SkillsTable.insert {
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
            SkillsTable.insert {
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
            SkillsTable.insert {
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


}
