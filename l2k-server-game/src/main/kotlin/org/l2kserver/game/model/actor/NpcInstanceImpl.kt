package org.l2kserver.game.model.actor

import org.l2kserver.game.domain.TemporalEffects
import org.l2kserver.game.extensions.model.stats.applyAbnormalsOf
import org.l2kserver.game.model.actor.character.PlayerCharacterInstance
import org.l2kserver.game.model.actor.npc.NpcInstance
import java.util.concurrent.ConcurrentHashMap
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.npc.Npc
import org.l2kserver.game.model.actor.npc.SpawnedAt
import org.l2kserver.game.model.actor.position.Heading
import org.l2kserver.game.model.item.Armor
import org.l2kserver.game.model.item.ItemRegistry
import org.l2kserver.game.model.item.Weapon
import org.l2kserver.game.utils.IdUtils
import kotlin.math.roundToInt

/**
 * NPC data
 *
 * @property id NPC identifier
 * @property name NPC name
 * @property templateId NPC template ID
 * @property level NPC level
 * @property title NPC title
 * @property race NPC's race
 * @property heading NPC's heading direction
 * @property position NPC's position in world
 * @property stats NPC's stats
 * @property reward Reward for killing this NPC
 * @property spawnedAt Where was this NPC spawned (position or zone)
 * @property collisionBox NPC's collision box
 * @property currentHp NPC's current HP
 * @property currentMp NPC's current mana
 * @property moveType NPC's current move type
 * @property hasShield Can this NPC block attacks by shield
 */
class NpcInstanceImpl(
    private val template: Npc,
    override val spawnedAt: SpawnedAt,
    override var position: Position,
    override var heading: Heading
): MutableActorInstance(), NpcInstance {
    override val id = IdUtils.getNextNpcId()
    override val name = template.name
    override val title = template.title

    override val templateId = template.id
    override val level = template.level
    override val race = template.race

    override val stats get() = template.stats.applyAbnormalsOf(this)
    override val basicStats = template.basicStats

    override val reward = template.reward

    override val collisionBox = template.collisionBox

    override var currentHp = template.stats.maxHp.roundToInt()
    override var currentMp = template.stats.maxMp.roundToInt()

    override var moveType = MoveType.WALK

    override var equippedWeaponTemplate = template.equippedWeaponTemplateId?.let {
        ItemRegistry.findByIdOrNull(it) as? Weapon
    }

    override var equippedShieldTemplate: Armor? = template.equippedShieldTemplateId?.let {
        ItemRegistry.findByIdOrNull(it) as? Armor
    }

    /**
     * How much damage had the opponents dealt to this NPC
     *
     * Key - attackerId, Value - damage dealt
     */
    //TODO clean this map after fighting has ended
    override val opponents = ConcurrentHashMap<ActorInstance, Int>(0)
    override var overhitDamage = 0

    override val isImmobilized: Boolean get() = isParalyzed //TODO check if rooted, stunned, paralyzed, casting, etc...
    override val isParalyzed: Boolean get() = false

    override var isFighting = false
    override var isMoving = false

    override var targetId: Int? = null
    override val targetedBy: MutableSet<ActorInstance> = ConcurrentHashMap.newKeySet(0)
    override val temporalEffects = TemporalEffects()

    override val weaponType = equippedWeaponTemplate?.type
    override val hasShield = equippedShieldTemplate != null

    override fun toString() = "Npc(name=$name id=$id)"

    override fun isEnemyOf(other: ActorInstance): Boolean = template.isEnemyOf(other)
    override fun onTalkWith(character: PlayerCharacterInstance) = template.onTalkWith(character)
    override fun onIdle() = template.onIdle(this)
}
