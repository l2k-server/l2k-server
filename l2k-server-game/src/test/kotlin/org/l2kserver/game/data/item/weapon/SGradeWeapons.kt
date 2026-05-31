package org.l2kserver.game.data.item.weapon

import org.l2kserver.game.model.item.Grade
import org.l2kserver.game.model.item.Weapon
import org.l2kserver.game.model.item.WeaponType
import org.l2kserver.game.model.stats.CombatStats

data object DemonSplinter: Weapon() {
    override val id = 6371
    override val name = "Demon Splinter"
    override val grade = Grade.S
    override val weight = 1350
    override val price = 48_800_000
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
    override val type = WeaponType.FIST
    override val isMagicWeapon = false
    override val stats = CombatStats.ofFist(
        pAtk = 342,
        mAtk = 132
    )
    override val crystalCount = 2_440
    override val soulshotUsed = 1
    override val spiritshotUsed = 1
}

data object HeavensDivider: Weapon() {
    override val id = 6372
    override val name = "Heaven's Divider"
    override val grade = Grade.S
    override val weight = 1380
    override val price = 48_800_000
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
    override val type = WeaponType.SWORD_TWO_HANDED
    override val isMagicWeapon = false
    override val stats = CombatStats.ofTwoHandedSword(
        pAtk = 342,
        mAtk = 132
    )
    override val crystalCount = 2_440
    override val soulshotUsed = 1
    override val spiritshotUsed = 1
}

data object ArcanaMace: Weapon() {
    override val id = 6579
    override val name = "Arcana Mace"
    override val grade = Grade.S
    override val weight = 1380
    override val price = 42_759_400
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
    override val type = WeaponType.BLUNT_ONE_HANDED
    override val isMagicWeapon = true
    override val stats = CombatStats.ofOneHandedBlunt(
        pAtk = 225,
        mAtk = 175
    )
    override val crystalCount = 2052
    override val soulshotUsed = 1
    override val spiritshotUsed = 1
}

data object TallumBladeDarkLegionsEdge: Weapon() {
    override val id = 6580
    override val name = "Tallum Blade*Dark Legion's Edge"
    override val grade = Grade.S
    override val weight = 2080
    override val price = 48_800_000
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
    override val type = WeaponType.DOUBLE_BLADES
    override val isMagicWeapon = false
    override val stats = CombatStats.ofDoubleBlades(
        pAtk = 342,
        mAtk = 132
    )
    override val crystalCount = 2_440
    override val soulshotUsed = 1
    override val spiritshotUsed = 1
}
