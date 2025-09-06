package org.l2kserver.game.model.skill

import org.l2kserver.game.domain.SkillEntity

/**
 * Skill instance
 *
 * @property skillId Skill identifier
 * @property skillName Skill name (eng)
 * @property skillLevel Level of skill
 * @property skillType Skill type - active, passive or toggle
 * @property reuseDelay Base cooldown of this skill
 * @property castTime Base casting time of this skill
 * @property repriseTime Time to return to the starting position after skill casting
 * @property castRange Range to target to cast this skill, or radius for mass skill
 * @property effectRange TODO
 * @property requires Requirements to use this skill
 * @property consumes Skill consumables - mp, items, etc.
 * @property skillAction Effects, dealt by this skill
 */
class Skill(
    private val entity: SkillEntity,
    private val template: SkillTemplate
): SkillInstance {
    companion object;

    override val skillId = entity.skillId
    override val skillName = template.skillName
    override val skillLevel by entity::skillLevel
    override val skillType = template.skillType
    override val targetType = template.targetType

    override val reuseDelay: Int = template.reuseDelay
    override val castTime = template.castTime
    override val repriseTime = template.repriseTime
    override val castRange = template.castRange
    override val effectRange = template.effectRange
    override val requires = template.requires
    override val consumes: SkillConsumables? get() = template.consumes?.toSkillConsumables()
    override val skillAction = template.skillAction

    var nextUsageTime by entity::nextUsageTime

    private fun SkillConsumablesTemplate.toSkillConsumables() = SkillConsumables(
        hp  = this.hp?.let { requireNotNull(it.getOrNull(skillLevel - 1)) {
            "No data about hp consumption at skill level = '$skillLevel' found"
        }} ?: 0,
        mp = this.mp?.let { requireNotNull(it.getOrNull(skillLevel - 1)) {
            "No data about mp consumption at skill level = '$skillLevel' found"
        }} ?: 0,
        item = this.item?.let { requireNotNull(it.getOrNull(skillLevel - 1)) {
            "No data about item consumption at skill level = '$skillLevel' found"
        }},
    )

    override fun toString() = "Skill(id=$skillId name=$skillName level=$skillLevel)"
}
