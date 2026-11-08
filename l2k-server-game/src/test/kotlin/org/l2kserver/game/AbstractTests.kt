package org.l2kserver.game

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.l2kserver.game.data.characterclass.HumanFighter
import org.l2kserver.game.domain.ItemEntity
import org.l2kserver.game.model.session.AuthorizationKey
import org.l2kserver.game.repository.GameWorld
import org.l2kserver.game.domain.PlayerCharacterTable
import org.l2kserver.game.domain.ItemTable
import org.l2kserver.game.domain.ShortcutTable
import org.l2kserver.game.domain.SkillTable
import org.l2kserver.game.extensions.model.item.toItemInstance
import org.l2kserver.game.model.GameData
import org.l2kserver.game.model.GameDataRegistry
import org.l2kserver.game.model.actor.PlayerCharacterInstanceImpl
import org.l2kserver.game.model.actor.ScatteredItem
import org.l2kserver.game.model.actor.character.CharacterRace
import org.l2kserver.game.model.actor.character.Gender
import org.l2kserver.game.model.actor.npc.NpcRegistry
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.item.Item
import org.l2kserver.game.model.item.ItemRegistry
import org.l2kserver.game.network.session.SessionContext
import org.l2kserver.game.repository.PlayerCharacterRepository
import org.l2kserver.game.service.ActorStateService
import org.l2kserver.game.service.AsyncTaskService
import org.l2kserver.game.service.IdGenerationService
import org.l2kserver.game.service.IntentionExecutorService
import org.l2kserver.game.utils.getPrivateProperty
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.random.Random

@SpringBootTest
// This profile disables several server tasks, as character perceptions and primary npc spawning
@ActiveProfiles("test")
abstract class AbstractTests {

    @Autowired
    private lateinit var asyncTaskService: AsyncTaskService

    @Autowired
    private lateinit var actorStateService: ActorStateService

    @Autowired
    protected lateinit var gameWorld: GameWorld

    @Autowired
    protected lateinit var playerCharacterRepository: PlayerCharacterRepository

    @Autowired
    private lateinit var intentionExecutorService: IntentionExecutorService

    @Autowired private lateinit var idGenerationService: IdGenerationService

    protected val testLogin = "VitalyasAccount"
    protected val testCharacterName = "Vitaliy"

    @BeforeEach
    fun init(): Unit = runBlocking {
        transaction {
            ShortcutTable.deleteAll()
            ItemTable.deleteAll()
            SkillTable.deleteAll()
            PlayerCharacterTable.deleteAll()
        }
        SessionContext.clear()
        gameWorld.deleteAll()
        actorStateService.flushStates()
        asyncTaskService.cancelTask("REGENERATION_JOB")
        intentionExecutorService.cancelAll()
        NpcRegistry.flush()
    }

    protected fun createRandomAuthorizationKey() = AuthorizationKey(
        loginSessionKey1 = Random.nextInt(),
        loginSessionKey2 = Random.nextInt(),
        gameSessionKey1 = Random.nextInt(),
        gameSessionKey2 = Random.nextInt()
    )

    protected fun createTestCharacter(
        enterGame: Boolean = true,
        name: String = testCharacterName,
        classId: Int = HumanFighter.id
    ): PlayerCharacterInstanceImpl {
        val character = playerCharacterRepository.create(
            accountName = testLogin,
            characterName = name,
            race = CharacterRace.HUMAN,
            gender = Gender.MALE,
            classId = classId,
            hairStyle = 0,
            hairColor = 0,
            faceType = 0
        )

        if (enterGame) {
            val context = createTestSessionContext(character.accountName)
            context.setCharacterId(character.id)

            gameWorld.save(character)
            intentionExecutorService.launchIntentionQueueListener(character)
        }

        return character
    }

    protected fun createTestSessionContext(accountName: String): SessionContext {
        val context = SessionContext(Random.nextInt())
        context.setAccountName(accountName)
        context.setAuthorizationKey(createRandomAuthorizationKey())

        return context
    }

    protected suspend fun createTestItem(
        templateId: Int, owner: PlayerCharacterInstanceImpl, amount: Int = 1, isEquipped: Boolean = false
    ) = transaction {
        val itemTemplate = ItemRegistry.findById(templateId)

        val item = ItemEntity.new(idGenerationService.next()) {
            this.templateId = templateId
            this.ownerId = owner.id
            this.amount = amount
            this.equippedAt = if (isEquipped) itemTemplate.type.availableSlots.firstOrNull() else null
        }.toItemInstance()
        owner.inventory.reload()

        item
    }

    protected suspend fun createTestScatteredItem(
        position: Position,
        template: Item,
        amount: Int = 1,
        enchantLevel: Int = 0
    ): ScatteredItem {
        val scatteredItem = ScatteredItem(
            id = idGenerationService.next(),
            position = position,
            templateId = template.id,
            isStackable = template.isStackable,
            amount = amount,
            enchantLevel = enchantLevel
        )

        return gameWorld.save(scatteredItem)
    }

    private fun ActorStateService.flushStates() {
        val fightingActors: MutableMap<Int, Long> = this.getPrivateProperty("fightingActors")!!
        fightingActors.clear()

        val charactersInPvpState: MutableMap<Int, Long> = this.getPrivateProperty("charactersInPvpState")!!
        charactersInPvpState.clear()
    }

    private fun <T: GameData> GameDataRegistry<T>.flush() {
        val gameDataMap: MutableMap<Int, Long> = this.getPrivateProperty("gameDataStorage")!!
        gameDataMap.clear()
    }
}
