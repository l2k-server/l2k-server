package org.l2kserver.game.data.skill

import org.l2kserver.game.model.skill.effect.SingleTargetPhysicalDamageSkillEffect
import org.l2kserver.game.model.item.template.WeaponType
import org.l2kserver.game.model.skill.SkillConsumablesTemplate
import org.l2kserver.game.model.skill.SkillRequirements
import org.l2kserver.game.model.skill.SkillTargetType
import org.l2kserver.game.model.skill.SkillTemplate
import org.l2kserver.game.model.skill.SkillType
import org.l2kserver.game.model.skill.effect.BlowSkillEffect

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
    effects = listOf(
        SingleTargetPhysicalDamageSkillEffect(
            power = listOf(25, 27, 30, 39, 42, 46, 60, 65, 70),
            ignoresShield = true,
            overhitPossible = true
        )
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
    effects = listOf(
        BlowSkillEffect(
            power = listOf(73,80,88,115,126,137,178,193,210,268,291,314,367,396,427,494,531,571,656,703,752,859,916,977)
            //TODO Lethal effect?
        )
    )
)
