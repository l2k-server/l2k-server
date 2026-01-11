package org.l2kserver.game.model.item.template

import org.l2kserver.game.model.skill.template.ActiveSkill

abstract class MagicItem: Item {
    abstract override val id: Int
    abstract override val name: String
    abstract override val grade: Grade
    abstract override val weight: Int
    abstract override val price: Int
    abstract override val isSellable: Boolean
    abstract override val isDroppable: Boolean
    abstract override val isDestroyable: Boolean
    abstract override val isExchangeable: Boolean
    abstract override val isStackable: Boolean

    /**
     * Skill template that will be used when this item is used
     * Contains skill template and level
     */
    abstract val skill: ActiveSkill
}
