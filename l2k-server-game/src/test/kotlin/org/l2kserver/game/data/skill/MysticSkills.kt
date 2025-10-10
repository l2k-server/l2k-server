package org.l2kserver.game.data.skill

import org.l2kserver.game.model.item.template.ArmorType
import org.l2kserver.game.model.skill.action.Attribute
import org.l2kserver.game.model.skill.template.SkillConsumablesTemplate
import org.l2kserver.game.model.skill.instance.SkillTargetType
import org.l2kserver.game.model.skill.action.SingleTargetHealSkillAction
import org.l2kserver.game.model.skill.action.SingleTargetMagicDamageSkillAction
import org.l2kserver.game.model.skill.effect.StatsEffect
import org.l2kserver.game.model.skill.effect.effects
import org.l2kserver.game.model.skill.instance.ActiveSkillType
import org.l2kserver.game.model.skill.template.ActiveSkillTemplate
import org.l2kserver.game.model.skill.template.PassiveSkillTemplate
import org.l2kserver.game.model.stats.CombatStatsMultipliers

val SPELLCRAFT = PassiveSkillTemplate(
    id = 163,
    skillName = "Spellcraft",
    maxLevel = 1,
    skillAction = { character, actionLevel ->
        effects {
            val equippedItemTypes = character.inventory.filter { it.isEquipped }.map { it.type }

            val robePartsEquipped = equippedItemTypes.containsAll(
                listOf(ArmorType.UPPER_BODY_ROBE, ArmorType.LOWER_BODY_ROBE)
            )
            val fullRobeEquipped = equippedItemTypes.contains(ArmorType.UPPER_AND_LOWER_BODY_ROBE)

            if (!robePartsEquipped && !fullRobeEquipped)
                add(StatsEffect(combatStatsMultipliers = CombatStatsMultipliers(castingSpd = 0.5)))
        }
    }
)

val WIND_STRIKE = ActiveSkillTemplate(
    id = 1177,
    skillName = "Wind Strike",
    maxLevel = 5,
    skillType = ActiveSkillType.MAGIC,
    targetType = SkillTargetType.ENEMY,
    reuseDelay = 6000,
    castTime = 4000,
    repriseTime = 800,
    castRange = 600,
    effectRange = 1100,
    consumesToStart = SkillConsumablesTemplate(
        mp = listOf(2, 2, 2, 3, 3)
    ),
    consumes = SkillConsumablesTemplate(
        mp = listOf(7, 7, 8, 11, 12)
    ),
    skillAction = SingleTargetMagicDamageSkillAction(
        power = listOf(12, 13, 15, 18, 21),
        magicLevel = listOf(1, 4, 7, 11, 14),
        attribute = Attribute.WIND
    )
)

val SELF_HEAL = ActiveSkillTemplate(
    id = 1216,
    skillName = "Self Heal",
    maxLevel = 1,
    skillType = ActiveSkillType.MAGIC,
    targetType = SkillTargetType.SELF,
    reuseDelay = 10_000,
    castTime = 5000,
    consumesToStart = SkillConsumablesTemplate(mp = listOf(2)),
    consumes = SkillConsumablesTemplate(mp = listOf(7)),
    skillAction = SingleTargetHealSkillAction(power = listOf(42))
)
