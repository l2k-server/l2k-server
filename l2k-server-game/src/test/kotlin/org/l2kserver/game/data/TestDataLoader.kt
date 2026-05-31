package org.l2kserver.game.data

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.l2kserver.game.configuration.properties.LevelProperties
import org.l2kserver.game.data.characterclass.HumanFighter
import org.l2kserver.game.data.characterclass.HumanMystic
import org.l2kserver.game.data.skill.DefenseAura
import org.l2kserver.game.data.skill.GreaterHeal
import org.l2kserver.game.data.skill.LifeScavenge
import org.l2kserver.game.data.skill.Might
import org.l2kserver.game.data.skill.MortalBlow
import org.l2kserver.game.data.skill.PowerShot
import org.l2kserver.game.data.skill.PowerStrike
import org.l2kserver.game.domain.AccessLevel
import org.l2kserver.game.domain.PlayerCharacterTable
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.model.GameData
import org.l2kserver.game.model.GameDataRegistry
import org.l2kserver.game.model.actor.character.Gender
import org.l2kserver.game.model.actor.character.CharacterClassRegistry
import org.l2kserver.game.model.actor.character.CharacterRace
import org.l2kserver.game.model.actor.character.ShortcutType
import org.l2kserver.game.model.actor.npc.NpcRegistry
import org.l2kserver.game.model.html.HtmlRegistry
import org.l2kserver.game.model.item.ItemRegistry
import org.l2kserver.game.model.map.TownRegistry
import org.l2kserver.game.model.skill.template.SkillRegistry
import org.l2kserver.game.repository.PlayerCharacterRepository
import org.l2kserver.game.repository.ShortcutRepository
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.event.EventListener
import org.springframework.core.type.filter.AssignableTypeFilter
import org.springframework.stereotype.Component
import kotlin.math.roundToInt
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

private const val TEST_CHARACTER_ACCOUNT_NAME = "admin"
private const val TEST_FIGHTER_CHARACTER_NAME = "TesterMan"
private const val TEST_MYSTIC_CHARACTER_NAME = "TesterWoman"

/** Loads data for LIVE test. Don't use it for integration testing */
@Component
class TestDataLoader(
    private val playerCharacterRepository: PlayerCharacterRepository,
    private val shortcutRepository: ShortcutRepository,
    private val levelProperties: LevelProperties
) {
    private val log = logger()

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
        NpcRegistry.autoRegisterGameData("org.l2kserver.game.data.npc")
        CharacterClassRegistry.autoRegisterGameData("org.l2kserver.game.data.characterclass")
        ItemRegistry.autoRegisterGameData("org.l2kserver.game.data.item")
        SkillRegistry.autoRegisterGameData("org.l2kserver.game.data.skill")
        TownRegistry.autoRegisterGameData("org.l2kserver.game.data.town")
    }

    private fun createTestCharacter() = transaction {
        val testFighter = playerCharacterRepository.create(
            accountName = TEST_CHARACTER_ACCOUNT_NAME,
            characterName = TEST_FIGHTER_CHARACTER_NAME,
            race = CharacterRace.HUMAN,
            gender = Gender.MALE,
            classId = HumanFighter.id,
            hairColor = 1,
            hairStyle = 2,
            faceType = 3
        )

        testFighter.exp = levelProperties.getRequiredExpForLevel(80)
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

        testFighter.currentCp = testFighter.stats.maxCp.roundToInt()
        testFighter.currentHp = testFighter.stats.maxHp.roundToInt()
        testFighter.currentMp = testFighter.stats.maxMp.roundToInt()

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
            classId = HumanMystic.id,
            hairColor = 3,
            hairStyle = 2,
            faceType = 1
        )

        PlayerCharacterTable.update({ PlayerCharacterTable.id eq testMystic.id }) {
            it[accessLevel] = AccessLevel.GAME_MASTER
            it[currentHp] = 1
        }

    }

    private inline fun <reified T : GameData> GameDataRegistry<T>.autoRegisterGameData(vararg basePackages: String) {
        //FIXME Looks like overhead, use more simple way to scan
        val scanner = ClassPathScanningCandidateComponentProvider(false).apply {
            addIncludeFilter(AssignableTypeFilter(T::class.java))
        }

        for (basePackage in basePackages) {
            val beanDefinitions = scanner.findCandidateComponents(basePackage)
            for (beanDefinition in beanDefinitions) {
                @Suppress("UNCHECKED_CAST")
                val clazz = Class.forName(beanDefinition.beanClassName).kotlin as KClass<out T>

                val instance: T = when {
                    clazz.objectInstance != null -> clazz.objectInstance!!
                    clazz.constructors.any { it.parameters.isEmpty() } -> clazz.createInstance()
                    else -> {
                        log.warn { "GameData ${clazz.qualifiedName} must be object or have empty constructor" }
                        continue
                    }
                }
                this.register(instance)
            }
        }
    }

}
