package org.l2kserver.game.data

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.l2kserver.game.data.character.HUMAN_FIGHTER_CLASS
import org.l2kserver.game.data.character.HUMAN_FIGHTER_TEMPLATE
import org.l2kserver.game.data.item.armor.LEATHER_SHIELD
import org.l2kserver.game.data.item.armor.SQUIRES_PANTS
import org.l2kserver.game.data.item.armor.SQUIRES_SHIRT
import org.l2kserver.game.data.item.arrows.BONE_ARROW
import org.l2kserver.game.data.item.arrows.WOODEN_ARROW
import org.l2kserver.game.data.item.etc.ADENA
import org.l2kserver.game.data.item.weapons.BOW
import org.l2kserver.game.data.item.weapons.DAGGER
import org.l2kserver.game.data.item.weapons.DEMON_SPLINTER
import org.l2kserver.game.data.item.weapons.HEAVENS_DIVIDER
import org.l2kserver.game.data.item.weapons.SQUIRES_SWORD
import org.l2kserver.game.data.item.weapons.TALLUM_BLADE_DARK_LEGIONS_EDGE
import org.l2kserver.game.data.item.weapons.WILLOW_STAFF
import org.l2kserver.game.data.npc.GRAND_MASTER_ROIEN
import org.l2kserver.game.data.npc.GREMLIN
import org.l2kserver.game.data.skill.MORTAL_BLOW
import org.l2kserver.game.data.skill.POWER_STRIKE
import org.l2kserver.game.domain.LearnedSkillsTable
import org.l2kserver.game.domain.PlayerCharacterTable
import org.l2kserver.game.domain.Shortcut
import org.l2kserver.game.extensions.model.actor.create
import org.l2kserver.game.extensions.model.shortcut.create
import org.l2kserver.game.model.actor.AccessLevel
import org.l2kserver.game.model.actor.character.Gender
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.actor.character.CharacterClass
import org.l2kserver.game.model.actor.character.CharacterClassName
import org.l2kserver.game.model.actor.character.CharacterRace
import org.l2kserver.game.model.actor.character.CharacterTemplate
import org.l2kserver.game.model.actor.character.ShortcutType
import org.l2kserver.game.model.actor.npc.NpcTemplate
import org.l2kserver.game.model.item.ItemTemplate
import org.l2kserver.game.model.skill.SkillTemplate
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

private const val TEST_CHARACTER_ACCOUNT_NAME = "admin"
private const val TEST_CHARACTER_NAME = "TesterMan"

@Component
object TestDataLoader {

    @EventListener(ApplicationStartedEvent::class)
    fun init() {
        registerTestData()
        createTestCharacter()
    }

    /**
     * Registers some items for testing
     */
    private fun registerTestData() {
        CharacterClass.Registry.register(
            HUMAN_FIGHTER_CLASS
        )

        CharacterTemplate.Registry.register(
            HUMAN_FIGHTER_TEMPLATE
        )

        NpcTemplate.Registry.register(
            GRAND_MASTER_ROIEN,
            GREMLIN
        )

        ItemTemplate.Registry.register(
            // No Grade weapons
            WILLOW_STAFF,
            DAGGER,
            BOW,
            SQUIRES_SWORD,

            // S-Grade Weapons
            DEMON_SPLINTER,
            HEAVENS_DIVIDER,
            TALLUM_BLADE_DARK_LEGIONS_EDGE,

            // Armor
            SQUIRES_SHIRT,
            SQUIRES_PANTS,
            LEATHER_SHIELD,

            //ETC
            ADENA,

            // Arrows
            WOODEN_ARROW,
            BONE_ARROW,
        )

        SkillTemplate.Registry.register(
            POWER_STRIKE,
            MORTAL_BLOW
        )
    }

    /**
     * Creates test character
     */
    private fun createTestCharacter() = transaction {
        //TODO Refactor to return created character
        val character = PlayerCharacter.findById(PlayerCharacter.create(
            accountName = TEST_CHARACTER_ACCOUNT_NAME,
            characterName = TEST_CHARACTER_NAME,
            race = CharacterRace.HUMAN,
            gender = Gender.MALE,
            className = CharacterClassName.HUMAN_FIGHTER,
            hairColor = 1,
            hairStyle = 2,
            faceType = 3
        ))

        character.exp = 48229L // 10 lvl
        PlayerCharacterTable.update({ PlayerCharacterTable.id eq character.id }) {
            it[accessLevel] = AccessLevel.GAME_MASTER
        }

        LearnedSkillsTable.insert {
            it[characterId] = character.id
            it[subclassIndex] = 0
            it[skillId] = MORTAL_BLOW.id
            it[skillLevel] = 1
        }

        LearnedSkillsTable.insert {
            it[characterId] = character.id
            it[subclassIndex] = 0
            it[skillId] = POWER_STRIKE.id
            it[skillLevel] = 1
        }

        Shortcut.create(character.id, 0, 1, ShortcutType.SKILL, POWER_STRIKE.id, 5)
    }

}
