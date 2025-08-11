package org.l2kserver.game.model.skill

/**
 * Effect dealt by skill
 *
 * @property targetType Target that the effect is applied to - Target, Self, Party, etc.
 */
sealed interface SkillEffect {
    val targetType: SkillTargetType
}

/**
 * This effect inflicts physical damage to it's targets
 *
 * @property targetType Target that the effect is applied to - Target, Self, Party, etc.
 * @property power Skill power
 */
data class PhysicalDamageEffect(
    override val targetType: SkillTargetType,
    val power: Int
): SkillEffect
