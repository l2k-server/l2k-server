package org.l2kserver.game.model.skill.action

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.stats.Attribute
import org.l2kserver.game.model.utils.MAGIC_ATTACK_BASE
import org.l2kserver.game.model.utils.calculateIsMagicCritical
import org.l2kserver.game.model.utils.calculateIsMagicSucceeded
import kotlin.math.roundToInt
import kotlin.math.sqrt

class SingleTargetMagicDamageSkillAction(
    val power: List<Int>,
    val magicLevel: List<Int>,
    val attribute: Attribute,
    val attributeValue: Int
): SingleTargetMagicSkillAction {

    override fun applyTo(target: ActorInstance, caster: ActorInstance, actionLevel: Int) = effects {
        //TODO https://github.com/l2k-server/l2k-server/issues/73
        val magicLevel = this@SingleTargetMagicDamageSkillAction.magicLevel.getOrNull(actionLevel - 1) ?: 0
        val power = this@SingleTargetMagicDamageSkillAction.power.getOrNull(actionLevel - 1) ?: 0

        var damage = MAGIC_ATTACK_BASE * sqrt(caster.stats.mAtk.toDouble() /* TODO * Spiritshot */) * power

        damage /= target.stats.mDef

        //TODO Pvp bonus/resistance
        //TODO attribute bonus/resistance

        if (!calculateIsMagicSucceeded(target, magicLevel)) {
            if (calculateIsMagicSucceeded(target, magicLevel) && (target.level - caster.level) <= 9) {
                hit(damage = (damage / 2).roundToInt(), isHalfSuccessful = true)
            }
            else {
                hit(1, isFailed = true)
            }
        }
        else if (calculateIsMagicCritical(caster)) {
            hit(damage = (damage * 4).roundToInt(), isMagicCritical = true)
        }
        else hit(damage.roundToInt())
    }

}
