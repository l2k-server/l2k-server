package org.l2kserver.game.model.skill.effect

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.item.template.calculateRandomDamageModifier
import org.l2kserver.game.model.utils.PHYSICAL_ATTACK_BASE
import org.l2kserver.game.model.utils.calculateIsBlocked
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
class BlowSkillEffect(val power: List<Int>, val lethalStrikePossible: Boolean = false): SingleTargetSkillEffect {

    override fun apply(caster: ActorInstance, target: ActorInstance, effectLevel: Int) = effects {
        // Calculate blow chance
        val successChance = when {
            caster.isBehind(target) -> BLOW_CHANCE_FROM_BEHIND
            caster.isOnSideOf(target) -> BLOW_CHANCE_FROM_ASIDE
            else -> BLOW_CHANCE_FROM_THE_FRONT
        }
        val dexSuccessRateModifier = 1.0 + (caster.basicStats.dex.value.toDouble() - 20.0) / 100.0

        if (successChance * dexSuccessRateModifier < Random.nextDouble()) return@effects

        // Calculate damage
        //TODO https://github.com/orgs/l2k-server/projects/1?pane=issue&itemId=120797806&issue=l2k-server%7Cl2k-server%7C19
        var damage = caster.stats.pAtk /* if SS used x2 */ + power[effectLevel - 1].toDouble() /* if SS used x1,5*/
        damage *= caster.weaponType.calculateRandomDamageModifier()
        damage *= calculatePositionCritChanceMultiplier(caster, target)
        //TODO * Critical chance percent bonus * 0.5
        damage += caster.stats.critDamage * 6

        //TODO Buffs for weapon vulnerabilities/resistances, PVP bonus
        // https://github.com/orgs/l2k-server/projects/1?pane=issue&itemId=124732573&issue=l2k-server%7Cl2k-server%7C47

        var defence = target.stats.pDef
        val isBlocked = calculateIsBlocked(caster, target)
        if (isBlocked) defence += target.stats.shieldDef

        damage = (PHYSICAL_ATTACK_BASE * damage) / defence

        dealDamage(damage.roundToInt(), target, isBlocked = isBlocked)
    }

}
