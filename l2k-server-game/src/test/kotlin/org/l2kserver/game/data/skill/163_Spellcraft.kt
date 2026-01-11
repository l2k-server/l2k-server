@file:Suppress("MatchingDeclarationName")
package org.l2kserver.game.data.skill

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.character.PlayerCharacterInstance
import org.l2kserver.game.model.item.template.ArmorType
import org.l2kserver.game.model.skill.effect.AbnormalEffect
import org.l2kserver.game.model.skill.template.PassiveSkill
import org.l2kserver.game.model.stats.CombatStatsMultipliers

data object SpellcraftEffect: AbnormalEffect {
    override val abnormalType = Spellcraft.skillName

    override fun getCombatStatsMultipliers(actor: ActorInstance): CombatStatsMultipliers? {
        if (actor !is PlayerCharacterInstance) return null

        val equippedItemTypes = actor.inventory.filter { it.isEquipped }.map { it.type }

        val robePartsEquipped = equippedItemTypes.containsAll(
            listOf(ArmorType.UPPER_BODY_ROBE, ArmorType.LOWER_BODY_ROBE)
        )
        val fullRobeEquipped = equippedItemTypes.contains(ArmorType.UPPER_AND_LOWER_BODY_ROBE)

        if (!robePartsEquipped && !fullRobeEquipped) return CombatStatsMultipliers(castingSpd = 0.5)

        return null
    }
}

data object Spellcraft: PassiveSkill() {
    override val id = 163
    override val skillName = "Spellcraft"
    override val maxLevel = 1

    override fun effect(actor: ActorInstance, actionLevel: Int) = SpellcraftEffect
}
