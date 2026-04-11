package org.l2kserver.game.extensions.model.actor

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.Intention
import org.l2kserver.game.model.actor.MutableActorInstance
import org.l2kserver.game.model.actor.NpcInstanceImpl
import org.l2kserver.game.model.actor.PlayerCharacterInstanceImpl
import org.l2kserver.game.model.actor.character.PlayerCharacterInstance
import org.l2kserver.game.model.skill.instance.ActiveSkillInstance

fun ActorInstance.asMutable() = requireNotNull(this as? MutableActorInstance) {
    "$this cannot be mutable"
}

/** Checks if actor has enough HP to cast [skill] */
fun ActorInstance.hasEnoughHpToCast(skill: ActiveSkillInstance) =
    (skill.consumes?.hp ?: 0) + (skill.consumesToStart?.hp ?: 0) <= this.currentHp

/** Checks if actor has enough MP to cast [skill] */
fun ActorInstance.hasEnoughMpToCast(skill: ActiveSkillInstance) =
    (skill.consumes?.mp ?: 0) + (skill.consumesToStart?.mp ?: 0) <= this.currentMp

/** Checks if [character] can interact with this actor */
fun ActorInstance.isInteractableBy(character: PlayerCharacterInstance): Boolean {
    val isFriendlyNpc = this is NpcInstanceImpl && !this.isEnemyOf(character)
    val isPlayerSeller = this is PlayerCharacterInstanceImpl && this.privateStore != null

    return isFriendlyNpc || isPlayerSeller
}

/** Checks if actor is already attacking [target] */
fun MutableActorInstance.isAttacking(
    target: MutableActorInstance
) = this.intentionQueue.contains(Intention.Attack(target))

/** Checks if PlayerCharacter has enough consumable item in the inventory to cast [skill]*/
fun PlayerCharacterInstanceImpl.hasEnoughConsumableItemFor(skill: ActiveSkillInstance): Boolean {
    val consumableToStart = skill.consumesToStart?.item
    val consumable = skill.consumes?.item

    return when {
        consumableToStart == null && consumable == null -> true
        consumableToStart?.templateId == consumable?.templateId -> this.inventory.existsByIdAndAmount(
            consumable!!.templateId, consumable.amount + consumableToStart!!.amount
        )
        consumableToStart?.let { this.inventory.hasEnough(it.templateId, it.amount) } == false -> false
        consumable?.let { this.inventory.hasEnough(it.templateId, it.amount) } == false -> false
        else -> true
    }
}
