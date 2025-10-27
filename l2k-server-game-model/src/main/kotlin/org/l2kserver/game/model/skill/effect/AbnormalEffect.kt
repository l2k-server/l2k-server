package org.l2kserver.game.model.skill.effect

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.skill.abnormal.Abnormals
import org.l2kserver.game.model.skill.abnormal.abnormals
import java.time.Duration
import java.time.Instant

/** Type of abnormal effect. Character can obtain only one effect of each type */
object AbnormalType {
    /** Increases evasion */
    const val AVOID_UP = "avoid_up"

    /** Increases physical defence*/
    const val PD_UP = "pd_up"

    /** Increases physical attack*/
    const val PA_UP = "pa_up"
}

fun interface AbnormalAction {
    fun affect(actor: ActorInstance, actionLevel: Int): Abnormals
}



data class AbnormalEffect(
    val targetId: Int,
    val skillId: Int,
    val expiresAt: Instant,
    val effectLevel: Int,
    val abnormalType: String,
    val abnormalAction: AbnormalAction
): Effect

inline fun Effects.applyAbnormal(
    target: ActorInstance,
    skillId: Int,
    duration: Duration,
    effectLevel: Int,
    abnormalType: String,
    crossinline action: Abnormals.() -> Unit
) = this.add(AbnormalEffect(
    targetId = target.id,
    skillId = skillId,
    expiresAt = Instant.now().plus(duration),
    effectLevel = effectLevel,
    abnormalType = abnormalType,
    abnormalAction = { _, _ -> abnormals { action() } }
))
