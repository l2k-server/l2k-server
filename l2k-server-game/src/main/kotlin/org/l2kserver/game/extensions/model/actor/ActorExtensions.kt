package org.l2kserver.game.extensions.model.actor

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.MutableActorInstance
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.skill.instance.CastableSkillInstance

fun ActorInstance.asMutable() = requireNotNull(this as? MutableActorInstance) {
    "$this cannot be mutable"
}

/** Checks if actor has enough HP to cast [skill] */
fun ActorInstance.hasEnoughHpToCast(skill: CastableSkillInstance) =
    (skill.consumes?.hp ?: 0) + (skill.consumesToStart?.hp ?: 0) <= this.currentHp

/** Checks if actor has enough MP to cast [skill] */
fun ActorInstance.hasEnoughMpToCast(skill: CastableSkillInstance) =
    (skill.consumes?.mp ?: 0) + (skill.consumesToStart?.mp ?: 0) <= this.currentMp

/** Checks if PlayerCharacter has enough consumable item in the inventory to cast [skill]*/
fun PlayerCharacter.hasEnoughConsumableItemFor(skill: CastableSkillInstance): Boolean {
    val consumableToStart = skill.consumesToStart?.item
    val consumable = skill.consumes?.item

    return when {
        consumableToStart == null && consumable == null -> true

        consumableToStart?.id == consumable?.id -> this.inventory.existsByIdAndAmount(
            consumable!!.id, consumable.amount + consumableToStart!!.amount
        )

        else -> {
            consumableToStart?.let { this.inventory.existsByIdAndAmount(it.id, it.amount) } != false
                    && consumable?.let { this.inventory.existsByIdAndAmount(it.id, it.amount) } != false
        }
    }
}
