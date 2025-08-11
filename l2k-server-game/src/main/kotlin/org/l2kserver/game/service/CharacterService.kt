package org.l2kserver.game.service

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.cancel
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.l2kserver.game.domain.AccessLevel
import org.l2kserver.game.model.map.Town
import org.l2kserver.game.domain.Shortcut
import org.l2kserver.game.extensions.model.actor.toInfoResponse
import org.l2kserver.game.extensions.model.item.findAllByOwnerId
import org.l2kserver.game.extensions.model.shortcut.findAllBy
import org.l2kserver.game.extensions.model.store.toMessageResponse
import org.l2kserver.game.handler.dto.request.RespawnAt
import org.l2kserver.game.handler.dto.request.RespawnRequest
import org.l2kserver.game.handler.dto.response.ChangePostureResponse
import org.l2kserver.game.handler.dto.response.CharacterInfoResponse
import org.l2kserver.game.handler.dto.response.DeleteObjectResponse
import org.l2kserver.game.handler.dto.response.ExitGameResponse
import org.l2kserver.game.handler.dto.response.FullCharacterResponse
import org.l2kserver.game.handler.dto.response.InventoryResponse
import org.l2kserver.game.handler.dto.response.PlayerDiedResponse
import org.l2kserver.game.handler.dto.response.PvPStatusResponse
import org.l2kserver.game.handler.dto.response.RestartResponse
import org.l2kserver.game.handler.dto.response.ReviveResponse
import org.l2kserver.game.handler.dto.response.ShortcutPanelResponse
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.item.Item
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.character.CharacterRace
import org.l2kserver.game.model.actor.character.Gender
import org.l2kserver.game.network.session.SessionContext
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sendTo
import org.l2kserver.game.network.session.sessionContext
import org.l2kserver.game.repository.GameObjectRepository
import org.l2kserver.game.repository.PlayerCharacterRepository
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

private const val CHARACTERS_MAX_AMOUNT = 7
private const val CHARACTERS_INFO_UPDATE_DELAY = 60_000L

