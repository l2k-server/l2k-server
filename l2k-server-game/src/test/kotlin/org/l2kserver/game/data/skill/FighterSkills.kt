package org.l2kserver.game.data.skill

import org.l2kserver.game.model.skill.action.SingleTargetPhysicalDamageSkillAction
import org.l2kserver.game.model.item.template.WeaponType
import org.l2kserver.game.model.skill.SkillConsumablesTemplate
import org.l2kserver.game.model.skill.SkillRequirements
import org.l2kserver.game.model.skill.SkillTargetType
import org.l2kserver.game.model.skill.SkillTemplate
import org.l2kserver.game.model.skill.SkillType
import org.l2kserver.game.model.skill.action.BlowSkillAction

val POWER_STRIKE = SkillTemplate(
    id = 3,
    skillName = "Power strike",
    skillType = SkillType.ACTIVE,
    targetType = SkillTargetType.ENEMY,
    reuseDelay = 13_000,
    castTime = 1_080,
    repriseTime = 720,
    castRange = 40,
    effectRange = 400,
    requires = SkillRequirements(
        weaponTypes = listOf(
            WeaponType.SWORD_ONE_HANDED,
            WeaponType.SWORD_TWO_HANDED,
            WeaponType.BLUNT_ONE_HANDED,
            WeaponType.BLUNT_TWO_HANDED
        )
    ),
    maxSkillLevel = 9,
    consumes = SkillConsumablesTemplate(
        mp = listOf(10, 10, 11, 13, 13, 14, 17, 18, 19)
    ),
    skillAction = SingleTargetPhysicalDamageSkillAction(
        power = listOf(25, 27, 30, 39, 42, 46, 60, 65, 70),
        ignoresShield = true,
        overhitPossible = true
    )
)

val MORTAL_BLOW = SkillTemplate(
    id = 16,
    skillName = "Mortal Blow",
    skillType = SkillType.ACTIVE,
    targetType = SkillTargetType.ENEMY,
    reuseDelay = 11_000,
    castTime = 1_080,
    repriseTime = 720,
    castRange = 40,
    effectRange = 400,
    requires = SkillRequirements(
        weaponTypes = listOf(WeaponType.DAGGER)
    ),
    maxSkillLevel = 24,
    consumes = SkillConsumablesTemplate(
        mp = listOf(9,9,10,11,12,13,16,16,17,19,20,20,21,22,23,25,26,27,28,28,29,32,33,34)
    ),
    skillAction = BlowSkillAction(
        power = listOf(73,80,88,115,126,137,178,193,210,268,291,314,367,396,427,494,531,571,656,703,752,859,916,977)
        //TODO Lethal effect?
    )
)

val POWER_SHOT = SkillTemplate(
    id = 56,
    skillName = "Power Shot",
    skillType = SkillType.ACTIVE,
    targetType = SkillTargetType.ENEMY,
    reuseDelay = 25_000,
    castTime = 3_200,
    repriseTime = 800,
    castRange = 700,
    effectRange = 1200,
    requires = SkillRequirements(
        weaponTypes = listOf(WeaponType.BOW)
    ),
    maxSkillLevel = 24,
    consumes = SkillConsumablesTemplate(
        mp = listOf(17, 18, 19, 22, 23, 25, 31, 32, 34, 38, 39, 40, 42, 43, 45, 49, 51, 53, 56, 56, 58, 63, 65, 67)
    ),
    skillAction = SingleTargetPhysicalDamageSkillAction(
        power = listOf(
            65, 71, 78, 102, 112, 122, 158, 172, 187, 239, 258, 279, 326, 352, 379, 440, 472, 507, 584, 625, 669, 763, 814, 865
        ),
        ignoresShield = true
    )
)
