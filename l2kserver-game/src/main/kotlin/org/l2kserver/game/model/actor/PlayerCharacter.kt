package org.l2kserver.game.model.actor

import java.util.concurrent.ConcurrentHashMap
import org.l2kserver.game.model.PaperDoll
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.utils.LevelUtils
import org.l2kserver.game.extensions.model.item.countWeightByOwnerId
import org.l2kserver.game.model.item.Item
import org.l2kserver.game.domain.PlayerCharacterEntity
import org.l2kserver.game.extensions.model.item.findAllEquippedByOwnerId
import org.l2kserver.game.extensions.model.stats.applyBasicStats
import org.l2kserver.game.extensions.model.stats.applyEquipment
import org.l2kserver.game.extensions.model.stats.applyLimitations
import org.l2kserver.game.extensions.model.stats.applyModifiers
import org.l2kserver.game.model.actor.character.L2kCharacterClass
import org.l2kserver.game.model.actor.character.PvpState
import org.l2kserver.game.model.actor.position.Heading
import org.l2kserver.game.model.stats.BasicStats
import org.l2kserver.game.model.stats.Stats
import org.l2kserver.game.model.stats.TradeAndInventoryStats
import org.l2kserver.game.model.store.PrivateStore

/**
 * Character data
 */
class PlayerCharacter(
    private val entity: PlayerCharacterEntity,
    val characterClass: L2kCharacterClass
) : Actor {

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

    var paperDoll = PaperDoll(Item.findAllEquippedByOwnerId(this.id))
    val itemsWeight: Int get() = Item.countWeightByOwnerId(this.id)

    override val collisionBox: CollisionBox get() {
        //Scan character class and its parent classes for character template, to get its collision box
        fun L2kCharacterClass.getCollisionBox(): CollisionBox = this.characterTemplate?.collisionBox ?: run {
            parentClass?.getCollisionBox() ?: CollisionBox(0.0, 0.0)
        }
        return characterClass.getCollisionBox()
    }

    override var isFighting = false
    override var isMoving = false

    override var targetId: Int? = null
    override val targetedBy: MutableSet<Int> = ConcurrentHashMap.newKeySet<Int>(0)

    var pvpState = PvpState.NOT_IN_PVP

    override val level: Int get() = LevelUtils.getByExp(exp)

    val basicStats: BasicStats get() = characterClass.basicStats

    override val stats: Stats get() = characterClass.combatStats
        .applyEquipment(paperDoll, characterClass)
        .applyModifiers(level, characterClass, basicStats)
        .applyLimitations()

    val tradeAndInventoryStats: TradeAndInventoryStats get() = characterClass.tradeAndInventoryStats
        .applyBasicStats(basicStats)//TODO apply skills

    var privateStore: PrivateStore? = null

    override val isImmobilized: Boolean get() = isParalyzed //TODO check if rooted, stunned, paralyzed, casting, etc...
    override val isParalyzed: Boolean get() = posture != Posture.STANDING

    override val weaponType get() = paperDoll.getWeapon()?.type
    override val hasShield: Boolean get() = paperDoll.shield != null

    //TODO Siege and clan relations
    override fun isEnemyOf(other: Actor) = karma > 0 || pvpState != PvpState.NOT_IN_PVP

    override fun toString() = "Character(name=$name id=$id gender=$gender race=$race)"
}
