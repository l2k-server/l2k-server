package org.l2kserver.game.model.actor

import org.l2kserver.game.domain.item.template.WeaponType
import org.l2kserver.game.model.position.Heading
import org.l2kserver.game.model.position.Position
import org.l2kserver.game.model.stats.Stats
import org.l2kserver.game.model.actor.enumeration.MoveType

/**
 * Game actor (npc, monster, player character, etc.)
 *
 * @property stats Actor's Stats
 * @property name Actor's name
 * @property position Actor's position in game world
 * @property heading Actor's sight direction
 * @property currentHp Actor's current HP
 * @property currentMp Actor's current MP
 * @property moveType this actor's moving mode (sitting, walking, or running)
 * @property isImmobilized is this actor immobilized
 * @property isParalyzed is this actor paralyzed (cannot move or act)
 * @property weaponType this actor's weapon type
 * @property hasShield does this actor have equipped shield
 * @property isFighting is this actor in fighting stance
 * @property isMoving is this actor running
 * @property targetId This actor's target id
 * @property targetedBy IDs of actors, who target this actor
 */
sealed interface Actor: GameObject {
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

}

/**
 * Is actor running now
 */
val Actor.isRunning: Boolean get() = isMoving && moveType == MoveType.RUN

/**
 * Actor's movement speed
 */
val Actor.moveSpeed: Int get() = if (moveType == MoveType.WALK) stats.walkSpeed else stats.speed

/**
 * Check if actor is behind other actor
 */
fun Actor.isBehind(other: Actor): Boolean {
    //If [this] is straight behind [other], delta will be equal to 0
    val delta = (other.heading - other.position.headingTo(this.position) + 32768).toShort()
    val tolerance = 8192 //45 degrees

    return -tolerance < delta && delta < tolerance
}

/**
 * Check if actor is on side of other actor
 */
fun Actor.isOnSideOf(other: Actor): Boolean {
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
fun Actor.isEnoughCloseToAttack(target: Actor) = this.position.isCloseTo(
    other = target.position,
    distance = this.stats.attackRange + Position.ACCEPTABLE_DELTA
)

/**
 * Is this actor dead
 */
fun Actor.isDead() = this.currentHp <= 0
