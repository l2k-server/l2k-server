package org.l2kserver.game.model.skill.action

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.item.template.calculateRandomDamageModifier
import org.l2kserver.game.model.skill.effect.effects
import org.l2kserver.game.model.skill.effect.hit
import org.l2kserver.game.model.skill.effect.miss
import org.l2kserver.game.model.utils.PHYSICAL_ATTACK_BASE
import org.l2kserver.game.model.utils.calculateIsPhysicalAttackAvoided
import org.l2kserver.game.model.utils.calculateIsPhysicalAttackBlocked
import org.l2kserver.game.model.utils.calculateIsPhysicalAttackCritical
import kotlin.math.roundToInt

/**
 * This effect deals physical damage to single target
 *
 * @property power Array of effect power per effect level (0 based)
 * @property ignoresShield Does this effect ignore shield or evasion
 */
class SingleTargetPhysicalDamageSkillAction(
    val power: List<Int>,
    val ignoresShield: Boolean = false
): SingleTargetPhysicalSkillAction {

    override fun applyTo(target: ActorInstance, caster: ActorInstance, actionLevel: Int, usedSoulshot: Boolean) = effects {
        if (!ignoresShield && calculateIsPhysicalAttackAvoided(caster, target)) {
            miss()
            return@effects
        }

        //TODO Excellent shield block
        // https://github.com/orgs/l2k-server/projects/1?pane=issue&itemId=120794579&issue=l2k-server%7Cl2k-server%7C10
        val isBlocked = !ignoresShield && calculateIsPhysicalAttackBlocked(caster, target)
        val isCritical = !isBlocked && calculateIsPhysicalAttackCritical(caster, target)

        var damage = ((power.getOrNull(actionLevel - 1) ?: 0) + caster.stats.pAtk).toDouble()
        damage *= (caster.weaponType.calculateRandomDamageModifier())

        if (usedSoulshot) damage *= 2
        if (isCritical) damage = damage * 2 + caster.stats.critDamage

        var defence = target.stats.pDef
        if (isBlocked) defence += target.stats.shieldDef

        //TODO Buffs for weapon vulnerabilities/resistances, PVP bonus
        // https://github.com/orgs/l2k-server/projects/1?pane=issue&itemId=124732573&issue=l2k-server%7Cl2k-server%7C47

        damage = (PHYSICAL_ATTACK_BASE * damage) / defence

        hit(damage.roundToInt(), isCritical, isBlocked)
    }

}
