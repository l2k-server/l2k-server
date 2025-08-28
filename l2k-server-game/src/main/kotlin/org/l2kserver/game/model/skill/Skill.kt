package org.l2kserver.game.model.skill

import org.l2kserver.game.domain.SkillEntity
import org.l2kserver.game.model.item.ConsumableItem

/**
 * Skill instance
 *
 * @property skillId Skill identifier
 * @property skillName Skill name (eng)
 * @property skillType - Skill type - active, passive or toggle
 * @property reuseDelay - Base cooldown of this skill
 * @property castTime - Base casting time of this skill
 * @property repriseTime - Time to return to the starting position after skill casting
 * @property castRange - Range to target to cast this skill, or radius for mass skill
 * @property effectRange - TODO
 * @property requires - Requirements to use this skill
 * @property consumes - Skill consumables - mp, items, etc.
 * @property effects - Effects, dealt by this skill
 */
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
    val repriseTime = template.repriseTime
    val castRange = template.castRange
    val effectRange = template.effectRange

    val requires = template.requires

    val consumes: SkillConsumables? get() = template.consumes?.toSkillConsumables()
    val effects = template.effects

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

    fun castsOnCorpse() = targetType == SkillTargetType.DEAD_ENEMY || targetType == SkillTargetType.DEAD_FRIEND
}

/**
 * Skill consumables
 *
 * @property mp - How much mana is spent to cast skill
 */
data class SkillConsumables(
    val hp: Int,
    val mp: Int,
    val item: ConsumableItem?
)
