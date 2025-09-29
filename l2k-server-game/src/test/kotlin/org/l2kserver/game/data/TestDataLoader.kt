package org.l2kserver.game.data

import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.l2kserver.game.data.character.classes.HUMAN_FIGHTER
import org.l2kserver.game.data.character.classes.HUMAN_MYSTIC
import org.l2kserver.game.data.item.armor.APPRENTICE_STOCKINGS
import org.l2kserver.game.data.item.armor.APPRENTICE_TUNIC
import org.l2kserver.game.data.item.armor.LEATHER_SHIELD
import org.l2kserver.game.data.item.armor.SQUIRES_PANTS
import org.l2kserver.game.data.item.armor.SQUIRES_SHIRT
import org.l2kserver.game.data.item.arrows.BONE_ARROW
import org.l2kserver.game.data.item.arrows.WOODEN_ARROW
import org.l2kserver.game.data.item.book.TUTORIAL_GUIDE
import org.l2kserver.game.data.item.etc.ADENA
import org.l2kserver.game.data.item.jewelry.EARRING_OF_STRENGTH
import org.l2kserver.game.data.item.jewelry.EARRING_OF_WISDOM
import org.l2kserver.game.data.item.jewelry.NECKLACE_OF_COURAGE
import org.l2kserver.game.data.item.jewelry.RING_OF_ANGUISH
import org.l2kserver.game.data.item.jewelry.RING_OF_KNOWLEDGE
import org.l2kserver.game.data.item.soulshot.BLESSED_SPIRITSHOT_NO_GRADE
import org.l2kserver.game.data.item.soulshot.SOULSHOT_NO_GRADE
import org.l2kserver.game.data.item.soulshot.SOULSHOT_S_GRADE
import org.l2kserver.game.data.item.soulshot.SPIRITSHOT_NO_GRADE
import org.l2kserver.game.data.item.weapons.APPRENTICE_WAND
import org.l2kserver.game.data.item.weapons.BOW
import org.l2kserver.game.data.item.weapons.DAGGER
import org.l2kserver.game.data.item.weapons.DEMON_SPLINTER
import org.l2kserver.game.data.item.weapons.HEAVENS_DIVIDER
import org.l2kserver.game.data.item.weapons.SQUIRES_SWORD
import org.l2kserver.game.data.item.weapons.TALLUM_BLADE_DARK_LEGIONS_EDGE
import org.l2kserver.game.data.item.weapons.WILLOW_STAFF
import org.l2kserver.game.data.npc.GRAND_MAGISTER_GALLINT
import org.l2kserver.game.data.npc.GRAND_MASTER_ROIEN
import org.l2kserver.game.data.npc.GREMLIN
import org.l2kserver.game.data.skill.MORTAL_BLOW
import org.l2kserver.game.data.skill.POWER_SHOT
import org.l2kserver.game.data.skill.POWER_STRIKE
import org.l2kserver.game.data.skill.WIND_STRIKE
import org.l2kserver.game.domain.AccessLevel
import org.l2kserver.game.domain.PlayerCharacterTable
import org.l2kserver.game.model.actor.character.Gender
import org.l2kserver.game.model.actor.character.CharacterClass
import org.l2kserver.game.model.actor.character.CharacterRace
import org.l2kserver.game.model.actor.character.ShortcutType
import org.l2kserver.game.model.actor.npc.NpcTemplate
import org.l2kserver.game.model.html.HtmlRegistry
import org.l2kserver.game.model.item.template.ItemTemplate
import org.l2kserver.game.model.skill.SkillTemplate
import org.l2kserver.game.repository.PlayerCharacterRepository
import org.l2kserver.game.repository.ShortcutRepository
import org.l2kserver.game.repository.SkillRepository
import org.l2kserver.game.utils.LevelUtils
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

private const val TEST_CHARACTER_ACCOUNT_NAME = "admin"
private const val TEST_FIGHTER_CHARACTER_NAME = "TesterMan"
private const val TEST_MYSTIC_CHARACTER_NAME = "TesterWoman"

/**
 * Loads data for LIVE test. Don't use it for integration testing
 */
