package org.l2kserver.game.domain

import org.l2kserver.game.model.skill.effect.AbnormalEffect
import java.util.concurrent.ConcurrentHashMap

/** Contains buffs, debuffs, several other states of actor(like death penalty) */
class AbnormalEffects: MutableCollection<AbnormalEffect> {

    /** Key - abnormal type, value - abnormal effect */
    private val effects = ConcurrentHashMap<String, AbnormalEffect>()

    /**
     * Adds new effect
     *
     * @return `true` if effect was added or `false` if effect cannot be applied
     * (for example if already applied effect of such abnormal type is better than [element]
     */
    override fun add(element: AbnormalEffect): Boolean {
        val existingEffect = effects[element.abnormalType] ?: run {
            effects[element.abnormalType] = element
            return true
        }

        if (element.effectLevel > existingEffect.effectLevel) {
            effects[element.abnormalType] = element
            return true
        }

        if (element.effectLevel == existingEffect.effectLevel &&
            element.expiresAt.isAfter(existingEffect.expiresAt)
        ) {
            effects[element.abnormalType] = element
            return true
        }

        return false
    }

    override fun addAll(elements: Collection<AbnormalEffect>) = synchronized(effects) {
        var result = false
        elements.forEach { result = result || add(it) }
        result
    }

    /**
     * Removes effect from effects list
     *
     * @return `true` if effect was removed or `false` if there was no such effect in list
     */
    override fun remove(element: AbnormalEffect): Boolean = synchronized(effects) {
        if (effects[element.abnormalType] == element) {
            effects.remove(element.abnormalType)
            true
        }
        else false
    }

    /**
     * Removes all the provided [elements] from effects list
     *
     * @return `true` if effect list was modified, `false` if none of provided [elements] was found in effect list
     */
    override fun removeAll(elements: Collection<AbnormalEffect>): Boolean = synchronized(effects) {
        var result = false
        elements.forEach { result = result || remove(it) }
        result
    }

    override fun retainAll(elements: Collection<AbnormalEffect>): Boolean = synchronized(effects) {
        var result = false
        effects.values.forEach { if (!elements.contains(it)) result = result || remove(it) }
        result
    }

    override val size: Int get() = effects.size
    override fun contains(element: AbnormalEffect) = effects[element.abnormalType] == element
    override fun containsAll(elements: Collection<AbnormalEffect>) = elements.all(this::contains)
    override fun isEmpty() = effects.isEmpty()
    override fun iterator() = effects.values.iterator()
    override fun clear() = effects.clear()
}
