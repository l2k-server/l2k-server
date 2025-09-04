package org.l2kserver.game.model.actor

import org.l2kserver.game.model.actor.position.Heading
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.item.template.WeaponType
import org.l2kserver.game.model.stats.BasicStats
import org.l2kserver.game.model.stats.Stats

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
 * @property isImmobilized True if character cannot move
 * @property isParalyzed Is this actor paralyzed (cannot move or act)
 * @property weaponType This actor's weapon type
 * @property hasShield Does this actor have equipped shield
 * @property isFighting Is this actor in fighting stance
 * @property isMoving Is this actor running
 * @property targetId This actor's target id
 * @property targetedBy IDs of actors, who target this actor
 */
interface ActorInstance: GameWorldObject {
    override val id: Int
    override val position: Position
    override val collisionBox: CollisionBox
    val stats: Stats
    val basicStats: BasicStats
    val name: String
    val level: Int
    val heading: Heading
    val currentHp: Int
    val currentMp: Int
    val moveType: MoveType
    val isImmobilized: Boolean
    val isParalyzed: Boolean
    val weaponType: WeaponType?
    val hasShield: Boolean
    val isFighting: Boolean
    val isMoving: Boolean
    val targetId: Int?
    val targetedBy: Set<ActorInstance>

    /** Checks if actor can be attacked by [other] without forcing */
    fun isEnemyOf(other: ActorInstance): Boolean

    /** Is actor running now */
    val isRunning: Boolean get() = isMoving && moveType == MoveType.RUN

    /** Actor's movement speed */
    val moveSpeed: Int get() = if (moveType == MoveType.WALK) stats.walkSpeed else stats.speed

    /** Check if actor is behind other actor */
    fun isBehind(other: ActorInstance): Boolean {
        //If [this] is straight behind [other], delta will be equal to 0
        val delta = (other.heading - other.position.headingTo(this.position) + 32768).toShort()
        val tolerance = 8192 //45 degrees

        return -tolerance < delta && delta < tolerance
    }

    /** Check if actor is on side of other actor */
    fun isOnSideOf(other: ActorInstance): Boolean {
        //If [other] is headed directly to [this], delta will be equal to 0
        val delta = (other.heading - other.position.headingTo(this.position)).toShort()
        val tolerance = 8192 //45 degrees

        val isOnLeft = tolerance < delta && delta < 3 * tolerance
        val isOnRight = -tolerance > delta && delta > -3 * tolerance

        return isOnLeft || isOnRight
    }

    /** Is this actor dead */
    fun isDead() = this.currentHp <= 0

}
