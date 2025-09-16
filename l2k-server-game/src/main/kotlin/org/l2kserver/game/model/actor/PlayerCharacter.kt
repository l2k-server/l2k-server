package org.l2kserver.game.model.actor

import java.util.concurrent.ConcurrentHashMap
import org.l2kserver.game.domain.Inventory
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.utils.LevelUtils
import org.l2kserver.game.domain.PlayerCharacterEntity
import org.l2kserver.game.extensions.model.skill.findAllByCharacterIdAndSubclassIndex
import org.l2kserver.game.extensions.model.stats.applyBasicStats
import org.l2kserver.game.extensions.model.stats.applyEquipment
import org.l2kserver.game.extensions.model.stats.applyLimitations
import org.l2kserver.game.extensions.model.stats.applyModifiers
import org.l2kserver.game.model.actor.character.CharacterClass
import org.l2kserver.game.model.actor.character.PvpState
import org.l2kserver.game.model.actor.position.Heading
import org.l2kserver.game.model.item.Soulshot
import org.l2kserver.game.model.item.Spiritshot
import org.l2kserver.game.model.skill.Skill
import org.l2kserver.game.model.stats.BasicStats
import org.l2kserver.game.model.stats.CombatStats
import org.l2kserver.game.model.stats.TradeAndInventoryStats
import org.l2kserver.game.model.store.PrivateStore

/**
 * Player-controlled character in the game world.
 *
 * This class encapsulates all data and behavior for a player character, providing:
 * - Access to core identifiers and attributes (name, gender, race, clan, title);
 * - Current resource stats (CP/HP/MP), experience/level, and reputation (karma, PK/PvP);
 * - World position and heading, movement/combat states, and targeting information;
 * - Inventory and equipment that influence calculated combat statistics;
 * - Character skills (considering active subclass) and utilities for skill management;
 * - Trading parameters and private store functionality.
 *
 * The persistent data source is [entity]; changes to public properties are synchronized
 * with entity fields. Combat statistics are calculated based on character class template
 * ([characterClass]) and modified by equipment and limitations.
 *
 * The class also maintains auxiliary states (PvP, paralysis/posture, auto soulshot/spiritshot)
 * and provides relationship logic (e.g., [isEnemyOf]). The [targetedBy] collection is thread-safe.
 *
 * Notes on computed properties:
 * - [level] is determined based on [exp];
 * - [stats] are assembled from class base parameters, equipment, and modifiers;
 *
 * @property characterClass The character class providing base/combat/trade stats and template
 * @property id Unique character identifier
 * @property accountName Name of the account that owns this character
 * @property name Character's display name
 * @property title Character's title
 * @property clanId ID of the clan this character belongs to
 * @property gender Character's gender
 * @property race Character's race
 * @property currentCp Current Combat Points
 * @property currentHp Current Health Points
 * @property currentMp Current Mana Points
 * @property sp Skill Points
 * @property exp Experience points
 * @property karma Karma value
 * @property pvpCount Number of PvP kills
 * @property pkCount Number of Player Kills
 * @property hairStyle Character's hair style
 * @property hairColor Character's hair color
 * @property faceType Character's face type
 * @property lastAccess Timestamp of last character access
 * @property deletionDate Timestamp when character will be deleted
 * @property moveType Current movement type (walk/run)
 * @property posture Current character posture (standing/sitting/etc)
 * @property nameColor Color of the character's name
 * @property titleColor Color of the character's title
 * @property activeSubclass Currently active subclass index
 * @property accessLevel Character's access level (GM permissions)
 * @property position Character's world position
 * @property heading Character's facing direction
 * @property inventory Character's inventory
 * @property collisionBox Character's collision box
 * @property isFighting Whether character is currently in combat
 * @property isMoving Whether character is currently moving
 * @property targetId ID of current target
 * @property targetedBy Set of actors currently targeting this character
 * @property pvpState Current PvP state
 * @property level Character's current level (computed from exp)
 * @property basicStats Basic stats
 * @property stats Final combat stats (base + equipment + modifiers)
 * @property tradeAndInventoryStats Stats for trading and inventory operations
 * @property skills Skill that are available for character
 * @property privateStore Character's private store (if any)
 * @property isImmobilized Whether character is immobilized
 * @property isParalyzed Whether character is paralyzed
 * @property weaponType Type of currently equipped weapon
 * @property hasShield Whether character has a shield equipped
 * @property autoUsesSoulshot Automatically used soulshot item. Null if disabled
 * @property autoUsesSpiritshot Automatically used spiritshot item. Null if disabled
 */
