package org.l2kserver.game.domain


import org.l2kserver.game.model.skill.effect.TemporalAbnormalEffect
import java.util.Collections

/** Contains buffs, debuffs, several other states of actor(like death penalty) */
class TemporalEffects : MutableCollection<TemporalAbnormalEffect> {

    /** Key - abnormal type, value - abnormal effect */
    private val effects = Collections.synchronizedMap(LinkedHashMap<String, TemporalAbnormalEffect>())

    /**
     * Adds new effect
     *
     * @return `true` if effect was added or `false` if effect cannot be applied
     * (for example if already applied effect of such abnormal type is better than [element]
     */
    override fun add(
        element: TemporalAbnormalEffect
    ): Boolean {
        val existingEffect = effects[element.abnormalType]

        return when {
            existingEffect == null -> {
                effects[element.abnormalType] = element
                true
            }
            element.effectLevel > existingEffect.effectLevel -> {
                effects[element.abnormalType] = element
                true
            }
            element.effectLevel == existingEffect.effectLevel &&
                    element.expiresAt.isAfter(existingEffect.expiresAt) -> {
                effects[element.abnormalType] = element
                return true
            }
            else -> false
        }
    }

    override fun addAll(elements: Collection<TemporalAbnormalEffect>) = synchronized(effects) {
        var result = false
        elements.forEach { result = result || add(it) }
        result
    }

    /**
     * Removes effect from effects list
     *
     * @return `true` if effect was removed or `false` if there was no such effect in list
     */
    override fun remove(element: TemporalAbnormalEffect): Boolean = synchronized(effects) {
        if (effects[element.abnormalType] == element) {
            effects.remove(element.abnormalType)
            true
        } else false
    }

    /**
     * Removes all the provided [elements] from effects list
     *
     * @return `true` if effect list was modified, `false` if none of provided [elements] was found in effect list
     */
    override fun removeAll(elements: Collection<TemporalAbnormalEffect>): Boolean = synchronized(effects) {
        var result = false
        elements.forEach { result = result || remove(it) }
        result
    }

    override fun retainAll(elements: Collection<TemporalAbnormalEffect>): Boolean = synchronized(effects) {
        var result = false
        effects.values.forEach { if (!elements.contains(it)) result = result || remove(it) }
        result
    }

    /** Bitwise sum of all the [org.l2kserver.game.model.skill.effect.AbnormalVisualEffect] on the owner */
    val visible: Int get() = effects.values.fold(0) { acc, effect ->
        acc or (effect.abnormalVisualEffect?.bit ?: 0)
    }

    override val size: Int get() = effects.size
    override fun contains(element: TemporalAbnormalEffect) = effects[element.abnormalType] == element
    override fun containsAll(elements: Collection<TemporalAbnormalEffect>) = elements.all(this::contains)
    override fun isEmpty() = effects.isEmpty()
    override fun iterator() = effects.values.iterator()
    override fun clear() = effects.clear()
}