@Service
class CharacterService(
    private val asyncTaskService: AsyncTaskService,
    private val actorStateService: ActorStateService,
    private val moveService: MoveService,

    private val playerCharacterRepository: PlayerCharacterRepository,
    override val gameObjectRepository: GameObjectRepository,

    @Value("\${characters.newCharacterNameRegexp}") private val newCharacterNameRegexp: String,
    @Value("\${characters.deletionTimeMs}") private val characterDeletionTime: Long,
    @Value("\${characters.newCharacterDeletionTimeMs}") private val newCharacterDeletionTime: Long,
    @Value("\${characters.respawnCpRate}") private val respawnCpRate: Double,
    @Value("\${characters.respawnHpRate}") private val respawnHpRate: Double,
    @Value("\${characters.respawnMpRate}") private val respawnMpRate: Double,
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
    fun init() = asyncTaskService.launchJob("UPDATE_CHARACTERS_INFO_JOB") {
        while (isActive) {
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

            delay(CHARACTERS_INFO_UPDATE_DELAY)
        }
    }

    /**
     * Sends list of player's characters to user with session [sessionId].
     * If [sessionId] is null, sends character list to user with current SessionContext
     *
     * @param sessionId User session identifier
     */
    suspend fun sendCharactersList(sessionId: Int? = null) = newSuspendedTransaction {
        val context = sessionId?.let { SessionContext.getById(it) } ?: sessionContext()

        try {
            log.debug("Loading characters of user '{}'...", context.getAccountName())
            val playerCharacters = playerCharacterRepository.findAllByAccountName(context.getAccountName())

            //Send characters list to
            send(CharacterListResponse(
                gameSessionKey1 = context.getAuthorizationKey().gameSessionKey1,
                accountName = context.getAccountName(),
                playerCharacters = playerCharacters
            ))
        } catch (e: Exception) {
            log.error("Error occurred while getting characters of user ${context.getAccountName()}", e)
        }
    }

    suspend fun getCharacterTemplates() {
        send(CharacterTemplatesResponse)
    }

    /**
     * Creates character using info got in [request]
     */
    suspend fun createCharacter(request: CreateCharacterRequest) = newSuspendedTransaction {
        val accountName = sessionContext().getAccountName()

        try {
            if (playerCharacterRepository.countByAccountName(accountName) >= CHARACTERS_MAX_AMOUNT)
                send(CreateCharacterFailResponse(CreateCharacterFailReason.TOO_MANY_CHARACTERS))
            else if (playerCharacterRepository.existsByName(request.characterName))
                send(CreateCharacterFailResponse(CreateCharacterFailReason.NAME_ALREADY_EXISTS))
            else if (!request.characterName.matches(Regex(newCharacterNameRegexp)))
                send(CreateCharacterFailResponse(CreateCharacterFailReason.NAME_EXCEED_16_CHARACTERS))
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
            log.error(
                "Error occurred while creating character '{}' at the account '{}'",
                request.characterName, accountName, e
            )

            send(CreateCharacterFailResponse(CreateCharacterFailReason.CREATION_FAILED))
        }
    }

    /**
     * Deletes character at selected slot
     */
    suspend fun deleteCharacter(request: DeleteCharacterRequest) {
        val accountName = sessionContext().getAccountName()

        log.debug("Deleting character at slot '{}' of user '{}'...", request.characterSlot, accountName)

        newSuspendedTransaction {
            val playerCharacter = playerCharacterRepository.findAllByAccountName(accountName)
                .getOrNull(request.characterSlot)

            if (playerCharacter != null) {
                //TODO Checks - cannot delete clan leader
                if (playerCharacter.clanId != 0) {
                    log.debug("Cannot delete clan member")
                    send(DeleteCharacterFailResponse(DeleteCharacterFailReason.YOU_MAY_NOT_DELETE_CLAN_MEMBER))
                } else {
                    val deletionDate = if (playerCharacter.level > 10)
                        LocalDateTime.now().plus(characterDeletionTime, ChronoUnit.MILLIS)
                    else LocalDateTime.now().plus(newCharacterDeletionTime, ChronoUnit.MILLIS)

                    playerCharacter.deletionDate = deletionDate

                    log.info("Character '{}' was assigned for deletion at '{}'", playerCharacter.name, deletionDate)
                }
            } else {
                log.debug(
                    "Cannot delete character, because character slot {} of the account {} is empty",
                    request.characterSlot, accountName
                )
                send(DeleteCharacterFailResponse(DeleteCharacterFailReason.DELETION_FAILED))
            }
        }

        sendCharactersList()
    }

    /**
     * Cancels deletion of character at selected slot
     */
    suspend fun restoreCharacter(request: RestoreCharacterRequest) {
        newSuspendedTransaction {
            val accountName = sessionContext().getAccountName()

            log.debug("Restoring character at slot '{}' of user '{}'", request.characterSlot, accountName)

            val playerCharacter = playerCharacterRepository.findAllByAccountName(accountName)
                .getOrNull(request.characterSlot)

            if (playerCharacter?.deletionDate == null)
                log.warn("Got restoreCharacterRequest for non-existing or not assigned for deletion character")
            else {
                playerCharacter.deletionDate = null
                log.info("User {} has restored character {}", accountName, playerCharacter.name)
            }
        }

        sendCharactersList()
    }

    /**
     * Select character to enter game with
     */
    suspend fun selectCharacter(request: SelectCharacterRequest) = newSuspendedTransaction {
        val context = sessionContext()
        val accountName = context.getAccountName()

        log.debug("Player {} is trying to select character at slot {}", accountName, request.characterSlot)

        check(context.inCharacterMenu()) { "Player $accountName cannot enter the game" }

        val selectedPlayerCharacter = requireNotNull(
            playerCharacterRepository.findAllByAccountName(accountName).getOrNull(request.characterSlot)
        ) {
            "Character slot ${request.characterSlot} of the account $accountName is empty!"
        }

        context.setCharacterId(selectedPlayerCharacter.id)
        selectedPlayerCharacter.lastAccess = LocalDateTime.now()

        log.debug("Player {} has successfully selected character {}", accountName, selectedPlayerCharacter.name)

        send(SelectCharacterResponse(context.getAuthorizationKey(), selectedPlayerCharacter))
    }

    /**
     * Enters game world with selected character
     *
     * For some Korean reasons server must get EnterWorldRequest after
     * character selection instead of entering world immediately ¯\_(ツ)_/¯
     */
    suspend fun enterWorld() = newSuspendedTransaction {
        val context = sessionContext()
        val accountName = context.getAccountName()
        val characterId = context.getCharacterId()

        check(gameObjectRepository.findByIdOrNull(characterId) == null) { "Player $accountName is already in game" }

        log.debug("User {} is entering game world with character id={}...", accountName, characterId)

        val character = requireNotNull(playerCharacterRepository.findById(characterId)) {
            "Cannot enter game: no character with id $characterId exists!"
        }
        gameObjectRepository.loadCharacter(character)
        val items = Item.findAllByOwnerId(character.id)
        val shortcuts = Shortcut.findAllBy(character.id, character.activeSubclass)

        send(FullCharacterResponse(character))
        send(InventoryResponse(items))
        send(ShortcutPanelResponse(shortcuts))
        send(SystemMessageResponse.Welcome)

        if (character.isDead()) send(PlayerDiedResponse(character))

        gameObjectRepository.findAllNear(character).forEach {
            send(it.toInfoResponse())

            if (it is PlayerCharacter) {
                sendTo(it.id, CharacterInfoResponse(character))
                it.privateStore?.let { store -> send(store.toMessageResponse(it.id)) }

                if (it.isEnemyOf(character)) {
                    sendTo(it.id, PvPStatusResponse(character))
                    send(PvPStatusResponse(it))
                }
            }
        }

        log.info("Player {} has entered world with character {}", accountName, character.name)
    }

    suspend fun respawnCharacter(request: RespawnRequest) {
        val context = sessionContext()
        val character = gameObjectRepository.findCharacterById(context.getCharacterId())

        log.debug("Start respawning '{}'", character)

        //TODO If no town found, teleport to 147451, 27014, -2205 (Center of Aden)
        val respawnPosition = when (request.respawnAt) {
            //TODO During a siege, character should be teleported to other town... or not?
            RespawnAt.VILLAGE -> Town.Registry.getRandomSpawnPointByPosition(
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

        respawnCharacterAt(character, respawnPosition)
    }

    suspend fun exitToCharactersMenu() {
        val context = sessionContext()
        val accountName = context.getAccountName()

        log.debug("Player {} is exiting to characters menu", accountName)

        val character = gameObjectRepository.findCharacterById(context.getCharacterId())

        if (character.canExitWorld()) {
            removeFromGameWorld(character)

            context.setCharacterId(null)
            send(RestartResponse)
            sendCharactersList()

            log.info("Player {} has quit to characters menu", accountName)
        }
        else {
            log.debug("Player {} cannot quit to characters menu", accountName)
        }
    }

    suspend fun exitGame() {
        val context = sessionContext()
        val accountName = context.getAccountName()

        log.debug("Player {} is exiting game", accountName)

        val character = gameObjectRepository.findCharacterById(context.getCharacterId())

        if (character.canExitWorld()) {
            send(ExitGameResponse)
            //TODO On windows coroutine is cancelled too quickly and ExitGameResponse isn't sent
            // https://github.com/orgs/l2kserver/projects/1/views/3?pane=issue&itemId=98027933
            coroutineContext.cancel()
            log.info("Player {} has quit game", context.getAccountName())
        }
    }

    suspend fun disconnectGame() {
        sessionContext().getCharacterIdOrNull()?.let { characterId ->
            gameObjectRepository.findCharacterByIdOrNull(characterId)?.let { removeFromGameWorld(it) }
        }
    }

    /**
     * Removes [character] from game world and stop all the related jobs
     */
    suspend fun removeFromGameWorld(character: PlayerCharacter) {
        broadcastPacket(DeleteObjectResponse(character.id), character)
        actorStateService.stopUpdatingStates(character)
        asyncTaskService.cancelActionByActorId(character.id)
        gameObjectRepository.deleteById(character.id)
    }

    /**
     * Checks if player can exit game world
     */
    private suspend fun PlayerCharacter.canExitWorld(): Boolean {
        //TODO Other checks if player cannot leave game
        if (this.isFighting) {
            send(SystemMessageResponse.CannotRestartInCombat)
            return false
        }

        return true
    }

    /**
     * Respawns [character] at provided [position] - teleports character at position,
     * restores character's cp, hp and mp and revives him
     */
    private suspend fun respawnCharacterAt(character: PlayerCharacter, position: Position) {
        moveService.teleport(character, position)
        broadcastPacket(ReviveResponse(character.id), character.position)

        newSuspendedTransaction {
            character.currentCp = (character.stats.maxCp * respawnCpRate).roundToInt()
            character.currentHp = (character.stats.maxHp * respawnHpRate).roundToInt()
            character.currentMp = (character.stats.maxMp * respawnMpRate).roundToInt()

            character.privateStore = null
            character.standUp()

            broadcastPacket(UpdateStatusResponse.hpMpCpOf(character), character.position)
            send(FullCharacterResponse(character))
            send(ChangePostureResponse(character.id, character.position, character.posture))
        }
    }

}
