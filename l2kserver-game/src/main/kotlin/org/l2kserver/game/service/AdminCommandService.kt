package org.l2kserver.game.service

import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.l2kserver.game.domain.AccessLevel
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.handler.dto.request.AdminCommandRequest
import org.l2kserver.game.handler.dto.response.PlaySoundResponse
import org.l2kserver.game.handler.dto.response.Sound
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.handler.dto.response.UpdateItemsResponse
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sessionContext
import org.l2kserver.game.repository.GameObjectRepository
import org.l2kserver.game.model.command.Command
import org.l2kserver.game.model.command.CommandDescription
import org.l2kserver.game.model.command.EnchantCommand
import org.l2kserver.game.model.command.HelpCommand
import org.l2kserver.game.model.command.ItemToEnchant
import org.l2kserver.game.model.command.TeleportCommand
import org.springframework.stereotype.Service

/**
 * Service for handling admin commands
 */
@Service
class AdminCommandService(
    override val gameObjectRepository: GameObjectRepository,
    private val moveService: MoveService
): AbstractService() {

    override val log = logger()

    suspend fun handleAdminCommand(commandRequest: AdminCommandRequest) {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        if (character.accessLevel != AccessLevel.GAME_MASTER) {
            log.warn("Player '{}' has no privileges to use admin commands!", character)
            return
        }

        val command = Command.parse(commandRequest.commandString).getOrElse { e ->
            log.error("Failed executing command '{}'", commandRequest.commandString, e)
            send(SystemMessageResponse("Failed executing command '${commandRequest.commandString}' - ${e.message}"))

            return
        }

        when (command) {
            is HelpCommand -> handleHelpCommand()
            is TeleportCommand -> handleTeleportCommand(command.name, command.position)
            is EnchantCommand -> handleEnchantCommand(command.characterName, command.itemToEnchant, command.enchantLevel)
        }
    }

    /**
     * Handles help command. Iterates through all the CommandDescription object instances and sends their manuals
     */
    private suspend fun handleHelpCommand() {
        send(SystemMessageResponse("List of available commands:"))
        CommandDescription::class.sealedSubclasses.forEach {
            it.objectInstance?.let { description -> send(SystemMessageResponse(description.manual)) }
        }
    }

    /**
     * Handles teleport command. Teleports character with [charName] to requested [position].
     *
     * @param charName Name of character, that should be teleported.
     * If null, character who called this command will be teleported
     * @param position Destination position
     */
    private suspend fun handleTeleportCommand(charName: String?, position: Position) {
        val characterToTeleport = charName?.let { gameObjectRepository.findCharacterByName(it) }
            ?: gameObjectRepository.findCharacterById(sessionContext().getCharacterId())

        send(SystemMessageResponse("'${characterToTeleport.name}' was teleported to '$position'"))
        moveService.teleport(characterToTeleport, position)
    }

    /**
     * Handles enchant command.
     * Enchants [itemToEnchant] equipped by player with [charName] or session owner,
     * if no [charName] was provided by [enchantLevel]
     *
     * @param charName Name of character, that should be teleported.
     * If null, character who called this command will be teleported
     * @param itemToEnchant Item to enchant, equipped by [charName] or session owner
     * @param enchantLevel Requested enchant level
     */
    private suspend fun handleEnchantCommand(
        charName: String?, itemToEnchant: ItemToEnchant, enchantLevel: Int
    ) {
        val characterToEnchant = charName?.let { gameObjectRepository.findCharacterByName(it) }
            ?: gameObjectRepository.findCharacterById(sessionContext().getCharacterId())

        log.debug("Got command to enchant '{}' of '{}' by '{}'", itemToEnchant, characterToEnchant, enchantLevel)

        val item = when(itemToEnchant) {
            ItemToEnchant.UNDERWEAR -> characterToEnchant.paperDoll.underwear
            ItemToEnchant.RIGHT_EARRING -> characterToEnchant.paperDoll.rightEarring
            ItemToEnchant.LEFT_EARRING -> characterToEnchant.paperDoll.leftEarring
            ItemToEnchant.NECKLACE -> characterToEnchant.paperDoll.necklace
            ItemToEnchant.RIGHT_RING -> characterToEnchant.paperDoll.rightRing
            ItemToEnchant.LEFT_RING -> characterToEnchant.paperDoll.leftRing
            ItemToEnchant.HEADGEAR -> characterToEnchant.paperDoll.headgear
            ItemToEnchant.WEAPON -> characterToEnchant.paperDoll.getWeapon()
            ItemToEnchant.SHIELD -> characterToEnchant.paperDoll.shield
            ItemToEnchant.GLOVES -> characterToEnchant.paperDoll.gloves
            ItemToEnchant.UPPER_BODY -> characterToEnchant.paperDoll.upperBody
            ItemToEnchant.LOWER_BODY -> characterToEnchant.paperDoll.lowerBody
            ItemToEnchant.BOOTS -> characterToEnchant.paperDoll.boots
        }

        if (item == null) {
            send(
                SystemMessageResponse(
                    "Player ${characterToEnchant.name} has no equipped ${itemToEnchant.name.lowercase().replace('_', ' ')}"
                ),
                PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE)
            )
            return
        }

        newSuspendedTransaction {
            item.enchantLevel = enchantLevel

            send(SystemMessageResponse.YourItemHasBeenSuccessfullyEnchanted(item))
            send(SystemMessageResponse(item.stats.toString()))
            send(UpdateItemsResponse.operationModify(item))
            broadcastActorInfo(characterToEnchant)
        }
    }

}
