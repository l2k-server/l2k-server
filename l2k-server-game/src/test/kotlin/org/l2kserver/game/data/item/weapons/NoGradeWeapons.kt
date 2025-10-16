package org.l2kserver.game.data.item.weapons

import org.l2kserver.game.data.item.arrows.WOODEN_ARROW
import org.l2kserver.game.model.item.template.Grade
import org.l2kserver.game.model.item.template.WeaponTemplate
import org.l2kserver.game.model.item.template.WeaponType
import org.l2kserver.game.model.item.of
import org.l2kserver.game.model.stats.CombatStats

val APPRENTICE_WAND = WeaponTemplate(
    id = 6,
    name = "Apprentice's Wand",
    grade = Grade.NO_GRADE,
    weight = 1350,
    price = 138,
    isSellable = true,
    isDroppable = true,
    isDestroyable = true,
    isExchangeable = true,
    type = WeaponType.BLUNT_ONE_HANDED,
    stats = CombatStats.ofOneHandedBlunt(
        pAtk = 5,
        mAtk = 7
    ),
    crystalCount = 0,
    soulshotUsed = 1,
    spiritshotUsed = 1
)

val WILLOW_STAFF = WeaponTemplate(
    id = 8,
    name = "Willow Staff",
    grade = Grade.NO_GRADE,
    weight = 1080,
    price = 12500,
    isSellable = true,
    isDroppable = true,
    isDestroyable = true,
    isExchangeable = true,
    type = WeaponType.BLUNT_TWO_HANDED,
    stats = CombatStats.ofTwoHandedBlunt(
        pAtk = 11,
        mAtk = 12
    ),
    crystalCount = 0,
    soulshotUsed = 1,
    spiritshotUsed = 1
)

val DAGGER = WeaponTemplate(
    id = 10,
    name = "Dagger",
    grade = Grade.NO_GRADE,
    weight = 1160,
    price = 138,
    isSellable = false,
    isDroppable = false,
    isDestroyable = true,
    isExchangeable = false,
    type = WeaponType.DAGGER,
    stats = CombatStats.ofDagger(
        pAtk = 5,
        mAtk = 5
    ),
    crystalCount = 0,
    soulshotUsed = 1,
    spiritshotUsed = 1
)

val BOW = WeaponTemplate(
    id = 14,
    name = "Bow",
    grade = Grade.NO_GRADE,
    weight = 1930,
    price = 12500,
    isSellable = true,
    isDroppable = true,
    isDestroyable = true,
    isExchangeable = true,
    type = WeaponType.BOW,
    stats = CombatStats.ofBow(
        pAtk = 23,
        mAtk = 9
    ),
    crystalCount = 0,
    soulshotUsed = 1,
    spiritshotUsed = 1,
    consumes = 1 of WOODEN_ARROW,
    manaCost = 1
)

val SHORT_SPEAR = WeaponTemplate(
    id = 15,
    name = "Short Spear",
    grade = Grade.NO_GRADE,
    weight = 2140,
    price = 136_000,
    isSellable = true,
    isDroppable = true,
    isDestroyable = true,
    isExchangeable = true,
    type = WeaponType.POLE,
    stats = CombatStats.ofPole(
        pAtk = 24,
        mAtk = 17
    ),
    crystalCount = 0,
    soulshotUsed = 2,
    spiritshotUsed = 2
)

val SQUIRES_SWORD = WeaponTemplate(
    id = 2369,
    name = "Squire's Sword",
    grade = Grade.NO_GRADE,
    weight = 1600,
    price = 26,
    isSellable = false,
    isDroppable = false,
    isDestroyable = true,
    isExchangeable = false,
    type = WeaponType.SWORD_ONE_HANDED,
    stats = CombatStats.ofOneHandedSword(
        pAtk = 6,
        mAtk = 5
    ),
    crystalCount = 0,
    soulshotUsed = 1,
    spiritshotUsed = 1
)
