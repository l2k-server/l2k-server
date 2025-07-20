package org.l2kserver.game.model

import com.fasterxml.jackson.annotation.JsonCreator
import org.l2kserver.game.extensions.toIntRange

/**
 * Reward given for killing monster or quest completion
 *
 * @property exp How many experience is given
 * @property sp How many skill points is given
 * @property itemGroups Reward item groups.
 *
 * @see org.l2kserver.game.model.RewardItemGroup
 */
data class Reward(
    val exp: Int = 0,
    val sp: Int = 0,
    val itemGroups: List<RewardItemGroup> = emptyList()
)

/**
 * Group of reward items. Only one item from group will be dropped, if [chance] occurs
 *
 * @property chance Drop chance
 * @property items Items, one of which will be dropped
 */
data class RewardItemGroup(
    val chance: Double,
    val items: List<RewardItem>
)

/**
 * Reward item data - what item should be dropped from monster,
 * given as quest reward, etc
 *
 * @property id id of item template (for example Squire's Shirt's item id is 1146)
 * @property name name of item
 * @property amount Range of item amount to drop
 * @property isEquipped Is this item equipped (for initial items)
 * @property enchantLevel Enchant level of this item (for armor, weapons and jewellery)
 */
data class RewardItem(
    val id: Int,
    val name: String,
    val amount: IntRange,
    val chance: Double = 1.0,
    val isEquipped: Boolean = false,
    val enchantLevel: Int = 0
) {

    @JsonCreator
    constructor(
        id: Int,
        name: String,
        amount: String = "1",
        chance: Double = 1.0,
        isEquipped: Boolean = false,
        enchantLevel: Int = 0
    ): this(
        id = id,
        name = name,
        amount = amount.toIntOrNull()?.let { it..it } ?: amount.toIntRange(),
        chance = chance,
        isEquipped = isEquipped,
        enchantLevel = enchantLevel
    )

    init {
        require(amount.first > 0 && amount.last > 0) { "Item amount range indices must not be negative" }
        require(amount.last >= amount.first) { "Item max amount cannot be lesser than min" }
    }

}
