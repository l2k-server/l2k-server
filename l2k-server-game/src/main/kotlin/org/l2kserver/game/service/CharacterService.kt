package org.l2kserver.game.service

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.handler.dto.request.CreateCharacterRequest
import org.l2kserver.game.handler.dto.request.DeleteCharacterRequest
import org.l2kserver.game.handler.dto.request.RestoreCharacterRequest
import org.l2kserver.game.handler.dto.request.SelectCharacterRequest
import org.l2kserver.game.handler.dto.response.CharacterTemplatesResponse
import org.l2kserver.game.handler.dto.response.CharacterListResponse
import org.l2kserver.game.handler.dto.response.CreateCharacterFailReason
import org.l2kserver.game.handler.dto.response.CreateCharacterFailResponse
import org.l2kserver.game.handler.dto.response.DeleteCharacterFailReason
import org.l2kserver.game.handler.dto.response.DeleteCharacterFailResponse
import org.l2kserver.game.handler.dto.response.SelectCharacterResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.l2kserver.game.domain.AccessLevel
import org.l2kserver.game.handler.dto.request.ConfirmDialogAnswerRequest
import org.l2kserver.game.handler.dto.request.RespawnAt
import org.l2kserver.game.handler.dto.request.RespawnRequest
import org.l2kserver.game.handler.dto.response.ChangePostureResponse
import org.l2kserver.game.handler.dto.response.DeleteObjectResponse
import org.l2kserver.game.handler.dto.response.ExitGameResponse
import org.l2kserver.game.handler.dto.response.FullCharacterResponse
import org.l2kserver.game.handler.dto.response.InventoryResponse
import org.l2kserver.game.handler.dto.response.PlayerDiedResponse
import org.l2kserver.game.handler.dto.response.RestartResponse
import org.l2kserver.game.handler.dto.response.ReviveResponse
import org.l2kserver.game.handler.dto.response.ShortcutPanelResponse
import org.l2kserver.game.handler.dto.response.SkillListResponse
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.model.actor.PlayerCharacterInstanceImpl
import org.l2kserver.game.model.actor.character.CharacterRace
import org.l2kserver.game.model.actor.character.Gender
import org.l2kserver.game.model.map.TownRegistry
import org.l2kserver.game.network.session.SessionContext
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sessionContext
import org.l2kserver.game.repository.GameObjectRepository
import org.l2kserver.game.repository.PlayerCharacterRepository
import org.l2kserver.game.repository.ShortcutRepository
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import kotlin.math.roundToInt

private const val CHARACTERS_MAX_AMOUNT = 7
private const val CHARACTERS_INFO_UPDATE_DELAY_MS = 60_000L

