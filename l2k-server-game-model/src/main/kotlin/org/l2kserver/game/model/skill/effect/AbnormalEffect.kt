package org.l2kserver.game.model.skill.effect

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.skill.context.SkillContext
import org.l2kserver.game.model.stats.BasicStat
import org.l2kserver.game.model.stats.BasicStats
import org.l2kserver.game.model.stats.CombatStats
import org.l2kserver.game.model.stats.CombatStatsMultipliers
import org.l2kserver.game.model.stats.TradeAndInventoryStats
import java.time.Duration
import java.time.Instant

/** Type of abnormal effect. Character can obtain only one effect of each type */
object AbnormalType {
    /** Increases physical defense*/
    const val PD_UP = "pd_up"

    /** Increases physical attack*/
    const val PA_UP = "pa_up"

    /** Increases accuracy */
    const val HIT_UP = "hit_up"

    /** Dot heal (from potions and Greater Heal) */
    const val HP_RECOVER = "hp_recover"

    /** Poison */
    const val POISON = "poison"
}

/** Visible abnormal effects, like stun, root, poison, etc. */
enum class AbnormalVisualEffect(val bit: Int) {
    BLEED(bit  = 0b00000000_00000000_00000000_00000001),
    POISON(bit = 0b00000000_00000000_00000000_00000010);
}

/**
 * Abnormal (buffs, debuffs, passives) - effect, that changes actor stats, add triggers, etc.
 *
 * @property abnormalType - Type of this abnormal. Only one abnormal of each type can affect
 */
interface AbnormalEffect {
    val abnormalType: String
    val abnormalVisualEffect: AbnormalVisualEffect? get() = null

    /** Fixed bonus combat stats, provided by this abnormal ('diff')*/
    fun getFixedBonusStats(actor: ActorInstance): CombatStats? = null

    /** Combat stats multipliers, provided by this abnormal ('per')*/
    fun getCombatStatsMultipliers(actor: ActorInstance): CombatStatsMultipliers? = null

    /** Basic stats, provided by this abnormal */
    fun getBonusBasicStats(actor: ActorInstance): BasicStats? = null

    /** Trade and inventory stats, provided by this abnormal */
    fun getBonusTradeAndInventoryStats(actor: ActorInstance): TradeAndInventoryStats? = null
}

/**
 * Skill, that provides negative temporal abnormal effect
 *
 * @property magicLevel Magic level of skill
 * @property basicProperty Basic property, that provides resistance to this skill
 * @property levelBonusRate How much does level difference affects success chance
 * @property activateRate Basic chance of this debuff. Must be from 0 to 100
 */
interface Debuff: AbnormalEffect {
    val magicLevel: Int
    val basicProperty: BasicStat
    val levelBonusRate: Double
    val activateRate: Int
}

/**
 * Temporal abnormal effect (buff and debuff effect)
 *
 * @property targetId Target of this effect
 * @property effectLevel level of this effect
 * @property skillId Identifier of skill, that produced this effect
 * @property expiresAt Time of this effect's expiration
 */
abstract class TemporalAbnormalEffect(duration: Duration): AbnormalEffect, Effect {
    abstract val effectLevel: Int
    abstract val skillId: Int

    val expiresAt: Instant = Instant.now() + duration

    override fun toString() = "${this::class.simpleName}(" +
            "skillId=$skillId, " +
            "effectLevel=$effectLevel, " +
            "targetId=$targetId, " +
            "expiresAt=$expiresAt, " +
            "abnormalType=$abnormalType)"
}

/**
 * Effect on time abnormal - produces some [effects] on target with provided [frequency]
 *
 * @property frequency How often should the [effects] be applied (in millis). Default: 3000
 */
abstract class EffectOnTimeAbnormalEffect(duration: Duration) : TemporalAbnormalEffect(duration) {
    open val frequency = 3000L
    abstract fun effects(context: SkillContext): Iterable<Effect>
}
