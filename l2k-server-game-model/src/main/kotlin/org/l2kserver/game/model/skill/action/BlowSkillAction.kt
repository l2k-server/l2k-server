package org.l2kserver.game.model.skill.action

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.item.template.calculateRandomDamageModifier
import org.l2kserver.game.model.utils.PHYSICAL_ATTACK_BASE
import org.l2kserver.game.model.utils.calculateIsPhysicalAttackBlocked
import org.l2kserver.game.model.utils.calculatePositionCritChanceMultiplier
import kotlin.math.roundToInt
import kotlin.random.Random

private const val BLOW_CHANCE_FROM_BEHIND = 0.7
private const val BLOW_CHANCE_FROM_ASIDE = 0.6
private const val BLOW_CHANCE_FROM_THE_FRONT = 0.5

/**
 * Blow effect of dagger skill - similar to SingleTargetPhysicalDamage, but calculates differently.
 *
 * @property power Array of effect power per effect level (0 based)
 * @property lethalStrikePossible Can this blow produce Lethal Strike
 */
class BlowSkillAction(
    val power: List<Int>,
    val lethalStrikePossible: Boolean = false
): SingleTargetPhysicalSkillAction {

    override fun applyTo(target: ActorInstance, caster: ActorInstance, actionLevel: Int, usedSoulshot: Boolean) = effects {
        // Calculate blow chance
        val successChance = when {
            caster.isBehind(target) -> BLOW_CHANCE_FROM_BEHIND
            caster.isOnSideOf(target) -> BLOW_CHANCE_FROM_ASIDE
            else -> BLOW_CHANCE_FROM_THE_FRONT
        }
        val dexSuccessRateModifier = 1.0 + (caster.basicStats.dex.value.toDouble() - 20.0) / 100.0

        if (successChance * dexSuccessRateModifier < Random.nextDouble()) {
            miss()
            return@effects
        }

        // Calculate damage
        var damage = (caster.stats.pAtk * if (usedSoulshot) 2 else 1).toDouble()
        damage += power[actionLevel - 1] * if (usedSoulshot) 1.5 else 1.0

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

        hit(damage.roundToInt(), isBlocked = isBlocked)
    }

}
