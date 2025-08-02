package org.l2kserver.game.model.actor.npc

import org.l2kserver.game.model.actor.CollisionBox
import org.l2kserver.game.model.GameData
import org.l2kserver.game.model.GameDataRegistry
import org.l2kserver.game.model.actor.npc.ai.Ai
import org.l2kserver.game.model.actor.position.SpawnPosition
import org.l2kserver.game.model.item.WeaponType
import org.l2kserver.game.model.reward.Reward
import org.l2kserver.game.model.stats.Stats
import org.l2kserver.game.model.zone.SpawnZone

data class NpcTemplate(
    override val id: Int,
    val name: String,
    val title: String? = null,
    val level: Int,
    val isAggressive: Boolean,
    val isEnemy: Boolean,
    val isInvulnerable: Boolean,
    val race: NpcRace,
    val collisionBox: CollisionBox,
    val stats: Stats = Stats(),
    val reward: Reward = Reward(),
    val spawn: SpawnData,
    val replicas: List<String> = emptyList(),
    val weaponType: WeaponType? = null,
    val hasShield: Boolean = false,
    val ai: Ai? = null
): GameData {
    init {
        //TODO Separate classes?
        require(isEnemy xor replicas.isNotEmpty()) { "Monsters must have no replicas!" }
    }

    object Registry: GameDataRegistry<NpcTemplate>()
}

data class SpawnData(
    val respawnDelay: Long = 0,
    val positions: List<SpawnPosition>? = null,
    val zones: List<SpawnZone>? = null
) {
    init {
        require(!positions.isNullOrEmpty() || !zones.isNullOrEmpty()) {
            "Invalid spawn data. Either positions or spawn zones must be provided"
        }
    }
}

enum class NpcRace {
    HUMANS,
    FAIRIES,
}
