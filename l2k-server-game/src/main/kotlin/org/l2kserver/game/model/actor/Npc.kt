package org.l2kserver.game.model.actor

import org.l2kserver.game.model.actor.npc.NpcInstance
import java.util.concurrent.ConcurrentHashMap
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.npc.NpcRace
import org.l2kserver.game.model.actor.npc.SpawnedAt
import org.l2kserver.game.model.actor.position.Heading
import org.l2kserver.game.model.actor.npc.ai.Ai
import org.l2kserver.game.model.item.template.ArmorTemplate
import org.l2kserver.game.model.item.template.WeaponTemplate
import org.l2kserver.game.model.reward.Reward
import org.l2kserver.game.model.stats.BasicStats
import org.l2kserver.game.model.stats.Stats

/**
 * NPC data
 *
 * @property id NPC identifier
 * @property name NPC name
 * @property templateId NPC template ID
 * @property level NPC level
 * @property title NPC title
 * @property isEnemy Can this NPC be attacked without forcing
 * @property race NPC's race
 * @property heading NPC's heading direction
 * @property position NPC's position in world
 * @property stats NPC's stats
 * @property reward Reward for killing this NPC
 * @property spawnedAt Where was this NPC spawned (position or zone)
 * @property replicas NPC's chat replicas
 * @property collisionBox NPC's collision box
 * @property currentHp NPC's current HP
 * @property currentMp NPC's current mana
 * @property moveType NPC's current move type
 * @property weaponType NPC's weapon type
 * @property hasShield Can this NPC block attacks by shield
 * @property ai AI script for this NPC
 */
class Npc(
    override val id: Int,
    override val name: String,
    override val templateId: Int,
    override val level: Int,
    override val title: String?,
    override val isEnemy: Boolean,
    override val race: NpcRace,
    override var heading: Heading,
    override var position: Position,
    override val stats: Stats,
    override val basicStats: BasicStats,
    override val reward: Reward,
    override val spawnedAt: SpawnedAt,
    override val replicas: List<String>,
    override val collisionBox: CollisionBox,
    override var currentHp: Int,
    override var currentMp: Int,
    override var moveType: MoveType,
    override val ai: Ai?,

    var equippedWeaponTemplate: WeaponTemplate? = null,
    var equippedShieldTemplate: ArmorTemplate? = null
): MutableActorInstance(), NpcInstance {

    /**
     * How much damage had the opponents dealt to this NPC
     *
     * Key - attackerId, Value - damage dealt
     */
    //TODO clean this map after fighting has ended
    val opponentsDamage = ConcurrentHashMap<ActorInstance, Int>(0)

    override val isImmobilized: Boolean get() = isParalyzed //TODO check if rooted, stunned, paralyzed, casting, etc...
    override val isParalyzed: Boolean get() = false

    override fun isEnemyOf(other: ActorInstance): Boolean = isEnemy

    override var isFighting = false
    override var isMoving = false

    override var targetId: Int? = null
    override val targetedBy: MutableSet<ActorInstance> = ConcurrentHashMap.newKeySet(0)

    override val weaponType = equippedWeaponTemplate?.type
    override val hasShield = equippedShieldTemplate != null

    override fun toString() = "Npc(name=$name id=$id)"
}
