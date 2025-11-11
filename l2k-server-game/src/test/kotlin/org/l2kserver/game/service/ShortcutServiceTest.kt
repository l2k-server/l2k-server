package org.l2kserver.game.service

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.l2kserver.game.AbstractTests
import org.l2kserver.game.handler.dto.request.CreateShortcutRequest
import org.l2kserver.game.handler.dto.response.CreateShortcutResponse
import org.l2kserver.game.model.actor.character.ShortcutType
import org.l2kserver.game.repository.ShortcutRepository
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class ShortcutServiceTest(
    @param:Autowired private val shortcutService: ShortcutService,
    @param:Autowired private val shortcutRepository: ShortcutRepository,
): AbstractTests() {

    @Test
    fun shouldSuccessfullyCreateShortcut(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val testActionType = ShortcutType.ACTION
        val testActionId = 2
        val testIndex = 0

        val request = CreateShortcutRequest(
            type = testActionType,
            index = testIndex,
            shortcutActionId = testActionId
        )
        withContext(context) { shortcutService.registerShortcut(request) }

        val response = assertIs<CreateShortcutResponse>(context.responseChannel.receive())

        assertEquals(testActionType, response.shortcut.type)
        assertEquals(testIndex, response.shortcut.index)
        assertEquals(testActionId, response.shortcut.shortcutActionId)

        transaction {
            val savedShortcut = assertNotNull(shortcutRepository.findBy(
                testIndex, character.id, character.activeSubclass
            ))

            assertEquals(testActionType, savedShortcut.type)
            assertEquals(testIndex, savedShortcut.index)
            assertEquals(testActionId, savedShortcut.shortcutActionId)
        }
    }

    @Test
    fun shouldSuccessfullyCreateShortcutOfSkill(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        val testActionType = ShortcutType.SKILL
        val testActionId = 3
        val testIndex = 0
        val testSkillLevel = 1

        character.skillsAndMagic.learn(skillId = testActionId, skillLevel = testSkillLevel)

        val request = CreateShortcutRequest(
            type = testActionType,
            index = testIndex,
            shortcutActionId = testActionId
        )
        withContext(context){ shortcutService.registerShortcut(request) }

        val response = assertIs<CreateShortcutResponse>(context.responseChannel.receive())

        assertEquals(testActionType, response.shortcut.type)
        assertEquals(testIndex, response.shortcut.index)
        assertEquals(testActionId, response.shortcut.shortcutActionId)
        assertEquals(testSkillLevel, response.shortcut.actionLevel)

        transaction {
            val savedShortcut = assertNotNull(shortcutRepository.findBy(
                testIndex,
                character.id,
                character.activeSubclass
            ))

            assertEquals(testActionType, savedShortcut.type)
            assertEquals(testIndex, savedShortcut.index)
            assertEquals(testActionId, savedShortcut.shortcutActionId)
            assertEquals(testSkillLevel, savedShortcut.actionLevel)
        }
    }

}
