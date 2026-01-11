@file:Suppress("MatchingDeclarationName")
package org.l2kserver.game.data.skill

import org.l2kserver.game.model.item.template.WeaponType
import org.l2kserver.game.model.skill.context.SkillContext
import org.l2kserver.game.model.skill.effect.DamageEffect
import org.l2kserver.game.model.skill.effect.Effects
import org.l2kserver.game.model.skill.instance.SkillTargetType
import org.l2kserver.game.model.skill.template.ActiveSkill
import org.l2kserver.game.model.skill.template.SkillConsumablesTemplate
import org.l2kserver.game.model.skill.template.SkillRequirements

data object MortalBlow: ActiveSkill() {
    override val id = 16
    override val skillName = "Mortal Blow"
    override val maxLevel = 24
    override val reuseDelay = 11_000
    override val castTime = 1_080
    override val isMagic = false
    override val repriseTime = 720
    override val castRange = 40
    override val effectRange = 400
    override val consumes = SkillConsumablesTemplate(
        mp = listOf(
            9,9,10,11,12,13,16,16,17,19,20,20,21,22,23,25,26,27,28,28,29,32,33,34
        )
    )
    override val targetType = SkillTargetType.ENEMY
    override val requires = SkillRequirements(weaponTypes = listOf(WeaponType.DAGGER))

    val power = listOf(
        73,80,88,115,126,137,178,193,210,268,291,314,367,396,427,494,531,571,656,703,752,859,916,977
    )

    override fun affect(context: SkillContext) = Effects(
        DamageEffect.blow(
            caster = context.caster,
            target = context.mainTarget,
            power = power[context.skillLevel - 1],
            usedSoulshot = context.usedSoulshot
        )
    )
}