@Component
class TestDataLoader(
    private val playerCharacterRepository: PlayerCharacterRepository,
    private val skillRepository: SkillRepository,
    private val shortcutRepository: ShortcutRepository,
) {

    @EventListener(ApplicationStartedEvent::class)
    fun init() {
        registerTestData()
        createTestCharacter()
    }

    /**
     * Registers some items for testing
     */
    private fun registerTestData() {
        HtmlRegistry.loadResource("data/html/tutobook")

        NpcTemplate.Registry.register(
            GRAND_MASTER_ROIEN,
            GRAND_MAGISTER_GALLINT,
            GREMLIN
        )

        CharacterClass.Registry.register(
            HUMAN_FIGHTER,
            HUMAN_MYSTIC
        )

        ItemTemplate.Registry.register(
            TUTORIAL_GUIDE,

            // No Grade weapons
            WILLOW_STAFF,
            DAGGER,
            BOW,
            SQUIRES_SWORD,
            APPRENTICE_WAND,

            // S-Grade Weapons
            DEMON_SPLINTER,
            HEAVENS_DIVIDER,
            TALLUM_BLADE_DARK_LEGIONS_EDGE,

            // Armor
            APPRENTICE_TUNIC,
            APPRENTICE_STOCKINGS,
            SQUIRES_SHIRT,
            SQUIRES_PANTS,
            LEATHER_SHIELD,

            //Jewelry
            EARRING_OF_STRENGTH,
            EARRING_OF_WISDOM,
            RING_OF_ANGUISH,
            RING_OF_KNOWLEDGE,
            NECKLACE_OF_COURAGE,

            //ETC
            ADENA,

            // Arrows
            WOODEN_ARROW,
            BONE_ARROW,

            //Soulshots
            SOULSHOT_NO_GRADE,
            SOULSHOT_S_GRADE,

            //Spiritshots
            SPIRITSHOT_NO_GRADE,
            BLESSED_SPIRITSHOT_NO_GRADE
        )

        SkillTemplate.Registry.register(
            POWER_STRIKE,
            MORTAL_BLOW,
            POWER_SHOT,
            WIND_STRIKE
        )
    }

    /**
     * Creates test character
     */
    private fun createTestCharacter() = transaction {
        val testFighter = playerCharacterRepository.create(
            accountName = TEST_CHARACTER_ACCOUNT_NAME,
            characterName = TEST_FIGHTER_CHARACTER_NAME,
            race = CharacterRace.HUMAN,
            gender = Gender.MALE,
            classId = HUMAN_FIGHTER.id,
            hairColor = 1,
            hairStyle = 2,
            faceType = 3
        )

        testFighter.exp = LevelUtils.getRequiredExpForLevel(3)
        PlayerCharacterTable.update({ PlayerCharacterTable.id eq testFighter.id }) {
            it[accessLevel] = AccessLevel.GAME_MASTER
        }

        skillRepository.save(
            characterId = testFighter.id,
            subclassIndex = testFighter.activeSubclass,
            skillId = MORTAL_BLOW.id,
            skillLevel = 1
        )

        skillRepository.save(
            characterId = testFighter.id,
            subclassIndex = testFighter.activeSubclass,
            skillId = POWER_STRIKE.id,
            skillLevel = 1
        )

        skillRepository.save(
            characterId = testFighter.id,
            subclassIndex = testFighter.activeSubclass,
            skillId = POWER_SHOT.id,
            skillLevel = 1
        )

        shortcutRepository.create(
            testFighter.id,
            0,
            1,
            ShortcutType.SKILL,
            POWER_STRIKE.id,
            5
        )

        testFighter.currentCp = testFighter.stats.maxCp
        testFighter.currentHp = testFighter.stats.maxHp
        testFighter.currentMp = testFighter.stats.maxMp


        val testMystic = playerCharacterRepository.create(
            accountName = TEST_CHARACTER_ACCOUNT_NAME,
            characterName = TEST_MYSTIC_CHARACTER_NAME,
            race = CharacterRace.HUMAN,
            gender = Gender.FEMALE,
            classId = HUMAN_MYSTIC.id,
            hairColor = 3,
            hairStyle = 2,
            faceType = 1
        )

        PlayerCharacterTable.update({ PlayerCharacterTable.id eq testMystic.id }) {
            it[accessLevel] = AccessLevel.GAME_MASTER
        }

    }

}
