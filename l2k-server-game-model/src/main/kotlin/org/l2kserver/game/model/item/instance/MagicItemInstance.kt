package org.l2kserver.game.model.item.instance

import org.l2kserver.game.model.item.template.Grade
import org.l2kserver.game.model.skill.instance.ActiveSkillInstance

interface MagicItemInstance: ItemInstance {
    override val id: Int
    override val templateId: Int

    override var ownerId: Int
    override var amount: Int

    override val name: String
    override val grade: Grade
    override val weight: Int
    override val price: Int

    /** Creates skill instance for the given character ID */
    fun createSkill(characterId: Int): ActiveSkillInstance
}
