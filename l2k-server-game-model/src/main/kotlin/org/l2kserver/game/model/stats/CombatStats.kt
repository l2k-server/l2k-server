package org.l2kserver.game.model.stats

object AttackSpeed {
    const val VERY_FAST = 433
    const val FAST = 379
    const val NORMAL = 325
    const val SLOW = 293
    const val VERY_SLOW = 227
}

object CritRate {
    const val BLUNT_AND_FIST_DEFAULT_CRIT_RATE = 40
    const val SWORD_AND_POLE_DEFAULT_CRIT_RATE = 80
    const val DAGGER_AND_BOW_DEFAULT_CRIT_RATE = 120
}

object AttackRange {
    const val MELEE_WEAPON_DEFAULT_ATTACK_RANGE = 40
    const val BOW_DEFAULT_ATTACK_RANGE = 450
}

/**
 * Data class representing stats.
 * Character stats is combination of its class stats, item stats, skill stats, etc...
 */
data class CombatStats(
    // Resource stats
    val maxCp: Int = 0,
    val maxHp: Int = 0,
    val maxMp: Int = 0,

    //Combat stats
    val pAtk: Int = 0,
    val pDef: Int = 0,
    val accuracy: Int = 0,
    val critRate: Int = 0,
    val atkSpd: Int = 0,
    val mAtk: Int = 0,
    val mDef: Int = 0,
    val evasion: Int = 0,
    val speed: Int = 0,
    val castingSpd: Int = 0,

    val shieldDef: Int = 0,
    val shieldDefRate: Int = 0,

    val critDamage: Int = 0,
    val attackRange: Int = 0,

    val mCritRate: Int = 0,

    //Regen stats
    val hpRegen: Double = 0.0,
    val mpRegen: Double = 0.0,
    val cpRegen: Double = 0.0
) {

    companion object {

        /** Applies default combat stats of one-handed blunt with provided [pAtk] and [mAtk] */
        @JvmStatic
        fun ofOneHandedBlunt(pAtk: Int, mAtk: Int) = CombatStats(
            pAtk = pAtk,
            mAtk = mAtk,
            critRate = CritRate.BLUNT_AND_FIST_DEFAULT_CRIT_RATE,
            atkSpd = AttackSpeed.FAST,
            attackRange = AttackRange.MELEE_WEAPON_DEFAULT_ATTACK_RANGE
        )

        /** Applies default combat stats of two-handed blunt with provided [pAtk] and [mAtk] */
        @JvmStatic
        fun ofTwoHandedBlunt(pAtk: Int, mAtk: Int) = CombatStats(
            pAtk = pAtk,
            mAtk = mAtk,
            critRate = CritRate.BLUNT_AND_FIST_DEFAULT_CRIT_RATE,
            atkSpd = AttackSpeed.NORMAL,
            attackRange = AttackRange.MELEE_WEAPON_DEFAULT_ATTACK_RANGE
        )

        /** Applies default combat stats of one-handed sword with provided [pAtk] and [mAtk] */
        @JvmStatic
        fun ofOneHandedSword(pAtk: Int, mAtk: Int) = CombatStats(
            pAtk = pAtk,
            mAtk = mAtk,
            critRate = CritRate.SWORD_AND_POLE_DEFAULT_CRIT_RATE,
            atkSpd = AttackSpeed.FAST,
            attackRange = AttackRange.MELEE_WEAPON_DEFAULT_ATTACK_RANGE
        )

        /** Applies default combat stats of two-handed sword with provided [pAtk] and [mAtk] */
        @JvmStatic
        fun ofTwoHandedSword(pAtk: Int, mAtk: Int) = CombatStats(
            pAtk = pAtk,
            mAtk = mAtk,
            critRate = CritRate.SWORD_AND_POLE_DEFAULT_CRIT_RATE,
            atkSpd = AttackSpeed.NORMAL,
            attackRange = AttackRange.MELEE_WEAPON_DEFAULT_ATTACK_RANGE
        )

        /** Applies default combat stats of dagger with provided [pAtk] and [mAtk] */
        @JvmStatic
        fun ofDagger(pAtk: Int, mAtk: Int) = CombatStats(
            pAtk = pAtk,
            mAtk = mAtk,
            critRate = CritRate.DAGGER_AND_BOW_DEFAULT_CRIT_RATE,
            atkSpd = AttackSpeed.VERY_FAST,
            attackRange = AttackRange.MELEE_WEAPON_DEFAULT_ATTACK_RANGE
        )

        /** Applies default combat stats of bow with provided [pAtk] and [mAtk] */
        @JvmStatic
        fun ofBow(pAtk: Int, mAtk: Int) = CombatStats(
            pAtk = pAtk,
            mAtk = mAtk,
            critRate = CritRate.DAGGER_AND_BOW_DEFAULT_CRIT_RATE,
            atkSpd = AttackSpeed.SLOW,
            attackRange = AttackRange.BOW_DEFAULT_ATTACK_RANGE
        )

        /** Applies default combat stats of slow bow with provided [pAtk] and [mAtk] */
        @JvmStatic
        fun ofSlowBow(pAtk: Int, mAtk: Int) = CombatStats(
            pAtk = pAtk,
            mAtk = mAtk,
            critRate = CritRate.DAGGER_AND_BOW_DEFAULT_CRIT_RATE,
            atkSpd = AttackSpeed.VERY_SLOW,
            attackRange = AttackRange.BOW_DEFAULT_ATTACK_RANGE
        )

        /** Applies default combat stats of pole with provided [pAtk] and [mAtk] */
        @JvmStatic
        fun ofPole(pAtk: Int, mAtk: Int) = CombatStats(
            pAtk = pAtk,
            mAtk = mAtk,
            critRate = CritRate.SWORD_AND_POLE_DEFAULT_CRIT_RATE,
            atkSpd = AttackSpeed.NORMAL,
            attackRange = AttackRange.MELEE_WEAPON_DEFAULT_ATTACK_RANGE
        )

        /** Applies default combat stats of double blades with provided [pAtk] and [mAtk] */
        @JvmStatic
        fun ofDoubleBlades(pAtk: Int, mAtk: Int) = ofTwoHandedSword(pAtk, mAtk)

        /** Applies default combat stats of fist with provided [pAtk] and [mAtk] */
        @JvmStatic
        fun ofFist(pAtk: Int, mAtk: Int) = ofTwoHandedBlunt(pAtk, mAtk)

        /** Applies default combat stats of Etc with provided [pAtk] and [mAtk] */
        @JvmStatic
        fun ofEtc(pAtk: Int, mAtk: Int) = ofOneHandedSword(pAtk, mAtk)
    }

    val walkSpeed = speed / 2

    operator fun plus(other: CombatStats?) = if (other == null) this else CombatStats(
        maxCp = this.maxCp + other.maxCp,
        maxHp = this.maxHp + other.maxHp,
        maxMp = this.maxMp + other.maxMp,
        pAtk = this.pAtk + other.pAtk,
        pDef = this.pDef + other.pDef,
        accuracy = this.accuracy + other.accuracy,
        critRate = this.critRate + other.critRate,
        atkSpd = this.atkSpd + other.atkSpd,
        mAtk = this.mAtk + other.mAtk,
        mDef = this.mDef + other.mDef,
        evasion = this.evasion + other.evasion,
        speed = this.speed + other.speed,
        castingSpd = this.castingSpd + other.castingSpd,
        critDamage = this.critDamage + other.critDamage,
        attackRange = this.attackRange + other.attackRange,
        mCritRate = this.mCritRate + other.mCritRate,
        hpRegen = this.hpRegen + other.hpRegen,
        mpRegen = this.mpRegen + other.mpRegen,
        cpRegen = this.cpRegen + other.cpRegen
    )

    override fun toString(): String {
        val statList = buildList {
            if (maxCp != 0) add("maxCp=$maxCp")
            if (maxHp != 0) add("maxHp=$maxHp")
            if (maxMp != 0) add("maxMp=$maxMp")

            if (pAtk != 0) add("pAtk=$pAtk")
            if (pDef != 0) add("pDef=$pDef")
            if (accuracy != 0) add("accuracy=$accuracy")
            if (critRate != 0) add("critRate=$critRate")
            if (atkSpd != 0) add("atkSpd=$atkSpd")
            if (mAtk != 0) add("mAtk=$mAtk")
            if (mDef != 0) add("mDef=$mDef")
            if (evasion != 0) add("evasion=$evasion")
            if (speed != 0) add("speed=$speed")
            if (castingSpd != 0) add("castingSpd=$castingSpd")

            if (shieldDef != 0) add("shieldDef=$shieldDef")
            if (shieldDefRate != 0) add("shieldDefRate=$shieldDefRate")

            if (critDamage != 0) add("critDamage=$critDamage")
            if (attackRange != 0) add("attackRange=$attackRange")

            if (mCritRate != 0) add("mCritRate=$mCritRate")

            if (hpRegen != 0.0) add("hpRegen=$hpRegen")
            if (mpRegen != 0.0) add("mpRegen=$mpRegen")
            if (cpRegen != 0.0) add("cpRegen=$cpRegen")
        }

        return with(StringBuilder()) {
            append("Stats(")
            append(statList.joinToString(","))
            append(")")
        }.toString()
    }
}
