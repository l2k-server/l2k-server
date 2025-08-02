package org.l2kserver.game.model.actor.npc

import org.l2kserver.game.model.actor.Actor
import org.l2kserver.game.model.actor.CollisionBox
import org.l2kserver.game.model.actor.MoveType
import org.l2kserver.game.model.actor.position.Heading
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.position.SpawnPosition
import org.l2kserver.game.model.actor.npc.ai.Ai
import org.l2kserver.game.model.item.WeaponType
import org.l2kserver.game.model.reward.Reward
import org.l2kserver.game.model.stats.Stats
import org.l2kserver.game.model.zone.SpawnZone

interface L2kNpc: Actor {
    override val id: Int
    override val name: String
    val templateId: Int
    override val level: Int
    val title: String?
    val isEnemy: Boolean
    val race: NpcRace
    override var heading: Heading
    override var position: Position
    override val stats: Stats
    val reward: Reward
    val spawnedAt: SpawnedAt
    val replicas: List<String>
    override val collisionBox: CollisionBox
    override var currentHp: Int
    override var currentMp: Int
    override var moveType: MoveType
    override val weaponType: WeaponType?
    override val hasShield: Boolean
    val ai: Ai?
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
