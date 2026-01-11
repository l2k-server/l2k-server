package org.l2kserver.game.model.actor.npc

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.CollisionBox
import org.l2kserver.game.model.actor.MoveType
import org.l2kserver.game.model.actor.character.PlayerCharacterInstance
import org.l2kserver.game.model.actor.position.Heading
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.position.SpawnPosition
import org.l2kserver.game.model.actor.npc.ai.AiIntents
import org.l2kserver.game.model.item.template.Armor
import org.l2kserver.game.model.item.template.Weapon
import org.l2kserver.game.model.item.template.WeaponType
import org.l2kserver.game.model.reward.Reward
import org.l2kserver.game.model.stats.BasicStats
import org.l2kserver.game.model.stats.CombatStats
import org.l2kserver.game.model.zone.SpawnZone

/**
 * Non-player character
 *
 * @property opponents Map of characters, who fights with his NPC to their damage dealt to this NPC
 */
interface NpcInstance: ActorInstance {
    override val id: Int
    override val name: String
    val title: String?

    val templateId: Int
    override val level: Int
    val race: NpcRace

    override val heading: Heading
    override val position: Position

    override val stats: CombatStats
    override val basicStats: BasicStats

    val reward: Reward?
    val spawnedAt: SpawnedAt

    override val collisionBox: CollisionBox
    override val currentHp: Int
    override val currentMp: Int
    override val moveType: MoveType
    val equippedWeaponTemplate: Weapon? get() = null
    val equippedShieldTemplate: Armor? get() = null
    val opponents: Map<ActorInstance, Int> //TODO State Machine

    override val weaponType: WeaponType? get() = equippedWeaponTemplate?.type
    override val hasShield: Boolean get() = equippedShieldTemplate != null

    fun onIdle(): AiIntents? = null
    fun onTalkWith(character: PlayerCharacterInstance): String? = null
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
