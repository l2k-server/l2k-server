package org.l2kserver.game.model.actor

import org.l2kserver.game.domain.AbnormalEffects
import org.l2kserver.game.extensions.model.stats.applyAbnormalsOf
import org.l2kserver.game.model.actor.npc.NpcInstance
import java.util.concurrent.ConcurrentHashMap
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.npc.NpcTemplate
import org.l2kserver.game.model.actor.npc.SpawnedAt
import org.l2kserver.game.model.actor.position.Heading
import org.l2kserver.game.model.item.template.ArmorTemplate
import org.l2kserver.game.model.item.template.ItemTemplateRegistry
import org.l2kserver.game.model.item.template.WeaponTemplate
import org.l2kserver.game.utils.IdUtils
import java.time.Instant

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
 * @property replica NPC's chat replica
 * @property collisionBox NPC's collision box
 * @property currentHp NPC's current HP
 * @property currentMp NPC's current mana
 * @property moveType NPC's current move type
 * @property hasShield Can this NPC block attacks by shield
 * @property ai AI script for this NPC
 */
class Npc(
    private val template: NpcTemplate,
    override val spawnedAt: SpawnedAt,
    override var position: Position,
    override var heading: Heading
): MutableActorInstance(), NpcInstance {

    override val id = IdUtils.getNextNpcId()
    override val name = template.name
    override val title = template.title

    override val templateId = template.id
    override val level = template.level
    override val isEnemy = template.isEnemy
    override val race = template.race

    override val stats get() = template.stats.applyAbnormalsOf(this)
    override val basicStats = template.basicStats

    override val reward = template.reward

    override var disappearanceTime: Instant? = null

    override val replica = template.replica
    override val collisionBox = template.collisionBox

    override var currentHp = template.stats.maxHp
    override var currentMp = template.stats.maxMp

    override var moveType = MoveType.WALK
    override val ai = template.ai

    override var equippedWeaponTemplate = template.equippedWeaponTemplateId?.let {
        ItemTemplateRegistry.findByIdOrNull(it) as? WeaponTemplate
    }

    override var equippedShieldTemplate: ArmorTemplate? = template.equippedShieldTemplateId?.let {
        ItemTemplateRegistry.findByIdOrNull(it) as? ArmorTemplate
    }

    /**
     * How much damage had the opponents dealt to this NPC
     *
     * Key - attackerId, Value - damage dealt
     */
    //TODO clean this map after fighting has ended
    override val opponents = ConcurrentHashMap<ActorInstance, Int>(0)

    override val isImmobilized: Boolean get() = isParalyzed //TODO check if rooted, stunned, paralyzed, casting, etc...
    override val isParalyzed: Boolean get() = false

    override fun isEnemyOf(other: ActorInstance): Boolean = isEnemy

    override var isFighting = false
    override var isMoving = false

    override var targetId: Int? = null
    override val targetedBy: MutableSet<ActorInstance> = ConcurrentHashMap.newKeySet(0)
    override val abnormalEffects = AbnormalEffects()

    override val weaponType = equippedWeaponTemplate?.type
    override val hasShield = equippedShieldTemplate != null

    override fun toString() = "Npc(name=$name id=$id)"
}
