@file:Suppress("MatchingDeclarationName")
package org.l2kserver.game.data.skill

import org.l2kserver.game.model.item.template.WeaponType
import org.l2kserver.game.model.skill.context.SkillContext
import org.l2kserver.game.model.skill.effect.DamageEffect
import org.l2kserver.game.model.skill.effect.Effects
import org.l2kserver.game.model.skill.instance.SkillTargetType
import org.l2kserver.game.model.skill.template.ActiveSkillTemplate
import org.l2kserver.game.model.skill.template.SkillConsumablesTemplate
import org.l2kserver.game.model.skill.template.SkillRequirements

object PowerStrike: ActiveSkillTemplate() {
    override val id = 3
    override val skillName = "Power strike"
    override val maxLevel = 9
    override val targetType = SkillTargetType.ENEMY
    override val reuseDelay = 13_000
    override val castTime = 1_080
    override val repriseTime = 720
    override val castRange = 40
    override val effectRange = 400
    override val requires = SkillRequirements(
        weaponTypes = listOf(
            WeaponType.SWORD_ONE_HANDED,
            WeaponType.SWORD_TWO_HANDED,
            WeaponType.BLUNT_ONE_HANDED,
            WeaponType.BLUNT_TWO_HANDED
        )
    )
    override val consumes = SkillConsumablesTemplate(
        mp = listOf(10, 10, 11, 13, 13, 14, 17, 18, 19)
    )
    override val overhitPossible = true

    val power = listOf(25, 27, 30, 39, 42, 46, 60, 65, 70)

    override fun affect(context: SkillContext) = Effects(
        DamageEffect.physicalHit(
            caster = context.caster,
            target = context.mainTarget,
            power = power.getOrElse(context.skillLevel - 1) {
                error("Skill $skillName does not have level ${context.skillLevel}")
            },
            usedSoulshot = context.usedSoulshot,
            ignoresShield = true
        )
    )
}
