package org.l2kserver.game.data.skill

import org.l2kserver.game.model.skill.SkillConsumablesTemplate
import org.l2kserver.game.model.skill.SkillTargetType
import org.l2kserver.game.model.skill.SkillTemplate
import org.l2kserver.game.model.skill.SkillType
import org.l2kserver.game.model.skill.action.SingleTargetMagicDamageSkillAction
import org.l2kserver.game.model.stats.Attribute

val WIND_STRIKE = SkillTemplate(
    id = 1177,
    skillName = "Wind Strike",
    skillType = SkillType.MAGIC,
    targetType = SkillTargetType.ENEMY,
    reuseDelay = 6000,
    castTime = 4000,
    repriseTime = 800,
    castRange = 600,
    effectRange = 1100,
    maxSkillLevel = 5,
    consumes = SkillConsumablesTemplate(
        mpToStart = listOf(2, 2, 2, 3, 3),
        mp = listOf(7, 7, 8, 11, 12)
    ),
    skillAction = SingleTargetMagicDamageSkillAction(
        power = listOf(12, 13, 15, 18, 21),
        magicLevel = listOf(1, 4, 7, 11, 14),
        attribute = Attribute.WIND,
        attributeValue = 20
    )
)