class PlayerCharacter(
    private val entity: PlayerCharacterEntity,
    val characterClass: CharacterClass
): MutableActorInstance() {

    override val id: Int = entity.id.value
    val accountName by entity::accountName
    override val name by entity::name

    var title by entity::title
    var clanId by entity::clanId

    val gender by entity::gender
    val race by entity::race

    var currentCp by entity::currentCp
    override var currentHp by entity::currentHp
    override var currentMp by entity::currentMp

    var sp by entity::sp
    var exp by entity::exp
    var karma by entity::karma
    var pvpCount by entity::pvpCount
    var pkCount by entity::pkCount

    val hairStyle by entity::hairStyle
    val hairColor by entity::hairColor
    val faceType by entity::faceType

    var lastAccess by entity::lastAccess
    var deletionDate by entity::deletionDate

    override var moveType = MoveType.RUN
    var posture: Posture = Posture.STANDING

    val nameColor by entity::nameColor
    val titleColor by entity::titleColor
    var activeSubclass by entity::activeSubclass

    val accessLevel by entity::accessLevel

    override var position: Position
        get() = Position(entity.x, entity.y, entity.z)
        set(newPosition) {
            entity.x = newPosition.x
            entity.y = newPosition.y
            entity.z = newPosition.z
        }

    override var heading = Heading()

    val inventory = Inventory(this)

    override val collisionBox: CollisionBox get() {
        //Scan character class and its parent classes for character template, to get its collision box
        fun CharacterClass.getCollisionBox(): CollisionBox = this.characterTemplate?.collisionBox ?: run {
            parentClass?.getCollisionBox() ?: CollisionBox(0.0, 0.0)
        }
        return characterClass.getCollisionBox()
    }

    override var isFighting = false
    override var isMoving = false

    override var targetId: Int? = null
    override val targetedBy: MutableSet<ActorInstance> = ConcurrentHashMap.newKeySet(0)

    var pvpState = PvpState.NOT_IN_PVP

    override val level: Int get() = LevelUtils.getLevelByExp(exp)

    override val basicStats: BasicStats get() = characterClass.basicStats

    override val stats: CombatStats get() = characterClass.combatStats
        .applyEquipment(this)
        .applyModifiers(level, characterClass, basicStats)
        .applyLimitations() //TODO apply skills

    val tradeAndInventoryStats: TradeAndInventoryStats get() = characterClass.tradeAndInventoryStats
        .applyBasicStats(basicStats)//TODO apply skills

    val skills: Map<Int, Skill> get() = Skill.findAllByCharacterIdAndSubclassIndex(
        characterId = this.id, subclassIndices = arrayOf(null, this.activeSubclass)
    ).associateBy { it.skillId }

    var privateStore: PrivateStore? = null

    override val isImmobilized: Boolean get() = isParalyzed //TODO check if rooted, stunned, paralyzed, casting, etc...
    override val isParalyzed: Boolean get() = posture != Posture.STANDING

    override val weaponType get() = inventory.weapon?.type
    override val hasShield: Boolean get() = inventory.shield != null

    var autoUsesSoulshot: Soulshot? = null
    var autoUsesSpiritshot: Spiritshot? = null

    //TODO Siege and clan relations
    override fun isEnemyOf(other: ActorInstance) = karma > 0 || pvpState != PvpState.NOT_IN_PVP

    override fun toString() = "Character(name=$name id=$id gender=$gender race=$race)"

    /**
     * Finds skill by [skillId] in this character's skill list
     *
     * @throws IllegalStateException if no skill was found
     */
    fun getSkillById(skillId: Int) = requireNotNull(this.skills[skillId]) {
        "$this has no skill with id = $skillId"
    }
}
