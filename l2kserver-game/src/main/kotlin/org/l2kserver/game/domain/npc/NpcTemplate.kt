package org.l2kserver.game.domain.npc

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRootName
import java.io.File
import org.l2kserver.game.domain.item.template.WeaponType
import org.l2kserver.game.extensions.model.toPath2D
import org.l2kserver.game.model.CollisionBox
import org.l2kserver.game.model.Point
import org.l2kserver.game.model.Reward
import org.l2kserver.game.model.stats.Stats
import org.l2kserver.game.model.Zone
import org.l2kserver.game.model.position.SpawnPosition
import org.l2kserver.game.utils.GameDataLoader

@JsonRootName("npc")
data class NpcTemplate(
    val id: Int,
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
    val aiType: String? = null
) {
    init {
        //TODO Separate classes?
        require(isEnemy xor replicas.isNotEmpty()) { "Monsters must have no replicas!" }
    }

    companion object {
        private val dataStorage = GameDataLoader.scanDirectory(File("data/npc"), NpcTemplate::class.java)
            .associateBy { it.id }

        fun findById(id: Int) = dataStorage[id]

        fun all() = dataStorage.values
    }
}

data class SpawnData(
    val respawnDelay: Long = 0,
    val positions: List<SpawnPosition>?,
    val zones: List<SpawnZone>?
) {
    init {
        require(!positions.isNullOrEmpty() || !zones.isNullOrEmpty()) {
            "Invalid spawn data. Either positions or spawn zones must be provided"
        }
    }
}

class SpawnZone(
    val name: String,
    val npcAmount: Int,
    zMin: Int,
    zMax: Int,
    private val vertices: List<Point>
): Zone(zMin, zMax, vertices.toPath2D()) {

    override fun toString(): String {
        return "SpawnZone(name='$name', amount=$npcAmount, vertices=$vertices)"
    }

}

enum class NpcRace {
    @JsonProperty("Humans") HUMANS,
    @JsonProperty("Fairies") FAIRIES,
}
