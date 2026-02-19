package org.l2kserver.game.model.item.template

import org.l2kserver.game.model.skill.template.ActiveSkill

interface MagicItem: Item {
    override val id: Int
    override val name: String
    override val grade: Grade get() = Grade.NO_GRADE
    override val weight: Int
    override val price: Int
    override val isSellable: Boolean
    override val isDroppable: Boolean
    override val isDestroyable: Boolean
    override val isExchangeable: Boolean
    override val isStackable: Boolean

    /**
     * Skill template that will be used when this item is used
     * Contains skill template and level
     */
    val skill: ActiveSkill
}
