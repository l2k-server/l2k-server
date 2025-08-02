package org.l2kserver.game.model.actor

import org.l2kserver.game.model.actor.position.Heading
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.item.WeaponType
import org.l2kserver.game.model.stats.Stats

/**
 * Interface for all game objects in the world - characters, monsters, dropped items etc.
 *
 * @property id Game object unique identifier
 * @property position Game object position in game world
 * @property collisionBox This game object's collision box
 */
interface GameObject {
    val id: Int
    val position: Position
    val collisionBox: CollisionBox
}

/**
 * Game actor (npc, monster, player character, etc.)
 *
 * @property stats Actor's battle characteristics
 * @property name Actor's name
 * @property level Actor's level
 * @property position Actor's position in game world
 * @property heading Actor's sight direction
 * @property currentHp Actor's current HP
 * @property currentMp Actor's current MP
 * @property moveType This actor's moving mode (sitting, walking, or running)
 * @property isImmobilized
 * @property isParalyzed is this actor paralyzed (cannot move or act)
 * @property weaponType this actor's weapon type
 * @property hasShield does this actor have equipped shield
 * @property isFighting is this actor in fighting stance
 * @property isMoving is this actor running
 * @property targetId This actor's target id
 * @property targetedBy IDs of actors, who target this actor
 */
interface Actor: GameObject {
    val stats: Stats
    val name: String
    val level: Int
    override var position: Position
    var heading: Heading
    var currentHp: Int
    var currentMp: Int
    var moveType: MoveType
    val isImmobilized: Boolean
    val isParalyzed: Boolean
    val weaponType: WeaponType?
    val hasShield: Boolean
    var isFighting: Boolean
    var isMoving: Boolean
    var targetId: Int?
    val targetedBy: MutableSet<Int>

    /**
     * Checks if actor can be attacked by [other] without forcing
     */
    fun isEnemyOf(other: Actor): Boolean

    /**
     * Is actor running now
     */
    val isRunning: Boolean get() = isMoving && moveType == MoveType.RUN

    /**
     * Actor's movement speed
     */
    val moveSpeed: Int get() = if (moveType == MoveType.WALK) stats.walkSpeed else stats.speed

    /**
     * Check if actor is behind other actor
     */
    fun isBehind(other: Actor): Boolean {
        //If [this] is straight behind [other], delta will be equal to 0
        val delta = (other.heading - other.position.headingTo(this.position) + 32768).toShort()
        val tolerance = 8192 //45 degrees

        return -tolerance < delta && delta < tolerance
    }

    /**
     * Check if actor is on side of other actor
     */
    fun isOnSideOf(other: Actor): Boolean {
        //If [other] is headed directly to [this], delta will be equal to 0
        val delta = (other.heading - other.position.headingTo(this.position)).toShort()
        val tolerance = 8192 //45 degrees

        val isOnLeft = tolerance < delta && delta < 3 * tolerance
        val isOnRight = -tolerance > delta && delta > -3 * tolerance

        return isOnLeft || isOnRight
    }

    /**
     * Check if actor can attack [target] from his current position
     */
    fun isEnoughCloseToAttack(target: Actor) = this.position.isCloseTo(
        other = target.position,
        distance = this.stats.attackRange + Position.ACCEPTABLE_DELTA
    )

    /**
     * Is this actor dead
     */
    fun isDead() = this.currentHp <= 0
}
