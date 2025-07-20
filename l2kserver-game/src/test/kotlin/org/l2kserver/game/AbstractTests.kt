package org.l2kserver.game

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.l2kserver.game.configuration.HazelcastInstanceTestConfiguration
import org.l2kserver.game.model.session.AuthorizationKey
import org.l2kserver.game.repository.GameObjectDAO
import org.l2kserver.game.domain.character.PlayerCharacterTable
import org.l2kserver.game.domain.item.entity.ItemTable
import org.l2kserver.game.domain.item.template.ItemTemplate
import org.l2kserver.game.domain.shortcut.ShortcutTable
import org.l2kserver.game.domain.skill.LearnedSkillsTable
import org.l2kserver.game.extensions.model.actor.create
import org.l2kserver.game.extensions.model.item.createAllFrom
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.actor.ScatteredItem
import org.l2kserver.game.model.actor.enumeration.CharacterClassName
import org.l2kserver.game.model.actor.enumeration.CharacterRace
import org.l2kserver.game.model.actor.enumeration.Gender
import org.l2kserver.game.model.item.InitialItem
import org.l2kserver.game.model.item.Item
import org.l2kserver.game.model.position.Position
import org.l2kserver.game.network.session.SessionContext
import org.l2kserver.game.service.ActorStateService
import org.l2kserver.game.utils.IdUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.random.Random
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

@SpringBootTest(
    // Local HazelcastInstance must be started for tests, in due not to conflict with bootRunTest gradle task
    properties = ["spring.main.allow-bean-definition-overriding=true"],
    classes = [HazelcastInstanceTestConfiguration::class]
)
abstract class AbstractTests {

    @Autowired
    protected lateinit var gameObjectDAO: GameObjectDAO

    @Autowired
    private lateinit var actorStateService: ActorStateService

    protected val testLogin = "VitalyasAccount"
    protected val testCharacterName = "Vitaliy"

    @BeforeEach
    fun init(): Unit = runBlocking {
        transaction {
            ShortcutTable.deleteAll()
            ItemTable.deleteAll()
            LearnedSkillsTable.deleteAll()
            PlayerCharacterTable.deleteAll()
        }
        SessionContext.clear()
        gameObjectDAO.deleteAll()
        actorStateService.flushStates()
    }

    protected fun createRandomAuthorizationKey() = AuthorizationKey(
        loginSessionKey1 = Random.nextInt(),
        loginSessionKey2 = Random.nextInt(),
        gameSessionKey1 = Random.nextInt(),
        gameSessionKey2 = Random.nextInt()
    )

    protected fun createTestCharacter(
        enterGame: Boolean = true,
        name: String = testCharacterName
    ): PlayerCharacter {
        val characterId = transaction {
            PlayerCharacter.create(
                accountName = testLogin,
                characterName = name,
                race = CharacterRace.HUMAN,
                gender = Gender.MALE,
                className = CharacterClassName.HUMAN_FIGHTER,
                hairStyle = 0,
                hairColor = 0,
                faceType = 0
            )
        }

        return if (enterGame) gameObjectDAO.loadCharacter(characterId)
        else transaction { PlayerCharacter.findById(characterId) }
    }

    protected fun createTestSessionContext(): SessionContext {
        val context = SessionContext(Random.nextInt())
        context.setAccountName(testLogin)
        context.setAuthorizationKey(createRandomAuthorizationKey())

        return context
    }

    protected suspend fun createTestItem(
        templateId: Int,
        ownerId: Int,
        amount: Int = 1,
        isEquipped: Boolean = false
    ) = transaction {
        Item.createAllFrom(ownerId, listOf(InitialItem(templateId, "", amount, isEquipped))).first()
    }

    protected suspend fun createTestScatteredItem(
        position: Position,
        template: ItemTemplate,
        amount: Int = 1,
        enchantLevel: Int = 0
    ): ScatteredItem {
        val scatteredItem = ScatteredItem(
            id = IdUtils.getNextScatteredItemId(),
            position = position,
            templateId = template.id,
            isStackable = template.isStackable,
            amount = amount,
            enchantLevel = enchantLevel
        )

        return gameObjectDAO.save(scatteredItem)
    }

    private fun ActorStateService.flushStates() {
        val fightingActors: MutableMap<Int, Long> = this.getPrivateProperty("fightingActors")!!
        fightingActors.clear()

        val charactersInPvpState: MutableMap<Int, Long> = this.getPrivateProperty("fightingActors")!!
        charactersInPvpState.clear()
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Any, R> T.getPrivateProperty(name: String): R? = T::class
        .memberProperties
        .firstOrNull { it.name == name }
        ?.apply { isAccessible = true }
        ?.get(this) as? R?
}
