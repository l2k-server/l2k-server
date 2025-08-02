package org.l2kserver.game.service

import java.time.Duration
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import org.l2kserver.game.AbstractTests
import org.l2kserver.game.handler.dto.request.CreateCharacterRequest
import org.l2kserver.game.handler.dto.request.DeleteCharacterRequest
import org.l2kserver.game.handler.dto.request.RestoreCharacterRequest
import org.l2kserver.game.handler.dto.request.SelectCharacterRequest
import org.l2kserver.game.handler.dto.response.CharacterListResponse
import org.l2kserver.game.handler.dto.response.CreateCharacterFailReason
import org.l2kserver.game.handler.dto.response.CreateCharacterFailResponse
import org.l2kserver.game.handler.dto.response.DeleteCharacterFailReason
import org.l2kserver.game.handler.dto.response.DeleteCharacterFailResponse
import org.l2kserver.game.handler.dto.response.SelectCharacterResponse
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.awaitility.kotlin.await
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.assertThrows
import org.l2kserver.game.data.character.HUMAN_FIGHTER_CLASS
import org.l2kserver.game.domain.PlayerCharacterEntity
import org.l2kserver.game.domain.Shortcut
import org.l2kserver.game.domain.LearnedSkillEntity
import org.l2kserver.game.extensions.findAllByCharacterId
import org.l2kserver.game.extensions.model.item.findAllByOwnerId
import org.l2kserver.game.handler.dto.response.ExitGameResponse
import org.l2kserver.game.handler.dto.response.FullCharacterResponse
import org.l2kserver.game.handler.dto.response.InventoryResponse
import org.l2kserver.game.handler.dto.response.RestartResponse
import org.l2kserver.game.handler.dto.response.ShortcutPanelResponse
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.actor.character.CharacterRace
import org.l2kserver.game.model.actor.character.Gender
import org.l2kserver.game.model.item.Item
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CharacterServiceTests(
    @Autowired private val characterService: CharacterService
) : AbstractTests() {

    @Test
    fun shouldSuccessfullyCreateCharacter(): Unit = runBlocking {
        val context = createTestSessionContext()

        val testCharacterName = "Vitalya"
        val testRace = CharacterRace.HUMAN
        val testGender = Gender.MALE
        val testClassName = HUMAN_FIGHTER_CLASS.name
        val testHairColor = 2
        val testHairStyle = 1
        val testFaceType = 3


        withContext(context) {
            characterService.createCharacter(
                request = createCreateCharacterRequest(
                    characterName = testCharacterName,
                    raceId = testRace.ordinal,
                    genderId = testGender.ordinal,
                    classId = testClassName.id,
                    hairStyle = testHairStyle,
                    hairColor = testHairColor,
                    faceType = testFaceType,
                ),
            )
        }

        val response = assertIs<CharacterListResponse>(context.responseChannel.receive())

        assertEquals(testCharacterName, response.playerCharacters[0].name)
        assertEquals(testRace, response.playerCharacters[0].race)
        assertEquals(testClassName, response.playerCharacters[0].characterClass.name)
        assertEquals(testGender, response.playerCharacters[0].gender)
        assertEquals(testHairColor, response.playerCharacters[0].hairColor)
        assertEquals(testHairStyle, response.playerCharacters[0].hairStyle)
        assertEquals(testFaceType, response.playerCharacters[0].faceType)
    }

    @Test
    fun shouldReturnErrorIfCharacterNameExists(): Unit = runBlocking {
        val context = createTestSessionContext()

        withContext(context) { characterService.createCharacter(createCreateCharacterRequest()) }
        context.responseChannel.receive()


        withContext(context) { characterService.createCharacter(createCreateCharacterRequest(genderId = 1)) }

        val response = assertIs<CreateCharacterFailResponse>(context.responseChannel.receive())
        assertEquals(CreateCharacterFailReason.NAME_ALREADY_EXISTS, response.reason)
    }

    @Test
    fun shouldReturnErrorIfCharacterNameDoNotMatchRegexp(): Unit = runBlocking {
        val context = createTestSessionContext()

        withContext(context) { characterService.createCharacter(createCreateCharacterRequest(characterName = "13")) }

        val response = assertIs<CreateCharacterFailResponse>(context.responseChannel.receive())
        assertEquals(CreateCharacterFailReason.NAME_EXCEED_16_CHARACTERS, response.reason)
    }

    @Test
    fun shouldReturnErrorIfThereAre7Characters(): Unit = runBlocking {
        val context = createTestSessionContext()
        withContext(context) {
            repeat(7) {
                characterService.createCharacter(createCreateCharacterRequest(characterName = "kek$it"))
                context.responseChannel.receive()
            }

            characterService.createCharacter(createCreateCharacterRequest())
        }

        val response = assertIs<CreateCharacterFailResponse>(context.responseChannel.receive())
        assertEquals(CreateCharacterFailReason.TOO_MANY_CHARACTERS, response.reason)
    }

    @Test
    fun shouldSuccessfullySetDeletionTime(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()

        withContext(context) { characterService.deleteCharacter(DeleteCharacterRequest(0)) }

        val response = assertIs<CharacterListResponse>(context.responseChannel.receive())
        assertEquals(character.id, response.playerCharacters[0].id)
        assertNotNull(response.playerCharacters[0].deletionDate)
    }

    @Test
    fun shouldGetDeletionFailedIfCharacterSlotIsEmpty(): Unit = runBlocking {
        val context = createTestSessionContext()
        withContext(context) { characterService.deleteCharacter(DeleteCharacterRequest(0)) }

        val response = assertIs<DeleteCharacterFailResponse>(context.responseChannel.receive())
        assertEquals(DeleteCharacterFailReason.DELETION_FAILED, response.reason)
    }

    @Test
    fun shouldRestoreCharacter(): Unit = runBlocking {
        val context = createTestSessionContext()
        val characterToDelete = createTestCharacter()

        newSuspendedTransaction { characterToDelete.deletionDate = LocalDateTime.now().plusDays(7) }

        withContext(context) { characterService.restoreCharacter(RestoreCharacterRequest(0)) }

        assertNull(newSuspendedTransaction { PlayerCharacter.findById(characterToDelete.id).deletionDate })
    }

    @Test
    fun shouldSuccessfullySelectCharacter(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter(enterGame = false)

        withContext(context) { characterService.selectCharacter(SelectCharacterRequest(0)) }

        val response = assertIs<SelectCharacterResponse>(context.responseChannel.receive())
        assertEquals(character.name, response.selectedPlayerCharacter.name)
        assertEquals(character.id, context.getCharacterId())
    }

    @Test
    fun shouldDeleteCharacterIfDeletionDateTimeHasPassed(): Unit = runBlocking {
        //Create character
        val character = createTestCharacter(enterGame = false)
        //Set character's deletion date to NOW
        newSuspendedTransaction {
            val characterEntity = PlayerCharacterEntity.findById(character.id)!!
            characterEntity.deletionDate = LocalDateTime.now()
        }

        //Wait for deletion job completion
        delay(60_000)
        await.atMost(Duration.ofSeconds(1)).untilAsserted {
            val context = createTestSessionContext()
            runBlocking(context) {
                characterService.sendCharactersList()
                val response = assertIs<CharacterListResponse>(context.responseChannel.receive())
                assertTrue(response.playerCharacters.isEmpty(), "Characters list must be empty")
            }

            // Check database
            transaction {
                assertNull(PlayerCharacterEntity.findById(character.id),
                    "PlayerCharacter must be deleted")
                assertTrue(Item.findAllByOwnerId(character.id).isEmpty(),
                    "Items of deleted character must be deleted too")
                assertTrue(Shortcut.findAllByCharacterId(character.id).isEmpty(),
                    "Shortcuts of deleted character must be deleted too")
                assertTrue(LearnedSkillEntity.findAllByCharacterId(character.id).isEmpty(),
                    "Learned skills of deleted character must be deleted too")
            }
        }
    }

    @Test
    fun shouldSuccessfullyEnterGame(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter(enterGame = false)
        context.setCharacterId(character.id)

        withContext(context) { characterService.enterWorld() }

        val characterResponse = assertIs<FullCharacterResponse>(context.responseChannel.receive())
        assertEquals(character.id, characterResponse.playerCharacter.id)
        assertNotNull(gameObjectDAO.findByIdOrNull(character.id))

        assertIs<InventoryResponse>(context.responseChannel.receive())
        assertIs<ShortcutPanelResponse>(context.responseChannel.receive())

        assertIs<SystemMessageResponse>(context.responseChannel.receive())

        //TODO Other responses
    }

    @Test
    fun shouldSuccessfullyExitToCharactersMenu(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        withContext(context) { characterService.exitToCharactersMenu() }

        assertIs<RestartResponse>(context.responseChannel.receive())
        assertIs<CharacterListResponse>(context.responseChannel.receive())
        assertTrue(context.inCharacterMenu())
    }

    @Test
    fun shouldFailExitingToCharactersMenuCauseOfNotBeingInGame(): Unit = runBlocking {
        val exception = assertThrows<IllegalStateException> {
            withContext(createTestSessionContext()) { characterService.exitToCharactersMenu() }
        }
        assertEquals("Player $testLogin has not selected character", exception.message)
    }

    @Test
    fun shouldSuccessfullyExitGame(): Unit = runBlocking {
        val context = createTestSessionContext()
        val character = createTestCharacter()
        context.setCharacterId(character.id)

        //Swallow BlockingCoroutineCancelledException
        runCatching { withContext(context) { characterService.exitGame() } }

        assertIs<ExitGameResponse>(context.responseChannel.receive())
    }

    @Suppress("LongParameterList")
    private fun createCreateCharacterRequest(
        characterName: String = "Vitaliy",
        raceId: Int = 0,
        genderId: Int = 0,
        classId: Int = 0,
        hairColor: Int = 0,
        hairStyle: Int = 0,
        faceType: Int = 0
    ) = CreateCharacterRequest(
        characterName = characterName,
        raceOrdinal = raceId,
        genderOrdinal = genderId,
        classId = classId,
        hairColor = hairColor,
        hairStyle = hairStyle,
        faceType = faceType
    )

}
