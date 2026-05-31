package org.l2kserver.game.data.item.weapon

import org.l2kserver.game.data.item.arrow.WoodenArrow
import org.l2kserver.game.model.item.Grade
import org.l2kserver.game.model.item.Weapon
import org.l2kserver.game.model.item.WeaponType
import org.l2kserver.game.model.item.of
import org.l2kserver.game.model.stats.CombatStats

data object ApprenticesWand: Weapon() {
    override val id = 6
    override val name = "Apprentice's Wand"
    override val grade = Grade.NO_GRADE
    override val weight = 1350
    override val price = 138
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
    override val type = WeaponType.BLUNT_ONE_HANDED
    override val isMagicWeapon = true
    override val stats = CombatStats.ofOneHandedBlunt(
        pAtk = 5,
        mAtk = 7
    )
    override val crystalCount = 0
    override val soulshotUsed = 1
    override val spiritshotUsed = 1
}

data object WillowStaff: Weapon() {
    override val id = 8
    override val name = "Willow Staff"
    override val grade = Grade.NO_GRADE
    override val weight = 1080
    override val price = 12500
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
    override val type = WeaponType.BLUNT_TWO_HANDED
    override val isMagicWeapon = true
    override val stats = CombatStats.ofTwoHandedBlunt(
        pAtk = 11,
        mAtk = 12
    )
    override val crystalCount = 0
    override val soulshotUsed = 1
    override val spiritshotUsed = 1
}

data object Dagger: Weapon() {
    override val id = 10
    override val name = "Dagger"
    override val grade = Grade.NO_GRADE
    override val weight = 1160
    override val price = 138
    override val isSellable = false
    override val isDroppable = false
    override val isDestroyable = true
    override val isExchangeable = false
    override val type = WeaponType.DAGGER
    override val isMagicWeapon = false
    override val stats = CombatStats.ofDagger(
        pAtk = 5,
        mAtk = 5
    )
    override val crystalCount = 0
    override val soulshotUsed = 1
    override val spiritshotUsed = 1
}

data object Bow: Weapon() {
    override val id = 14
    override val name = "Bow"
    override val grade = Grade.NO_GRADE
    override val weight = 1930
    override val price = 12500
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
    override val type = WeaponType.BOW
    override val isMagicWeapon = false
    override val stats = CombatStats.ofBow(
        pAtk = 23,
        mAtk = 9
    )
    override val crystalCount = 0
    override val soulshotUsed = 1
    override val spiritshotUsed = 1
    override val consumes = 1 of WoodenArrow
    override val manaCost = 1
}

data object ShortSpear: Weapon() {
    override val id = 15
    override val name = "Short Spear"
    override val grade = Grade.NO_GRADE
    override val weight = 2140
    override val price = 136_000
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
    override val type = WeaponType.POLE
    override val isMagicWeapon = false
    override val stats = CombatStats.ofPole(
        pAtk = 24,
        mAtk = 17
    )
    override val crystalCount = 0
    override val soulshotUsed = 2
    override val spiritshotUsed = 2
}

data object SquiresSword: Weapon() {
    override val id = 2369
    override val name = "Squire's Sword"
    override val grade = Grade.NO_GRADE
    override val weight = 1600
    override val price = 26
    override val isSellable = false
    override val isDroppable = false
    override val isDestroyable = true
    override val isExchangeable = false
    override val type = WeaponType.SWORD_ONE_HANDED
    override val isMagicWeapon = false
    override val stats = CombatStats.ofOneHandedSword(
        pAtk = 6,
        mAtk = 5
    )
    override val crystalCount = 0
    override val soulshotUsed = 1
    override val spiritshotUsed = 1
}
