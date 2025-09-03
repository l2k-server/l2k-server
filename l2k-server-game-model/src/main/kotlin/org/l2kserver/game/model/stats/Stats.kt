package org.l2kserver.game.model.stats

import kotlin.math.roundToInt

/**
 * Data class representing stats.
 * Character stats is combination of its class stats, item stats, skill stats, etc...
 */
data class Stats(
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

    //Regen stats
    val hpRegen: Double = 0.0,
    val mpRegen: Double = 0.0,
    val cpRegen: Double = 0.0
) {

    val walkSpeed = (speed * 0.7).roundToInt()
    //TODO Should be 0,5 fix in
    // https://github.com/orgs/l2k-server/projects/1/views/3?pane=issue&itemId=120796094&issue=l2k-server%7Cl2k-server%7C15

    operator fun plus(other: Stats?) = if (other == null) this else Stats(
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
