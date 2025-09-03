package org.l2kserver.game.model.skill.effect

import org.l2kserver.game.model.actor.Actor
import org.l2kserver.game.model.item.template.calculateRandomDamageModifier
import org.l2kserver.game.model.utils.PHYSICAL_ATTACK_BASE
import org.l2kserver.game.model.utils.calculateIsAvoided
import org.l2kserver.game.model.utils.calculateIsBlocked
import org.l2kserver.game.model.utils.calculateIsCritical
import kotlin.math.roundToInt

/**
 * This effect deals physical damage to single target
 *
 * @property power Array of effect power per effect level (0 based)
 * @property ignoresShield Does this effect ignore shield or evasion
 * @property overhitPossible Can this effect produce an over-hit
 */
class SingleTargetPhysicalDamageSkillEffect(
    val power: List<Int>,
    val ignoresShield: Boolean = false,
    val overhitPossible: Boolean = false
): SingleTargetSkillEffect {

    override fun apply(caster: Actor, target: Actor, effectLevel: Int) = effects {
        if (!ignoresShield && calculateIsAvoided(caster, target)) {
            miss(target)
            return@effects
        }
        //TODO Excellent shield block
        // https://github.com/orgs/l2k-server/projects/1?pane=issue&itemId=120794579&issue=l2k-server%7Cl2k-server%7C10
        val isBlocked = !ignoresShield && calculateIsBlocked(caster, target)
        val isCritical = !isBlocked && calculateIsCritical(caster, target)

        var damage = ((power.getOrNull(effectLevel - 1) ?: 0) + caster.stats.pAtk).toDouble()
        damage *= (caster.weaponType.calculateRandomDamageModifier())

        //TODO if used soulshot damage *= 2
        // https://github.com/orgs/l2k-server/projects/1?pane=issue&itemId=120797806&issue=l2k-server%7Cl2k-server%7C19
        if (isCritical) damage = damage * 2 + caster.stats.critDamage

        var defence = target.stats.pDef
        if (isBlocked) defence += target.stats.shieldDef

        //TODO Buffs for weapon vulnerabilities/resistances, PVP bonus
        // https://github.com/orgs/l2k-server/projects/1?pane=issue&itemId=124732573&issue=l2k-server%7Cl2k-server%7C47

        damage = (PHYSICAL_ATTACK_BASE * damage) / defence

        dealDamage(damage.roundToInt(), target, isCritical, isBlocked, overhitPossible)
    }

}
