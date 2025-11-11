package org.l2kserver.game.model.actor

import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.l2kserver.game.domain.TemporalEffects
import java.util.concurrent.ConcurrentHashMap
import org.l2kserver.game.domain.Inventory
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.utils.LevelUtils
import org.l2kserver.game.domain.PlayerCharacterEntity
import org.l2kserver.game.domain.SkillsAndMagic
import org.l2kserver.game.extensions.model.stats.applyBasicStats
import org.l2kserver.game.extensions.model.stats.applyEquipmentOf
import org.l2kserver.game.extensions.model.stats.applyLimitations
import org.l2kserver.game.extensions.model.stats.applyModifiersOf
import org.l2kserver.game.extensions.model.stats.applyFixedBonusStatsOf
import org.l2kserver.game.extensions.model.stats.applyPostureBonusOf
import org.l2kserver.game.model.actor.character.CharacterClass
import org.l2kserver.game.model.actor.character.CharacterInstance
import org.l2kserver.game.model.actor.character.PvpState
import org.l2kserver.game.model.actor.position.Heading
import org.l2kserver.game.model.item.Soulshot
import org.l2kserver.game.model.item.Spiritshot
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
 * @property hairStyle Character's hairstyle
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
): MutableActorInstance(), CharacterInstance {

    override val id: Int = entity.id.value
    override val accountName by entity::accountName
    override val name by entity::name

    override var title by entity::title
    override var clanId by entity::clanId

    override val gender by entity::gender
    override val race by entity::race

    override var currentCp by entity::currentCp
    override var currentHp by entity::currentHp
    override var currentMp by entity::currentMp

    override var sp by entity::sp
    override var exp by entity::exp
    override var karma by entity::karma
    override var pvpCount by entity::pvpCount
    override var pkCount by entity::pkCount

    override val hairStyle by entity::hairStyle
    override val hairColor by entity::hairColor
    override val faceType by entity::faceType

    var lastAccess by entity::lastAccess
    var deletionDate by entity::deletionDate

    override var moveType = MoveType.RUN
    override var posture: Posture = Posture.STANDING

    val nameColor by entity::nameColor
    val titleColor by entity::titleColor
    var activeSubclass by entity::activeSubclass

    val accessLevel by entity::accessLevel

    override var position: Position
        get() = transaction { Position(entity.x, entity.y, entity.z) }
        set(newPosition) = transaction {
            entity.x = newPosition.x
            entity.y = newPosition.y
            entity.z = newPosition.z
        }

    override var heading = Heading()

    override val inventory = Inventory(this)
    override val skillsAndMagic = SkillsAndMagic(this)

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

    override val basicStats: BasicStats get() = characterClass.basicStats //TODO + Henna, set bonuses, augmentations

    //TODO Cache?
    override val stats: CombatStats get() = characterClass.combatStats
        .applyEquipmentOf(this)
        .applyModifiersOf(this)
        .applyFixedBonusStatsOf(this)
        .applyPostureBonusOf(this)
        .applyLimitations()

    val tradeAndInventoryStats: TradeAndInventoryStats get() = characterClass.tradeAndInventoryStats
        .applyBasicStats(basicStats)//TODO apply skills

    var privateStore: PrivateStore? = null

    override val isImmobilized: Boolean get() = isParalyzed //TODO check if rooted, stunned, paralyzed, casting, etc...
    override val isParalyzed: Boolean get() = posture != Posture.STANDING

    override val weaponType get() = inventory.weapon?.type
    override val hasShield: Boolean get() = inventory.shield != null

    var autoUsesSoulshot: Soulshot? = null
    var autoUsesSpiritshot: Spiritshot? = null

    val knownGameWorldObjects: MutableSet<GameWorldObject> = ConcurrentHashMap.newKeySet()

    override val temporalEffects = TemporalEffects()

    //TODO Siege and clan relations
    override fun isEnemyOf(other: ActorInstance): Boolean {
        if (other == this) return false

        return karma > 0 || pvpState != PvpState.NOT_IN_PVP
    }

    override fun toString() = "Character(name=$name id=$id gender=$gender race=$race)"
}
