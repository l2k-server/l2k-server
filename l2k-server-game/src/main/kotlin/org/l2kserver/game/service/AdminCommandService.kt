package org.l2kserver.game.service

import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.l2kserver.game.domain.AccessLevel
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.handler.dto.request.AdminCommandRequest
import org.l2kserver.game.handler.dto.response.PlaySoundResponse
import org.l2kserver.game.handler.dto.response.Sound
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.handler.dto.response.UpdateItemsResponse
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sessionContext
import org.l2kserver.game.repository.GameObjectRepository
import org.l2kserver.game.model.command.AdminCommand
import org.l2kserver.game.model.command.CommandDescription
import org.l2kserver.game.model.command.EnchantCommand
import org.l2kserver.game.model.command.GiveCommand
import org.l2kserver.game.model.command.HelpCommand
import org.l2kserver.game.model.command.ItemToEnchant
import org.l2kserver.game.model.command.LearnCommand
import org.l2kserver.game.model.command.RestoreCommand
import org.l2kserver.game.model.command.StatToRestore
import org.l2kserver.game.model.command.TeleportCommand
import org.l2kserver.game.network.session.sendTo
import org.springframework.stereotype.Service

/** Service for handling admin commands */
@Service
class AdminCommandService(
    override val gameObjectRepository: GameObjectRepository,
    private val moveService: MoveService,
    private val itemService: ItemService,
    private val skillService: SkillService
) : AbstractService() {

    override val log = logger()

    suspend fun handleAdminCommand(commandRequest: AdminCommandRequest) {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        if (character.accessLevel != AccessLevel.GAME_MASTER) {
            log.warn("Player '{}' has no privileges to use admin commands!", character)
            return
        }

        try {
            val adminCommand = AdminCommand.parse(commandRequest.commandString)
            when (adminCommand) {
                is HelpCommand -> handleHelpCommand()
                is TeleportCommand -> handleTeleportCommand(adminCommand)
                is EnchantCommand -> handleEnchantCommand(adminCommand)
                is GiveCommand -> handleGiveCommand(adminCommand)
                is RestoreCommand -> handleRestoreCommand(adminCommand)
                is LearnCommand -> handleLearnCommand(adminCommand)
            }
        } catch (e: Exception) {
            log.error("Failed executing command '{}'", commandRequest.commandString, e)
            send {
                SystemMessageResponse(
                    "Failed executing command '${commandRequest.commandString}' - ${e.message}"
                )
            }
        }
    }

    /**
     * Handles help command. Iterates through all the CommandDescription object instances and sends their manuals
     */
    private suspend fun handleHelpCommand() {
        send { SystemMessageResponse("List of available commands:") }
        CommandDescription::class.sealedSubclasses.forEach {
            it.objectInstance?.let { description -> send { SystemMessageResponse(description.manual) } }
        }
    }

    /**
     * Handles teleport command.
     * Teleports character with [TeleportCommand.name] to requested [TeleportCommand.position].
     * If [TeleportCommand.name] is null, character who called this command will be teleported
     */
    private suspend fun handleTeleportCommand(command: TeleportCommand) {
        val characterToTeleport = command.name?.let { gameObjectRepository.findCharacterByName(it) }
            ?: gameObjectRepository.findCharacterById(sessionContext().getCharacterId())

        send { SystemMessageResponse("'${characterToTeleport.name}' was teleported to '${command.position}'") }
        moveService.teleport(characterToTeleport, command.position)
    }

    /**
     * Handles enchant command.
     * Enchants [EnchantCommand.itemToEnchant] equipped by player with [EnchantCommand.characterName] or session owner,
     * if no [EnchantCommand.characterName] was provided, by [EnchantCommand.enchantLevel]
     */
    private suspend fun handleEnchantCommand(command: EnchantCommand) {
        val characterToEnchant = command.characterName
            ?.let { gameObjectRepository.findCharacterByName(it) }
            ?: gameObjectRepository.findCharacterById(sessionContext().getCharacterId())

        log.debug(
            "Got command to enchant '{}' of '{}' by '{}'",
            command.itemToEnchant,
            characterToEnchant,
            command.enchantLevel
        )

        val item = when (command.itemToEnchant) {
            ItemToEnchant.UNDERWEAR -> characterToEnchant.inventory.underwear
            ItemToEnchant.RIGHT_EARRING -> characterToEnchant.inventory.rightEarring
            ItemToEnchant.LEFT_EARRING -> characterToEnchant.inventory.leftEarring
            ItemToEnchant.NECKLACE -> characterToEnchant.inventory.necklace
            ItemToEnchant.RIGHT_RING -> characterToEnchant.inventory.rightRing
            ItemToEnchant.LEFT_RING -> characterToEnchant.inventory.leftRing
            ItemToEnchant.HEADGEAR -> characterToEnchant.inventory.headgear
            ItemToEnchant.WEAPON -> characterToEnchant.inventory.weapon
            ItemToEnchant.SHIELD -> characterToEnchant.inventory.shield
            ItemToEnchant.GLOVES -> characterToEnchant.inventory.gloves
            ItemToEnchant.UPPER_BODY -> characterToEnchant.inventory.upperBody
            ItemToEnchant.LOWER_BODY -> characterToEnchant.inventory.lowerBody
            ItemToEnchant.BOOTS -> characterToEnchant.inventory.boots
        }

        if (item == null) {
            send {
                SystemMessageResponse(
                    "Player ${characterToEnchant.name} has " +
                            "no equipped ${command.itemToEnchant.name.lowercase().replace('_', ' ')}"
                )
            }
            send { PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE) }
            return
        }

        suspendTransaction {
            item.enchantLevel = command.enchantLevel
            send { SystemMessageResponse.YourItemHasBeenSuccessfullyEnchanted(item) }
            send { UpdateItemsResponse().wasModified(item) }
            broadcastActorInfo(characterToEnchant)
        }
    }

    /**
     * Handles enchant command.
     * Gives [GiveCommand.amount] items with [GiveCommand.templateId]
     * to [GiveCommand.name] or session owner, if no [GiveCommand.name] was provided,
     * enchanted by [GiveCommand.enchantLevel]
     */
    private suspend fun handleGiveCommand(command: GiveCommand) {
        val character = command.name?.let { gameObjectRepository.findCharacterByName(it) }
            ?: gameObjectRepository.findCharacterById(sessionContext().getCharacterId())

        itemService.giveItem(
            character,
            command.templateId,
            command.amount,
            enchantLevel = command.enchantLevel,
        )
    }

    /**
     * Handles restore command. Restores [RestoreCommand.statsToRestore]
     * of character with [RestoreCommand.name]
     * If [RestoreCommand.name] is null, restore stats of character who called this command
     */
    private suspend fun handleRestoreCommand(command: RestoreCommand) = suspendTransaction {
        val healer = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        val character = command.name?.let { gameObjectRepository.findCharacterByName(it) } ?: healer

        val stats = command.statsToRestore ?: StatToRestore.entries
        val healerName = if (healer != character) healer.name else null

        stats.forEach {
            when (it) {
                StatToRestore.CP -> {
                    sendTo(character.id) {
                        SystemMessageResponse.CpRestored(character.stats.maxCp - character.currentCp, healerName)
                    }
                    character.currentCp = character.stats.maxCp
                }
                StatToRestore.HP -> {
                    sendTo(character.id) {
                        SystemMessageResponse.HpRestored(character.stats.maxHp - character.currentHp, healerName)
                    }
                    character.currentHp = character.stats.maxHp
                }
                StatToRestore.MP -> {
                    sendTo(character.id) {
                        SystemMessageResponse.MpRestored(character.stats.maxMp - character.currentMp, healerName)
                    }
                    character.currentMp = character.stats.maxMp
                }
            }
        }

        send { UpdateStatusResponse.hpMpCpOf(character) }
    }

    private suspend fun handleLearnCommand(command: LearnCommand) {
        val character = command.name?.let { gameObjectRepository.findCharacterByName(it) }
            ?: gameObjectRepository.findCharacterById(sessionContext().getCharacterId())

        skillService.learnSkill(character, command.skillId, command.level)
    }


}
