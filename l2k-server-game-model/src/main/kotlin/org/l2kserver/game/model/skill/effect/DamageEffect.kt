package org.l2kserver.game.model.skill.effect

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.item.template.SpiritshotType
import org.l2kserver.game.model.item.template.calculateRandomDamageModifier
import org.l2kserver.game.model.utils.MAGIC_ATTACK_BASE
import org.l2kserver.game.model.utils.PHYSICAL_ATTACK_BASE
import org.l2kserver.game.model.utils.calculateIsMagicCritical
import org.l2kserver.game.model.utils.calculateIsMagicSucceeded
import org.l2kserver.game.model.utils.calculateIsPhysicalAttackAvoided
import org.l2kserver.game.model.utils.calculateIsPhysicalAttackBlocked
import org.l2kserver.game.model.utils.calculateIsPhysicalAttackCritical
import org.l2kserver.game.model.utils.calculatePositionCritChanceMultiplier
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Data class representing single target hit
 *
 * @property damage How many damage points has the attack dealt
 * @property isCritical is this attack critical
 * @property isBlocked is this attack blocked by shield
 * @property isAvoided is this attack avoided
 * @property isMagicCritical is this attack a magical crit
 * @property isHalfSuccessful is this attack half successful
 * @property isFailed is this attack failed
 */
data class DamageEffect(
    val targetId: Int,
    val damage: Int = 0,
    val isCritical: Boolean = false,
    val isBlocked: Boolean = false,
    val isAvoided: Boolean = false,
    val isMagicCritical: Boolean = false,
    val isHalfSuccessful: Boolean = false,
    val isFailed: Boolean = false
): Effect {
    companion object {
        /**
         * Calculates physical hit
         *
         * @param caster Actor, who casts the skill
         * @param target Target of this hit
         * @param power POwer of skill itself
         * @param usedSoulshot Was the soulshot used
         * @param ignoresShield Does this skill ignore shield defence or evasion
         */
        fun physicalHit(
            caster: ActorInstance,
            target: ActorInstance,
            power: Int,
            usedSoulshot: Boolean = false,
            ignoresShield: Boolean = false
        ): DamageEffect {
            if (!ignoresShield && calculateIsPhysicalAttackAvoided(caster, target)) {
                return DamageEffect(target.id, isAvoided = true)
            }

            //TODO Excellent shield block
            // https://github.com/orgs/l2k-server/projects/1?pane=issue&itemId=120794579&issue=l2k-server%7Cl2k-server%7C10
            val isBlocked = !ignoresShield && calculateIsPhysicalAttackBlocked(caster, target)
            val isCritical = !isBlocked && calculateIsPhysicalAttackCritical(caster, target)

            var damage = power.toDouble() + caster.stats.pAtk
            damage *= (caster.weaponType.calculateRandomDamageModifier())

            if (usedSoulshot) damage *= 2
            if (isCritical) damage = damage * 2 + caster.stats.critDamage

            var defence = target.stats.pDef
            if (isBlocked) defence += target.stats.shieldDef

            //TODO Buffs for weapon vulnerabilities/resistances, PVP bonus
            // https://github.com/orgs/l2k-server/projects/1?pane=issue&itemId=124732573&issue=l2k-server%7Cl2k-server%7C47

            damage = (PHYSICAL_ATTACK_BASE * damage) / defence

            return DamageEffect(target.id, damage.roundToInt(), isCritical, isBlocked)
        }

        /** Calculates magic hit */
        fun magicHit(
            caster: ActorInstance,
            target: ActorInstance,
            power: Int,
            magicLevel: Int,
            usedSpiritshotType: SpiritshotType? = null
        ): DamageEffect {
            val spiritshotMultiplier = when(usedSpiritshotType) {
                null -> 1
                SpiritshotType.SPIRITSHOT -> 2
                SpiritshotType.BLESSED_SPIRITSHOT -> 4
            }

            var damage = MAGIC_ATTACK_BASE * sqrt(caster.stats.mAtk.toDouble() * spiritshotMultiplier) * power

            damage /= target.stats.mDef

            //TODO Pvp bonus/resistance
            //TODO attribute bonus/resistance

            return if (!calculateIsMagicSucceeded(target, magicLevel)) {
                if (calculateIsMagicSucceeded(target, magicLevel) && (target.level - caster.level) <= 9) {
                    DamageEffect(target.id, damage = (damage / 2).roundToInt(), isHalfSuccessful = true)
                } else {
                    DamageEffect(target.id, damage = 1, isFailed = true)
                }
            }
            else if (calculateIsMagicCritical(caster)) {
                DamageEffect(targetId = target.id, damage = (damage * 4).roundToInt(), isMagicCritical = true)
            }
            else DamageEffect(targetId = target.id, damage.roundToInt())
        }

        private const val BLOW_CHANCE_FROM_BEHIND = 0.7
        private const val BLOW_CHANCE_FROM_ASIDE = 0.6
        private const val BLOW_CHANCE_FROM_THE_FRONT = 0.5

        fun blow(
            caster: ActorInstance,
            target: ActorInstance,
            power: Int,
            usedSoulshot: Boolean = false
        ): DamageEffect {
            val successChance = when {
                caster.isBehind(target) -> BLOW_CHANCE_FROM_BEHIND
                caster.isOnSideOf(target) -> BLOW_CHANCE_FROM_ASIDE
                else -> BLOW_CHANCE_FROM_THE_FRONT
            }
            val dexSuccessRateModifier = 1.0 + (caster.basicStats.dex.value.toDouble() - 20.0) / 100.0

            if (successChance * dexSuccessRateModifier < Random.nextDouble()) {
                return DamageEffect(target.id, isAvoided = true)
            }

            // Calculate damage
            var damage = (caster.stats.pAtk * if (usedSoulshot) 2 else 1).toDouble()
            damage += power * if (usedSoulshot) 1.5 else 1.0

            damage *= caster.weaponType.calculateRandomDamageModifier()
            damage *= calculatePositionCritChanceMultiplier(caster, target)
            //TODO * Critical chance percent bonus * 0.5
            damage += caster.stats.critDamage * 6

            //TODO Buffs for weapon vulnerabilities/resistances, PVP bonus
            // https://github.com/orgs/l2k-server/projects/1?pane=issue&itemId=124732573&issue=l2k-server%7Cl2k-server%7C47

            var defence = target.stats.pDef
            val isBlocked = calculateIsPhysicalAttackBlocked(caster, target)
            if (isBlocked) defence += target.stats.shieldDef

            damage = (PHYSICAL_ATTACK_BASE * damage) / defence

            return DamageEffect(target.id, damage.roundToInt(), isBlocked = isBlocked)
        }
    }
}
