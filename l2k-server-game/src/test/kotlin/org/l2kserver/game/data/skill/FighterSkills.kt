package org.l2kserver.game.data.skill

import org.l2kserver.game.model.skill.action.SingleTargetPhysicalDamageSkillAction
import org.l2kserver.game.model.item.template.WeaponType
import org.l2kserver.game.model.skill.template.SkillConsumablesTemplate
import org.l2kserver.game.model.skill.template.SkillRequirements
import org.l2kserver.game.model.skill.instance.SkillTargetType
import org.l2kserver.game.model.skill.template.ActiveSkillTemplate
import org.l2kserver.game.model.skill.instance.ActiveSkillType
import org.l2kserver.game.model.skill.action.BlowSkillAction
import org.l2kserver.game.model.skill.action.CorpseDrainSkillAction

val POWER_STRIKE = ActiveSkillTemplate(
    id = 3,
    skillName = "Power strike",
    maxLevel = 9,
    skillType = ActiveSkillType.ACTIVE,
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
    consumes = SkillConsumablesTemplate(
        mp = listOf(10, 10, 11, 13, 13, 14, 17, 18, 19)
    ),
    overhitPossible = true,
    skillAction = SingleTargetPhysicalDamageSkillAction(
        power = listOf(25, 27, 30, 39, 42, 46, 60, 65, 70),
        ignoresShield = true
    )
)

val MORTAL_BLOW = ActiveSkillTemplate(
    id = 16,
    skillName = "Mortal Blow",
    maxLevel = 24,
    skillType = ActiveSkillType.ACTIVE,
    targetType = SkillTargetType.ENEMY,
    reuseDelay = 11_000,
    castTime = 1_080,
    repriseTime = 720,
    castRange = 40,
    effectRange = 400,
    requires = SkillRequirements(
        weaponTypes = listOf(WeaponType.DAGGER)
    ),
    consumes = SkillConsumablesTemplate(
        mp = listOf(9,9,10,11,12,13,16,16,17,19,20,20,21,22,23,25,26,27,28,28,29,32,33,34)
    ),
    skillAction = BlowSkillAction(
        power = listOf(73,80,88,115,126,137,178,193,210,268,291,314,367,396,427,494,531,571,656,703,752,859,916,977)
        //TODO Lethal effect?
    )
)

val POWER_SHOT = ActiveSkillTemplate(
    id = 56,
    skillName = "Power Shot",
    maxLevel = 24,
    skillType = ActiveSkillType.ACTIVE,
    targetType = SkillTargetType.ENEMY,
    reuseDelay = 25_000,
    castTime = 3_200,
    repriseTime = 800,
    castRange = 700,
    effectRange = 1200,
    requires = SkillRequirements(
        weaponTypes = listOf(WeaponType.BOW)
    ),
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

val LIFE_SCAVENGE = ActiveSkillTemplate(
    id = 46,
    skillName = "Life Scavenge",
    maxLevel = 15,
    skillType = ActiveSkillType.MAGIC,
    targetType = SkillTargetType.DEAD_NPC,
    reuseDelay = 20_000,
    castTime = 1500,
    castRange = 400,
    effectRange = 900,
    consumesToStart = SkillConsumablesTemplate(
        mp = listOf(7, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13, 13, 14, 14)
    ),
    consumes = SkillConsumablesTemplate(
        mp = listOf(28, 30, 33, 35, 38, 40, 43, 44, 46, 48, 49, 51, 52, 53, 55)
    ),
    skillAction = CorpseDrainSkillAction(
        power = listOf(105, 113, 122, 131, 140, 150, 159, 169, 180, 190, 201, 211, 222, 232, 243),
    )
)
