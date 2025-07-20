package org.l2kserver.game.model.actor

import java.util.concurrent.ConcurrentHashMap
import org.l2kserver.game.ai.L2AiScript
import org.l2kserver.game.domain.item.template.WeaponType
import org.l2kserver.game.model.CollisionBox
import org.l2kserver.game.model.position.Position
import org.l2kserver.game.model.stats.Stats
import org.l2kserver.game.domain.npc.NpcRace
import org.l2kserver.game.domain.npc.SpawnZone
import org.l2kserver.game.model.Reward
import org.l2kserver.game.model.position.Heading
import org.l2kserver.game.model.actor.enumeration.MoveType
import org.l2kserver.game.model.position.SpawnPosition

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
    val templateId: Int,
    override val level: Int,
    val title: String?,
    val isEnemy: Boolean,
    val race: NpcRace,
    override var heading: Heading,
    override var position: Position,
    override val stats: Stats,
    val reward: Reward,
    val spawnedAt: SpawnedAt,
    val replicas: List<String>,
    override val collisionBox: CollisionBox,
    override var currentHp: Int,
    override var currentMp: Int,
    override var moveType: MoveType,
    override val weaponType: WeaponType?,
    override val hasShield: Boolean,
    val ai: L2AiScript?
): Actor {
    val initialPosition = position.copy()
    val initialHeading = Heading(heading.value)

    /**
     * How much damage had the opponents dealt to this NPC
     *
     * Key - attackerId, Value - damage dealt
     */
    //TODO clean this map after fighting has ended
    val opponentsDamage = ConcurrentHashMap<Actor, Int>(0)

    override val isImmobilized: Boolean get() = isParalyzed //TODO check if rooted, stunned, paralyzed, casting, etc...
    override val isParalyzed: Boolean get() = false

    override fun isEnemyOf(other: Actor): Boolean = isEnemy

    override var isFighting = false
    override var isMoving = false

    override var targetId: Int? = null
    override val targetedBy: MutableSet<Int> = ConcurrentHashMap.newKeySet<Int>(0)

    override fun toString() = "Npc(name=$name id=$id)"
}

/**
 * Record about where was the NPC spawned - at position or SpawnZone.
 * Only one of them can be provided
 */
class SpawnedAt private constructor(
    val spawnPosition: SpawnPosition?,
    val spawnZone: SpawnZone?
) {
    constructor(spawnZone: SpawnZone): this(spawnPosition = null, spawnZone = spawnZone)
    constructor(spawnPosition: SpawnPosition): this(spawnPosition = spawnPosition, spawnZone = null)

    init {
        require((spawnPosition == null) xor (spawnZone == null)) {
            "One NPC cannot be spawned at both position and zone"
        }
    }

    override fun toString(): String {
        return if (spawnZone != null ) "SpawnedAt(spawnZone=$spawnZone)"
        else "SpawnedAt(spawnPosition=$spawnPosition"
    }
}
