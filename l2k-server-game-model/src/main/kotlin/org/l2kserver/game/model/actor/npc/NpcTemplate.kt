package org.l2kserver.game.model.actor.npc

import org.l2kserver.game.model.actor.CollisionBox
import org.l2kserver.game.model.GameData
import org.l2kserver.game.model.GameDataRegistry
import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.character.CharacterInstance
import org.l2kserver.game.model.actor.npc.ai.AiIntents
import org.l2kserver.game.model.actor.position.SpawnPosition
import org.l2kserver.game.model.reward.Reward
import org.l2kserver.game.model.stats.BasicStats
import org.l2kserver.game.model.stats.CombatStats
import org.l2kserver.game.model.zone.SpawnZone

/** Stores all the NPC templates */
object NpcTemplateRegistry: GameDataRegistry<NpcTemplate>()

interface NpcTemplate: GameData {
    override val id: Int
    val name: String
    val title: String? get() = null
    val level: Int
    val race: NpcRace
    val collisionBox: CollisionBox
    val stats: CombatStats
    val basicStats: BasicStats
    val reward: Reward? get() = null
    val spawn: SpawnData? get() = null
    val equippedWeaponTemplateId: Int? get() = null
    val equippedShieldTemplateId: Int? get() = null

    fun isEnemyOf(other: ActorInstance): Boolean

    fun onIdle(npc: NpcInstance): AiIntents? = null
    fun onTalkWith(character: CharacterInstance): String? = null
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
