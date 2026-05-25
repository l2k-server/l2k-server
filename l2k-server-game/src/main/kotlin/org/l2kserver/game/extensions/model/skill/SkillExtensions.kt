package org.l2kserver.game.extensions.model.skill

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.character.PlayerCharacterInstance
import org.l2kserver.game.model.actor.npc.NpcInstance
import org.l2kserver.game.model.skill.instance.ActiveSkillInstance
import org.l2kserver.game.model.skill.instance.SkillTargetType
import java.time.Instant

fun ActiveSkillInstance.isOnCooldown() = Instant.now().isBefore(this.nextUsageTime)

fun ActiveSkillInstance.isCastableOnSelf(): Boolean {
    return this.targetType == SkillTargetType.SELF || this.targetType == SkillTargetType.FRIEND
}

fun ActiveSkillInstance.isTargetTypeCorrect(
    caster: ActorInstance, target: ActorInstance, forced: Boolean
) = when (this.targetType) {
    SkillTargetType.FRIEND if (target.isDead() || target.isEnemyOf(caster) && !forced) -> {
        false
    }

    SkillTargetType.DEAD_NPC if (!target.isDead() || target !is NpcInstance) -> {
        false
    }

    SkillTargetType.DEAD_PLAYER if (!target.isDead() || target !is PlayerCharacterInstance) -> {
        false
    }

    SkillTargetType.ENEMY if (target.isDead() || (!target.isEnemyOf(caster) && !forced)) -> {
        false
    }

    else -> true
}