@Service
class CharacterService(
    private val asyncTaskService: AsyncTaskService,
    private val actorStateService: ActorStateService,
    private val moveService: MoveService,
    private val intentionExecutorService: IntentionExecutorService,
    private val itemService: ItemService,

    private val playerCharacterRepository: PlayerCharacterRepository,
    private val shortcutRepository: ShortcutRepository,
    override val gameObjectRepository: GameObjectRepository,

    @param:Value($$"${characters.newCharacterNameRegexp}") private val newCharacterNameRegexp: String,
    @param:Value($$"${characters.deletionTimeMs}") private val characterDeletionTime: Long,
    @param:Value($$"${characters.newCharacterDeletionTimeMs}") private val newCharacterDeletionTime: Long,
    @param:Value($$"${characters.respawnCpRate}") private val respawnCpRate: Double,
    @param:Value($$"${characters.respawnHpRate}") private val respawnHpRate: Double,
    @param:Value($$"${characters.respawnMpRate}") private val respawnMpRate: Double,
): AbstractService() {

    override val log = logger()

    /**
     * Launches a job that deletes characters when their deletion time has come and
     * notifies players about deleting or deleted characters.
     *
     * If player is in characters menu and has deleting characters, CharacterListResponse
     * is sent to him every minute to update deleting time on client side and delete deleted characters
     */
    @EventListener(ApplicationReadyEvent::class)
    fun init() = asyncTaskService.launchRepeated(
        "UPDATE_CHARACTERS_INFO_JOB", CHARACTERS_INFO_UPDATE_DELAY_MS
    ) {
        val deletedPlayerCharacterOwners = playerCharacterRepository.deleteAllWithExpiredDeletionDate()
            .map { it.accountName }

        SessionContext.forEach { withContext(NonCancellable) {
            if (it.inCharacterMenu()) {
                val hasDeletingCharacters = playerCharacterRepository
                    .existDeletingByAccountName(it.getAccountName())
                val hasDeletedCharacters = deletedPlayerCharacterOwners.contains(it.getAccountName())

                if (hasDeletingCharacters || hasDeletedCharacters) sendCharactersList(it.sessionId)
            }
        }}
    }

    /**
     * Sends list of player's characters to user with session [sessionId].
     * If [sessionId] is null, sends character list to user with current SessionContext
     *
     * @param sessionId User session identifier
     */
    suspend fun sendCharactersList(sessionId: Int? = null) = suspendTransaction {
        val context = sessionId?.let { SessionContext.getById(it) } ?: sessionContext()

        try {
            log.debug { "Loading characters of user '${context.getAccountName()}'..." }
            val playerCharacters = playerCharacterRepository.findAllByAccountName(context.getAccountName())

            //Send characters list to
            send {
                CharacterListResponse(
                    gameSessionKey1 = context.getAuthorizationKey().gameSessionKey1,
                    accountName = context.getAccountName(),
                    playerCharacters = playerCharacters
                )
            }
        } catch (e: Exception) {
            log.error(e) { "Error occurred while getting characters of user ${context.getAccountName()}" }
        }
    }

    suspend fun getCharacterTemplates() = send { CharacterTemplatesResponse }

    /**
     * Creates character using info got in [request]
     */
    suspend fun createCharacter(request: CreateCharacterRequest) = suspendTransaction {
        val accountName = sessionContext().getAccountName()

        try {
            if (playerCharacterRepository.countByAccountName(accountName) >= CHARACTERS_MAX_AMOUNT)
                send { CreateCharacterFailResponse(CreateCharacterFailReason.TOO_MANY_CHARACTERS) }
            else if (playerCharacterRepository.existsByName(request.characterName))
                send { CreateCharacterFailResponse(CreateCharacterFailReason.NAME_ALREADY_EXISTS) }
            else if (!request.characterName.matches(Regex(newCharacterNameRegexp)))
                send { CreateCharacterFailResponse(CreateCharacterFailReason.NAME_EXCEED_16_CHARACTERS) }
            else {
                playerCharacterRepository.create(
                    accountName = accountName,
                    characterName = request.characterName,
                    race = CharacterRace.entries.getOrElse(request.raceId) {
                        throw IllegalArgumentException("Invalid race ordinal '${request.raceId}")
                    },
                    gender = Gender.entries.getOrElse(request.genderId) {
                        throw IllegalArgumentException("Invalid gender ordinal '${request.genderId}")
                    },
                    classId = request.classId,
                    hairColor = request.hairColor,
                    hairStyle = request.hairStyle,
                    faceType = request.faceType
                )
                commit()

                sendCharactersList()
            }
        } catch (e: Exception) {
            log.error(e) {
                "Error occurred while creating character '${request.characterName}' at the account '$accountName'"
            }

            send { CreateCharacterFailResponse(CreateCharacterFailReason.CREATION_FAILED) }
        }
    }

    /**
     * Deletes character at selected slot
     */
    suspend fun deleteCharacter(request: DeleteCharacterRequest) {
        val accountName = sessionContext().getAccountName()

        log.debug { "Deleting character at slot '${request.characterSlot}' of user '$accountName'..." }

        suspendTransaction {
            val playerCharacter = playerCharacterRepository.findAllByAccountName(accountName)
                .getOrNull(request.characterSlot)

            if (playerCharacter != null) {
                //TODO Checks - cannot delete clan leader
                if (playerCharacter.clanId != 0) {
                    log.debug { "Cannot delete clan member" }
                    send { DeleteCharacterFailResponse(DeleteCharacterFailReason.YOU_MAY_NOT_DELETE_CLAN_MEMBER) }
                } else {
                    val deletionDate = if (playerCharacter.level > 10)
                        LocalDateTime.now().plus(characterDeletionTime, ChronoUnit.MILLIS)
                    else LocalDateTime.now().plus(newCharacterDeletionTime, ChronoUnit.MILLIS)

                    playerCharacter.deletionDate = deletionDate

                    log.info { "'$playerCharacter' was assigned for deletion at '$deletionDate'" }
                }
            } else {
                log.debug { "Cannot delete character, because character " +
                    "slot ${request.characterSlot} of the account $accountName is empty" }
                send { DeleteCharacterFailResponse(DeleteCharacterFailReason.DELETION_FAILED) }
            }
        }

        sendCharactersList()
    }

    /**
     * Cancels deletion of character at selected slot
     */
    suspend fun restoreCharacter(request: RestoreCharacterRequest) {
        suspendTransaction {
            val accountName = sessionContext().getAccountName()

            log.debug { "Restoring character at slot '${request.characterSlot}' of user '$accountName'" }

            val playerCharacter = playerCharacterRepository.findAllByAccountName(accountName)
                .getOrNull(request.characterSlot)

            if (playerCharacter?.deletionDate == null)
                log.warn { "Got restoreCharacterRequest for non-existing or not assigned for deletion character" }
            else {
                playerCharacter.deletionDate = null
                log.info { "User $accountName has restored character $playerCharacter" }
            }
        }

        sendCharactersList()
    }

    /**
     * Select character to enter game with
     */
    suspend fun selectCharacter(request: SelectCharacterRequest) = suspendTransaction {
        val context = sessionContext()
        val accountName = context.getAccountName()

        log.debug { "Player $accountName is trying to select character at slot ${request.characterSlot}" }

        check(context.inCharacterMenu()) { "Player $accountName cannot enter the game" }

        val selectedPlayerCharacter = requireNotNull(
            playerCharacterRepository.findAllByAccountName(accountName).getOrNull(request.characterSlot)
        ) {
            "Character slot ${request.characterSlot} of the account $accountName is empty!"
        }

        context.setCharacterId(selectedPlayerCharacter.id)
        selectedPlayerCharacter.lastAccess = LocalDateTime.now()

        log.debug { "Player $accountName has successfully selected character $selectedPlayerCharacter" }

        send { SelectCharacterResponse(context.getAuthorizationKey(), selectedPlayerCharacter) }
    }

    /**
     * Enters game world with selected character
     *
     * For some Korean reasons server must get EnterWorldRequest after
     * character selection instead of entering world immediately ¯\_(ツ)_/¯
     */
    suspend fun enterWorld() = suspendTransaction {
        val context = sessionContext()
        val accountName = context.getAccountName()
        val characterId = context.getCharacterId()

        check(gameObjectRepository.findByIdOrNull(characterId) == null) { "Player $accountName is already in game" }

        log.debug { "User $accountName is entering game world with character id=$characterId..." }

        val character = requireNotNull(playerCharacterRepository.findById(characterId)) {
            "Cannot enter game: no character with id $characterId exists!"
        }

        gameObjectRepository.save(character)
        intentionExecutorService.launchIntentionQueueListener(character)

        val shortcuts = shortcutRepository.findAllBy(character.id, character.activeSubclass)

        send { FullCharacterResponse(character) }
        send { InventoryResponse(character.inventory) }
        send { SkillListResponse(character.skillsAndMagic) }
        send { ShortcutPanelResponse(shortcuts) }
        send { SystemMessageResponse.Welcome }

        if (character.isDead()) send { PlayerDiedResponse(character) }

        updateObjectsAround(character)
        log.info { "Player $accountName has entered world with character $character" }
    }

    suspend fun sendCharacterInfo() = send {
        FullCharacterResponse(gameObjectRepository.findCharacterById(sessionContext().getCharacterId()))
    }

    suspend fun respawnCharacter(request: RespawnRequest) {
        val context = sessionContext()
        val character = gameObjectRepository.findCharacterById(context.getCharacterId())

        log.debug { "Start respawning '$character'" }

        val respawnPosition = when (request.respawnAt) {
            //TODO During a siege, character should be teleported to other town
            RespawnAt.VILLAGE -> TownRegistry.getRandomSpawnPointByPosition(
                character.position,
                isOutlaw = character.karma > 0
            )
            RespawnAt.CLAN_HALL -> TODO()
            RespawnAt.CASTLE -> TODO()
            RespawnAt.SIEGE_HEADQUARTERS -> TODO()
            RespawnAt.FIXED -> {
                //TODO players can respawn at Fixed position if they are festival participants
                require(character.accessLevel == AccessLevel.GAME_MASTER) { "This action is available only for GM!" }
                character.position
            }
            RespawnAt.JAIL -> TODO()
        }

        suspendTransaction {
            character.expLostAfterDeath = 0
            character.expRestoredByResurrection = 0
        }

        moveService.teleport(character, respawnPosition)
        reviveCharacter(character)
    }

    suspend fun resurrectCharacter(request: ConfirmDialogAnswerRequest.Resurrection) = suspendTransaction {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        log.debug { "$character has ${if (request.confirmed) "confirmed" else "declined"} resurrection request" }

        val expToRestore = character.expRestoredByResurrection ?: run {
            log.warn { "Cannot confirm resurrection - $character is not resurrected!!!" }
            return@suspendTransaction
        }

        if (request.confirmed) {
            character.exp += expToRestore
            reviveCharacter(character)
        }

        character.expLostAfterDeath = 0
        character.expRestoredByResurrection = null
    }

    suspend fun exitToCharactersMenu() {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())

        log.debug { "'$character' is exiting to characters menu" }

        if (exitWorld()) {
            send { RestartResponse }
            sendCharactersList()

            log.info { "'$character' has quit to characters menu" }
        }
        else log.debug { "'$character' cannot quit to characters menu" }
    }

    suspend fun exitGame() {
        val context = sessionContext()
        val accountName = context.getAccountName()

        val character = context.getCharacterIdOrNull()?.let { gameObjectRepository.findCharacterByIdOrNull(it) }
        character?.let { if (exitWorld()) send { ExitGameResponse } }

        log.info { "Player $accountName has quit the game" }
    }

    /**
     * Removes character from game world and stop all the related jobs. Broadcasts deleteResponse
     * @param forced - do not check if player can exit game
     *
     * @return `true` if character has exited game world (or if it even was not in game), `false` if not
     */
    suspend fun exitWorld(forced: Boolean = false): Boolean = sessionContext().getCharacterIdOrNull()?.let {
        gameObjectRepository.findCharacterByIdOrNull(it)?.let { character ->
            //TODO Other checks if player cannot leave game
            if (!forced && character.isFighting) {
                send { SystemMessageResponse.CannotRestartInCombat }
                return false
            }

            itemService.clearEnchantSession(character)
            asyncTaskService.cancelActionByActorId(character.id)
            intentionExecutorService.disableIntentionQueueListener(character.id)
            actorStateService.stopUpdatingStates(character)
            gameObjectRepository.deleteById(character.id)

            broadcastAround(character) { DeleteObjectResponse(character.id) }
            //TODO Notify friends, clan, wife, favourite cat, etc.
        }
        sessionContext().setCharacterId(null)
        return true
    } ?: true

    /**
     * Respawns [character] - restores character's cp, hp and mp and revives him
     */
    private suspend fun reviveCharacter(character: PlayerCharacterInstanceImpl) {
        broadcastAround(character.position) { ReviveResponse(character.id) }

        suspendTransaction {
            character.currentCp = (character.stats.maxCp * respawnCpRate).roundToInt()
            character.currentHp = (character.stats.maxHp * respawnHpRate).roundToInt()
            character.currentMp = (character.stats.maxMp * respawnMpRate).roundToInt()

            character.privateStore = null
            character.standUp()

            broadcastAround(character.position) { UpdateStatusResponse.currentHpMpCpOf(character) }
            send { FullCharacterResponse(character) }
            send { ChangePostureResponse(character.id, character.position, character.posture) }
        }
    }

}
