@file:Suppress("MatchingDeclarationName")
package org.l2kserver.game.data.skill

import org.l2kserver.game.model.item.WeaponType
import org.l2kserver.game.model.skill.context.SkillContext
import org.l2kserver.game.model.skill.effect.DamageEffect
import org.l2kserver.game.model.skill.instance.SkillTargetType
import org.l2kserver.game.model.skill.template.ActiveSkill
import org.l2kserver.game.model.skill.template.SkillConsumablesTemplate
import org.l2kserver.game.model.skill.template.SkillRequirements

data object PowerShot: ActiveSkill {
    override val id = 56
    override val skillName = "Power Shot"
    override val maxLevel = 24
    override val targetType = SkillTargetType.ENEMY
    override val reuseDelay = 25_000
    override val castTime = 3_200
    override val isMagic = false
    override val repriseTime = 800
    override val castRange = 700
    override val effectRange = 1200
    override val requires = SkillRequirements(weaponTypes = listOf(WeaponType.BOW))
    override val consumes = SkillConsumablesTemplate(
        mp = listOf(17, 18, 19, 22, 23, 25, 31, 32, 34, 38, 39, 40, 42, 43, 45, 49, 51, 53, 56, 56, 58, 63, 65, 67)
    )

    val power = listOf(
        65, 71, 78, 102, 112, 122, 158, 172, 187, 239,
        258, 279, 326, 352, 379, 440, 472, 507, 584, 625,
        669, 763, 814, 865
    )

    override fun affect(context: SkillContext) = listOf(
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
