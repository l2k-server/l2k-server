package org.l2kserver.game.model.stats

data class CombatStatsMultipliers(
    // Resource stats
    val maxCp: Double = 1.0,
    val maxHp: Double = 1.0,
    val maxMp: Double = 1.0,

    //Combat stats
    val pAtk: Double = 1.0,
    val pDef: Double = 1.0,
    val accuracy: Double = 1.0,
    val critRate: Double = 1.0,
    val atkSpd: Double = 1.0,
    val mAtk: Double = 1.0,
    val mDef: Double = 1.0,
    val speed: Double = 1.0,
    val castingSpd: Double = 1.0,

    val shieldDef: Double = 1.0,
    val shieldDefRate: Double = 1.0,

    val critDamage: Double = 1.0,
    val attackRange: Double = 1.0,

    val mCritRate: Double = 1.0,

    //Regen stats
    val cpRegen: Double = 1.0,
    val hpRegen: Double = 1.0,
    val mpRegen: Double = 1.0,
) {
    operator fun times(other: CombatStatsMultipliers) = this.copy(
        maxCp = this.maxCp * other.maxCp,
        maxHp = this.maxHp * other.maxHp,
        maxMp = this.maxMp * other.maxMp,

        pAtk = this.pAtk * other.pAtk,
        pDef = this.pDef * other.pDef,
        critRate = this.critRate * other.critRate,
        atkSpd = this.atkSpd * other.atkSpd,
        mAtk = this.mAtk * other.mAtk,
        mDef = this.mDef * other.mDef,
        speed = this.speed * other.speed,
        castingSpd = this.castingSpd * other.castingSpd,

        shieldDef = this.shieldDef * other.shieldDef,
        shieldDefRate = this.shieldDefRate * other.shieldDefRate,

        critDamage = this.critDamage * other.critDamage,
        attackRange = this.attackRange * other.attackRange,

        mCritRate = this.mCritRate * other.mCritRate,

        cpRegen = this.cpRegen * other.cpRegen,
        hpRegen = this.hpRegen * other.hpRegen,
        mpRegen = this.mpRegen * other.mpRegen,
    )

    override fun toString(): String {
        val multipliersList = buildList {
            if (maxCp != 1.0) add("maxCp=$maxCp")
            if (maxHp != 1.0) add("maxHp=$maxHp")
            if (maxMp != 1.0) add("maxMp=$maxMp")

            if (pAtk != 1.0) add("pAtk=$pAtk")
            if (pDef != 1.0) add("pDef=$pDef")
            if (critRate != 1.0) add("critRate=$critRate")
            if (atkSpd != 1.0) add("atkSpd=$atkSpd")
            if (mAtk != 1.0) add("mAtk=$mAtk")
            if (mDef != 1.0) add("mDef=$mDef")
            if (speed != 1.0) add("speed=$speed")
            if (castingSpd != 1.0) add("castingSpd=$castingSpd")

            if (shieldDef != 1.0) add("shieldDef=$shieldDef")
            if (shieldDefRate != 1.0) add("shieldDefRate=$shieldDefRate")

            if (critDamage != 1.0) add("critDamage=$critDamage")
            if (attackRange != 1.0) add("attackRange=$attackRange")

            if (mCritRate != 1.0) add("mCritRate=$mCritRate")

            if (hpRegen != 1.0) add("hpRegen=$hpRegen")
            if (mpRegen != 1.0) add("mpRegen=$mpRegen")
            if (cpRegen != 1.0) add("cpRegen=$cpRegen")
        }

        return multipliersList.joinToString(",", "CombatStatsMultipliers(", ")")
    }
}
