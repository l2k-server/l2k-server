package org.l2kserver.game.data.item.weapons

import org.l2kserver.game.data.item.arrows.WOODEN_ARROW
import org.l2kserver.game.model.item.ConsumableItem
import org.l2kserver.game.model.item.Grade
import org.l2kserver.game.model.item.WeaponTemplate
import org.l2kserver.game.model.item.WeaponType
import org.l2kserver.game.model.stats.Stats

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
    stats = Stats(
        pAtk = 11,
        mAtk = 12,
        atkSpd = 325,
        critRate = 40
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
    stats = Stats(
        pAtk = 5,
        mAtk = 5,
        atkSpd = 433,
        critRate = 120
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
    stats = Stats(
        pAtk = 23,
        mAtk = 9,
        atkSpd = 293,
        critRate = 120
    ),
    crystalCount = 0,
    soulshotUsed = 1,
    spiritshotUsed = 1,
    consumes = ConsumableItem(WOODEN_ARROW.id),
    manaCost = 1
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
    stats = Stats(
        pAtk = 6,
        mAtk = 5,
        atkSpd = 379,
        critRate = 80
    ),
    crystalCount = 0,
    soulshotUsed = 1,
    spiritshotUsed = 1
)
