package org.l2kserver.game.model.actor.npc.ai

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.position.Position

sealed interface AiIntent

@JvmInline
value class WaitIntent(val waitTimeMillis: Long): AiIntent

@JvmInline
value class SayIntent(val message: String): AiIntent

@JvmInline
value class MoveIntent(val position: Position): AiIntent

@JvmInline
value class AttackIntent(val target: ActorInstance): AiIntent

@JvmInline
value class AiIntents private constructor(
    private val actions: MutableList<AiIntent>
): Iterable<AiIntent> by actions {
    constructor(): this(ArrayList<AiIntent>())

    @Suppress("unused")
    fun say(message: String) {
        actions.add(SayIntent(message))
    }

    @Suppress("unused")
    fun sleep(waitTimeMillis: Long) {
        actions.add(WaitIntent(waitTimeMillis))
    }

    @Suppress("unused")
    fun moveTo(position: Position) {
        actions.add(MoveIntent(position))
    }

    @Suppress("unused")
    fun attack(target: ActorInstance) {
        actions.add(AttackIntent(target))
    }

}
