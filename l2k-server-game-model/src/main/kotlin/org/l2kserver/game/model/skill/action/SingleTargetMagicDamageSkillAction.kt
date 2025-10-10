package org.l2kserver.game.model.skill.action

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.item.template.SpiritshotType
import org.l2kserver.game.model.skill.effect.effects
import org.l2kserver.game.model.skill.effect.hit
import org.l2kserver.game.model.utils.MAGIC_ATTACK_BASE
import org.l2kserver.game.model.utils.calculateIsMagicCritical
import org.l2kserver.game.model.utils.calculateIsMagicSucceeded
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class Attribute {
    EARTH,
    WIND,
    FIRE,
    WATER,
    DARK,
    LIGHT
}

/**
 * This effect deals magic damage to single target
 *
 * @property power Array of effect power per effect level (0 based)
 * @property magicLevel Array of level of magic (to compare with target level) per effect level
 * @property attribute
 */
class SingleTargetMagicDamageSkillAction(
    val power: List<Int>,
    val magicLevel: List<Int>,
    val attribute: Attribute
): SingleTargetMagicSkillAction {

    override fun apply(
        target: ActorInstance,
        caster: ActorInstance,
        actionLevel: Int,
        usedSpiritshotType: SpiritshotType?
    ) = effects {
        //TODO https://github.com/l2k-server/l2k-server/issues/73
        val magicLevel = this@SingleTargetMagicDamageSkillAction.magicLevel.getOrNull(actionLevel - 1) ?: 0
        val power = this@SingleTargetMagicDamageSkillAction.power.getOrNull(actionLevel - 1) ?: 0

        val spiritshotMultiplier = when(usedSpiritshotType) {
            null -> 1
            SpiritshotType.SPIRITSHOT -> 2
            SpiritshotType.BLESSED_SPIRITSHOT -> 4
        }

        var damage = MAGIC_ATTACK_BASE * sqrt(caster.stats.mAtk.toDouble() * spiritshotMultiplier) * power

        damage /= target.stats.mDef

        //TODO Pvp bonus/resistance
        //TODO attribute bonus/resistance

        if (!calculateIsMagicSucceeded(target, magicLevel)) {
            if (calculateIsMagicSucceeded(target, magicLevel) && (target.level - caster.level) <= 9) {
                hit(targetId = target.id, damage = (damage / 2).roundToInt(), isHalfSuccessful = true)
            }
            else {
                hit(targetId = target.id, 1, isFailed = true)
            }
        }
        else if (calculateIsMagicCritical(caster)) {
            hit(targetId = target.id, damage = (damage * 4).roundToInt(), isMagicCritical = true)
        }
        else hit(targetId = target.id, damage.roundToInt())
    }

}
