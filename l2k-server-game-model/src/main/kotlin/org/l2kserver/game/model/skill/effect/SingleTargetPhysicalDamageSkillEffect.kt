package org.l2kserver.game.model.skill.effect

import org.l2kserver.game.model.actor.Actor

class SingleTargetPhysicalDamageSkillEffect(
    val power: List<Int>,
    val ignoresShield: Boolean = false,
    val overhitPossible: Boolean = false
): SingleTargetSkillEffect {

    override fun apply(by: Actor, to: Actor, effectLevel: Int) = effects {
        //TODO Real damage calculation
        val damage = power.getOrElse(effectLevel - 1) { by.stats.pAtk }

        if (ignoresShield) println("This skill ignores shield")
        if (overhitPossible) println("This skill can make an overhit!")

        dealDamage(damage, to)
    }

}
