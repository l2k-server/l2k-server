package org.l2kserver.game.data

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.l2kserver.game.data.character.classes.HUMAN_FIGHTER
import org.l2kserver.game.data.character.classes.HUMAN_MYSTIC
import org.l2kserver.game.data.item.armor.ApprenticesStockings
import org.l2kserver.game.data.item.armor.ApprenticeTunic
import org.l2kserver.game.data.item.armor.LeatherShield
import org.l2kserver.game.data.item.armor.SquiresPants
import org.l2kserver.game.data.item.armor.SquiresShirt
import org.l2kserver.game.data.item.arrows.BoneArrow
import org.l2kserver.game.data.item.arrows.WoodenArrow
import org.l2kserver.game.data.item.book.TutorialGuide
import org.l2kserver.game.data.item.etc.Adena
import org.l2kserver.game.data.item.jewelry.EarringOfStrength
import org.l2kserver.game.data.item.jewelry.EarringOfWisdom
import org.l2kserver.game.data.item.jewelry.NecklaceOfCourage
import org.l2kserver.game.data.item.jewelry.RingOfAnguish
import org.l2kserver.game.data.item.jewelry.RingOfKnowledge
import org.l2kserver.game.data.item.soulshot.BlessedSpiritshotNoGrade
import org.l2kserver.game.data.item.soulshot.SoulshotNoGrade
import org.l2kserver.game.data.item.soulshot.SoulshotSGrade
import org.l2kserver.game.data.item.soulshot.SpiritshotNoGrade
import org.l2kserver.game.data.item.weapons.ApprenticesWand
import org.l2kserver.game.data.item.weapons.Bow
import org.l2kserver.game.data.item.weapons.Dagger
import org.l2kserver.game.data.item.weapons.DemonSplinter
import org.l2kserver.game.data.item.weapons.HeavensDivider
import org.l2kserver.game.data.item.weapons.ShortSpear
import org.l2kserver.game.data.item.weapons.SquiresSword
import org.l2kserver.game.data.item.weapons.TallumBladeDarkLegionsEdge
import org.l2kserver.game.data.item.weapons.WillowStaff
import org.l2kserver.game.data.npc.GrandMagisterGallint
import org.l2kserver.game.data.npc.GrandMasterRoien
import org.l2kserver.game.data.npc.Gremlin
import org.l2kserver.game.data.skill.CursePoison
import org.l2kserver.game.data.skill.DefenseAura
import org.l2kserver.game.data.skill.GreaterHeal
import org.l2kserver.game.data.skill.LifeScavenge
import org.l2kserver.game.data.skill.Might
import org.l2kserver.game.data.skill.MortalBlow
import org.l2kserver.game.data.skill.PowerShot
import org.l2kserver.game.data.skill.PowerStrike
import org.l2kserver.game.data.skill.SelfHeal
import org.l2kserver.game.data.skill.Spellcraft
import org.l2kserver.game.data.skill.WindStrike
import org.l2kserver.game.domain.AccessLevel
import org.l2kserver.game.domain.PlayerCharacterTable
import org.l2kserver.game.model.actor.character.Gender
import org.l2kserver.game.model.actor.character.CharacterClassRegistry
import org.l2kserver.game.model.actor.character.CharacterRace
import org.l2kserver.game.model.actor.character.ShortcutType
import org.l2kserver.game.model.actor.npc.NpcTemplateRegistry
import org.l2kserver.game.model.html.HtmlRegistry
import org.l2kserver.game.model.item.template.ItemTemplateRegistry
import org.l2kserver.game.model.skill.template.SkillTemplateRegistry
import org.l2kserver.game.repository.PlayerCharacterRepository
import org.l2kserver.game.repository.ShortcutRepository
import org.l2kserver.game.utils.LevelUtils
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

private const val TEST_CHARACTER_ACCOUNT_NAME = "admin"
private const val TEST_FIGHTER_CHARACTER_NAME = "TesterMan"
private const val TEST_MYSTIC_CHARACTER_NAME = "TesterWoman"

/** Loads data for LIVE test. Don't use it for integration testing */
@Component
@Suppress("unused")
class TestDataLoader(
    private val playerCharacterRepository: PlayerCharacterRepository,
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
        HtmlRegistry.loadResource("data/html")

        NpcTemplateRegistry.register(Gremlin, GrandMasterRoien, GrandMagisterGallint)
        CharacterClassRegistry.register(HUMAN_FIGHTER, HUMAN_MYSTIC)

        ItemTemplateRegistry.register(
            TutorialGuide,

            // No Grade weapons
            WillowStaff,
            Dagger,
            Bow,
            ShortSpear,
            SquiresSword,
            ApprenticesWand,

            // S-Grade Weapons
            DemonSplinter,
            HeavensDivider,
            TallumBladeDarkLegionsEdge,

            // Armor
            ApprenticeTunic,
            ApprenticesStockings,
            SquiresShirt,
            SquiresPants,
            LeatherShield,

            //Jewelry
            EarringOfStrength,
            EarringOfWisdom,
            RingOfAnguish,
            RingOfKnowledge,
            NecklaceOfCourage,

            //ETC
            Adena,

            // Arrows
            WoodenArrow,
            BoneArrow,

            //Soulshots
            SoulshotNoGrade,
            SoulshotSGrade,

            //Spiritshots
            SpiritshotNoGrade,
            BlessedSpiritshotNoGrade
        )

        SkillTemplateRegistry.register(
            PowerStrike,
            MortalBlow,
            PowerShot,
            LifeScavenge,
            DefenseAura,
            WindStrike,
            SelfHeal,
            GreaterHeal,
            Spellcraft,
            Might,
            CursePoison
        )
    }

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

        testFighter.exp = LevelUtils.getRequiredExpForLevel(10)
        PlayerCharacterTable.update({ PlayerCharacterTable.id eq testFighter.id }) {
            it[accessLevel] = AccessLevel.GAME_MASTER
        }

        testFighter.skillsAndMagic.learn(skillId = LifeScavenge.id, skillLevel = 1)
        testFighter.skillsAndMagic.learn(skillId = MortalBlow.id, skillLevel = 1)
        testFighter.skillsAndMagic.learn(skillId = PowerStrike.id, skillLevel = 1)
        testFighter.skillsAndMagic.learn(skillId = PowerShot.id, skillLevel = 1)
        testFighter.skillsAndMagic.learn(skillId = DefenseAura.id, skillLevel = 1)
        testFighter.skillsAndMagic.learn(skillId = Might.id, skillLevel = 1)
        testFighter.skillsAndMagic.learn(skillId = GreaterHeal.id, skillLevel = 1)

        testFighter.currentCp = testFighter.stats.maxCp
        testFighter.currentHp = testFighter.stats.maxHp
        testFighter.currentMp = testFighter.stats.maxMp

        shortcutRepository.create(
            testFighter.id,
            0,
            1,
            ShortcutType.SKILL,
            PowerStrike.id,
            5
        )

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
            it[currentHp] = 1
        }

    }

}
