package org.l2kserver.game.model.skill

import org.l2kserver.game.domain.SkillEntity

class Skill(
    private val entity: SkillEntity,
    private val template: SkillTemplate
) {
    companion object

    val subclassIndex = entity.subclassIndex
    val skillId = entity.skillId
    val skillName = template.skillName
    val skillLevel by entity::skillLevel
    val skillType = template.skillType
    val targetType = template.targetType

    val reuseDelay: Int = template.reuseDelay
    val castTime = template.castTime
    val castRange = template.castRange
    val effectRange = template.effectRange

    val requires = template.requirements

    val consumables: SkillConsumables? get() = template.consumes?.toSkillConsumables()
    val effects: List<SkillEffect> get() = template.effects.map { it.toSkillEffect() }

    var nextUsageTime by entity::nextUsageTime

    private fun SkillConsumablesTemplate.toSkillConsumables() = SkillConsumables(
        mp = requireNotNull(this.mp.getOrNull(skillLevel - 1)) {
            "No data about mp consumption at skill level = '$skillLevel' found"
        }
    )

    private fun SkillEffectTemplate.toSkillEffect(): SkillEffect = when (this.effectType) {
        SkillEffectType.PHYS_DAMAGE -> PhysicalDamageEffect(
            targetType = this.targetType ?: this@Skill.targetType,
            power = requireNotNull(this.power?.getOrNull(skillLevel - 1)) {
                "No data about skill power on skill level = '$skillLevel' found"
            }
        )
    }

    override fun toString() = "Skill(id=$skillId name=$skillName level=$skillLevel)"
}

/**
 * Skill consumables
 *
 * @property mp - How much mana is spent to cast skill
 */
data class SkillConsumables(
    val mp: Int
)
