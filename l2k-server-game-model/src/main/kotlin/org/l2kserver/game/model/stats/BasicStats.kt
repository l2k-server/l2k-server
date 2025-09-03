package org.l2kserver.game.model.stats

import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Character's basic stats
 */
data class BasicStats(
    val str: STR = STR(0),
    val dex: DEX = DEX(0),
    val con: CON = CON(0),
    val int: INT = INT(0),
    val wit: WIT = WIT(0),
    val men: MEN = MEN(0),
) {

    operator fun plus(other: BasicStats?) = if (other == null) this else BasicStats(
        str = this.str + other.str,
        dex = this.dex + other.dex,
        con = this.con + other.con,
        int = this.int + other.int,
        wit = this.wit + other.wit,
        men = this.men + other.men
    )

    override fun toString(): String {
        val statList = buildList {
            if (str.value != 0) add("str=${str.value}")
            if (dex.value != 0) add("dex=${dex.value}")
            if (con.value != 0) add("con=${con.value}")
            if (int.value != 0) add("int=${int.value}")
            if (wit.value != 0) add("wit=${wit.value}")
            if (men.value != 0) add("men=${men.value}")
        }

        return with(StringBuilder()) {
            append("BasicStats(")
            append(statList.joinToString(","))
            append(")")
        }.toString()
    }
}

sealed class BaseStat(modifierBase: Double, modifierPowBase: Double) {

    private val modifiers = Array(100) { index ->
        floor(modifierBase.pow(index - modifierPowBase) * 100 + 0.5) / 100
    }

    /**
     * @param statValue Base stat value
     * @return the multiplier for the given base stat value
     */
    protected fun getModifier(statValue: Int) = modifiers[statValue.coerceIn(modifiers.indices)]
}

private const val STR_MULTIPLIER_BASE = 1.036
private const val STR_MULTIPLIER_POW_BASE = 34.845

/**
 * Increases p.atk. Each +STR from can increase it by as little as +2%, or by as much as +6%,
 * depending on how high your STR is. The higher it is, the greater the bonus from each point.
 */
@JvmInline
value class STR(val value: Int) {
    companion object: BaseStat(STR_MULTIPLIER_BASE, STR_MULTIPLIER_POW_BASE)

    val pAtkModifier: Double get() = getModifier(value)

    operator fun plus(other: STR) = STR(this.value + other.value)
}

private const val DEX_MULTIPLIER_BASE = 1.009
private const val DEX_MULTIPLIER_POW_BASE = 19.36

/**
 * Increases attack speed, run speed, evasion, accuracy, critical chance, and shield block rate.
 * Most of these stats will increase by 1% for each point, aside from accuracy and evasion,
 * which are usually less.
 */
@JvmInline
value class DEX(val value: Int) {
    companion object: BaseStat(DEX_MULTIPLIER_BASE, DEX_MULTIPLIER_POW_BASE)

    val atkSpdModifier: Double get() = getModifier(value)
    val speedModifier: Double get() = getModifier(value)
    val shieldBlockRateModifier: Double get() = getModifier(value)
    val critRateModifier: Double get() = getModifier(value)

    val accuracyBonus: Int get() = (sqrt(value.toDouble()) * 6).toInt()
    val evasionBonus: Int get() = (sqrt(value.toDouble()) * 6).toInt()

    operator fun plus(other: DEX) = DEX(this.value + other.value)
}

/**
 * Increases maximum CP and HP, weight capacity, HP regen, underwater breath,
 * stun resistance, and poison/bleed resistance. Just like STR, each point in CON will
 * increase your HP/CP by +2-6%, depending on how high it is. If you have 40 CON, that's a 1.44 modifier
 * to your HP, or +44. Increasing it by +4, would give you a 1.62 modifier, or +18.
 * Again, just like STR, the higher your CON is, the greater the benefits (or losses) from dyes.
 */
@JvmInline
value class CON(val value: Int) {

    companion object: BaseStat(modifierBase = 1.03, modifierPowBase = 27.632) {
        private const val MAX_WEIGHT_BASE = 1.029993928
        private const val MAX_WEIGHT_MULTIPLIER = 30495.627366
    }

    val hpModifier: Double get() = getModifier(value)

    val hpRegenModifier: Double get() = getModifier(value)

    val cpModifier: Double get() = getModifier(value)

    val cpRegenModifier: Double get() = getModifier(value)

    val baseMaxWeight: Int get() = (MAX_WEIGHT_BASE.pow(value) * MAX_WEIGHT_MULTIPLIER).toInt()

    operator fun plus(other: CON) = CON(this.value + other.value)
}

/**
 * Increases m.atk and debuff land rate. Unlike STR/CON, the effect these modifiers have on your character,
 * are squared. In other words, whatever the modifier value is in the chart, you multiply by itself,
 * and that is the actual effect. So if you have 40 INT, the modifier is 1.19,
 * which you multiply by itself (1.19 x 1.19), which is 1.41, or +41% modifier.
 * If you added +4 through dyes, then the modifier would be 1.28, which when squared is 1.63,
 * giving you a +22% higher bonus.
 */
@JvmInline
value class INT(val value: Int) {

    companion object: BaseStat(modifierBase = 1.02, modifierPowBase = 31.375)

    val mAtkModifier: Double get() = getModifier(value)
    val debuffSuccessBonus: Int get() = TODO()

    operator fun plus(other: INT) = INT(this.value + other.value)
}

/**
 * Increases casting speed and magical critical chance.
 * You'll see about a 5 to 6% casting speed increase for each point, for all mage classes.
 */
@JvmInline
value class WIT(val value: Int) {

    companion object: BaseStat(modifierBase = 1.05, modifierPowBase = 20.0)

    val castingSpdModifier: Double get() = getModifier(value)
    val magicCritChanceBonus: Int get() = TODO()

    operator fun plus(other: WIT) = WIT(this.value + other.value)
}

/**
 * MEN - Increases m.def, maximum MP, and decreases magic interruption chance.
 * It also helps against debuffs due to the increased m.def.
 */
@JvmInline
value class MEN(val value: Int) {

    companion object: BaseStat(modifierBase = 1.01, modifierPowBase = -0.060)

    val mpModifier: Double get() = getModifier(value)
    val mpRegenModifier: Double get() = getModifier(value)
    val mDefModifier: Double get() = getModifier(value)

    operator fun plus(other: MEN) = MEN(this.value + other.value)
}
