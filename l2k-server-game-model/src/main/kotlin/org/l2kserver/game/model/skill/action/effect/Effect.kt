package org.l2kserver.game.model.skill.action.effect

@JvmInline
value class SkillEffects private constructor(
    private val effectList: MutableList<Effect>
): Iterable<Effect> by effectList {
    constructor(): this(ArrayList<Effect>())

    fun add(effect: Effect) {
        effectList.add(effect)
    }

    /** Applies the event of dealing damage */
    fun hit(
        damage: Int,
        isCritical: Boolean = false,
        isBlocked: Boolean = false,
        isMagicCritical: Boolean = false,
        isHalfSuccessful: Boolean = false,
        isFailed: Boolean = false
    ) = add(
        DamageEffect(
            damage,
            isCritical = isCritical,
            isBlocked = isBlocked,
            isMagicCritical = isMagicCritical,
            isHalfSuccessful = isHalfSuccessful,
            isFailed = isFailed
        )
    )

    /** Applies the event of missing target */
    fun miss() = add(DamageEffect(isAvoided = true))

}

inline fun effects(builderFunction: SkillEffects.() -> Unit): SkillEffects {
    val skillEffects = SkillEffects()
    skillEffects.builderFunction()

    return skillEffects
}

sealed interface Effect

/**
 * Data class representing single target hit
 *
 * @property damage How many damage points has the attack dealt
 * @property isCritical is this attack critical
 * @property isBlocked is this attack blocked by shield
 * @property isAvoided is this attack avoided
 * @property isMagicCritical is this attack a magical crit
 * @property isHalfSuccessful is this attack half successful
 * @property isFailed is this attack failed
 */
data class DamageEffect(
    val damage: Int = 0,
    val isCritical: Boolean = false,
    val isBlocked: Boolean = false,
    val isAvoided: Boolean = false,
    val isMagicCritical: Boolean = false,
    val isHalfSuccessful: Boolean = false,
    val isFailed: Boolean = false
): Effect
