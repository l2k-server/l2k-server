package org.l2kserver.game.model.skill.action

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.stats.Attribute

class SingleTargetMagicDamageSkillAction(
    val power: List<Int>,
    val attribute: Pair<Attribute, Int>
): SingleTargetMagicSkillAction {

    override fun applyTo(target: ActorInstance, caster: ActorInstance, effectLevel: Int) = effects {
        //TODO https://github.com/l2k-server/l2k-server/issues/73
    }

}
