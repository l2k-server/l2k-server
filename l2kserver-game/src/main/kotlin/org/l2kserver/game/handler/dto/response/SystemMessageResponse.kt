package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUTF16String
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.item.Item
import org.l2kserver.game.model.skill.Skill

private const val SYSTEM_MESSAGE_RESPONSE_PACKET_ID: UByte = 100u

open class SystemMessageResponse private constructor(
    val systemMessageId: Int,
    private vararg val placeholders: Any
) : ResponsePacket {

    companion object {

        fun youHavePurchased(item: Item, sellerName: String, amount: Int) =
            if (item.isStackable)
                YouHavePurchasedStackable(sellerName, item, amount)
            else if (!item.isStackable && item.enchantLevel > 0)
                YouHavePurchasedEnchanted(sellerName, item)
            else
                YouHavePurchasedNonStackable(sellerName, item)

        fun otherHasPurchased(customerName: String, item: Item, amount: Int) =
            if (item.isStackable) OtherHasPurchasedStackable(customerName, item, amount)
            else if (!item.isStackable && item.enchantLevel > 0)
                OtherHasPurchasedEnchanted(customerName, item)
            else
                OtherHasPurchasedNonStackable(customerName, item)
    }

    /** Custom system message */
    constructor(message: String): this(systemMessageId = 614, message)

    /** Message: "Your target is out of range." */
    data object TargetOutOfRange: SystemMessageResponse(systemMessageId = 22)

    /** Message: "Welcome to the World of Lineage II." */
    data object Welcome: SystemMessageResponse(systemMessageId = 34)

    /** Message: "You have equipped your [item]" */
    data class EquipItem(val item: Item): SystemMessageResponse(systemMessageId = 49, item)

    /** Message: "[item] has been disarmed" */
    data class DisarmItem(val item: Item): SystemMessageResponse(systemMessageId = 417, item)

    /** Message: "This item cannot be discarded" */
    data object CannotDiscardItem: SystemMessageResponse(systemMessageId = 98)

    /** Message: "[item] cannot be used due to unsuitable terms."*/
    data class ItemCannotBeUsed(val item: Item): SystemMessageResponse(systemMessageId = 113, item)

    /** Message: While operating a private store or workshop, you cannot discard, destroy, or trade an item. */
    data object CannotDiscardDestroyOrTradeWhileInShop: SystemMessageResponse(systemMessageId = 1065)

    /** Message: "You cannot restart while in combat" */
    data object CannotRestartInCombat: SystemMessageResponse(systemMessageId = 102)

    /** Message: "Your shield defense has succeeded" */
    data object ShieldDefenceSuccessful: SystemMessageResponse(systemMessageId = 111)

    /** Message: "Your target cannot be found." */
    data object TargetCannotBeFound: SystemMessageResponse(systemMessageId = 50)

    /** Message: "You cannot use this on yourself." */
    data object CannotUseThisOnYourself: SystemMessageResponse(systemMessageId = 51)

    /** Message: "That is the incorrect target" */
    data object IncorrectTarget: SystemMessageResponse(systemMessageId = 144)

    /** Message: "Select target." */
    data object YouMustSelectTarget: SystemMessageResponse(systemMessageId = 242)

    /** Message: "That is too far from you to discard" */
    data object TooFarToDiscard: SystemMessageResponse(systemMessageId = 151)

    /** Message: "Incorrect item count" */
    data object NotEnoughItems: SystemMessageResponse(systemMessageId = 351)

    /** Message: "Attention: [characterName] picked up [item]" */
    data class AttentionPlayerPickedUp(
        val characterName: String, val item: Item
    ): SystemMessageResponse(systemMessageId = 1533, characterName, item)

    /** Message: "You have obtained $itemName" */
    data class YouHaveObtained(val item: Item): SystemMessageResponse(systemMessageId = 30, item)

    /** Message: "You hit [damage] damage." */
    data class YouHit(val damage: Int): SystemMessageResponse(systemMessageId = 35, damage)

    /** Message: "[actorName] hit you for [damage] damage." */
    data class YouWereHitBy(
        val actorName: String, val damage: Int
    ): SystemMessageResponse(systemMessageId = 36, actorName, damage)

    /** Message: "You carefully nock an arrow" */
    data object YouCarefullyNockAnArrow: SystemMessageResponse(systemMessageId = 41)

    /** Message: "You have avoided [attackerName]'s attack" */
    data class YouHaveAvoidedAttackOf(val attackerName: String) :
        SystemMessageResponse(systemMessageId = 42, attackerName)

    /** Message: "You have missed" */
    data object YouMissed: SystemMessageResponse(systemMessageId = 43)

    /** Message: "Critical hit!" */
    data object CriticalHit: SystemMessageResponse(systemMessageId = 44)

    /** Message: "You use [skill]" */
    data class YouUse(val skill: Skill): SystemMessageResponse(systemMessageId = 46, skill)

    /** Message: "You have earned [earnedExp] experience and [earnedSp] SP" */
    data class YouHaveEarnedExpAndSp(
        val earnedExp: Int, val earnedSp: Int
    ): SystemMessageResponse(systemMessageId = 95, earnedExp, earnedSp)

    /** Message: "Your level has increased!" */
    data object YourLevelHasIncreased: SystemMessageResponse(systemMessageId = 96)

    /**  Message: "You have exceeded the quantity that can be inputted." */
    data object YouHaveExceededPrivateStoreQuantity: SystemMessageResponse(systemMessageId = 1036)

    /**  Message: "Not enough MP." */
    data object NotEnoughMP: SystemMessageResponse(systemMessageId = 24)

    /** Message: "You have run out of arrows."*/
    data object NotEnoughArrows: SystemMessageResponse(systemMessageId = 112)

    /** Message: "You do not have enough adena."*/
    data object NotEnoughAdena: SystemMessageResponse(systemMessageId = 279)

    /** Message: "[customerName] purchased [item]." */
    data class OtherHasPurchasedNonStackable(val customerName: String, val item: Item): SystemMessageResponse(
        systemMessageId = 378, customerName, item
    )

    /** Message: "[customerName] purchased +[item.item.enchantLevel][item]." */
    data class OtherHasPurchasedEnchanted(val customerName: String, val item: Item): SystemMessageResponse(
        systemMessageId = 379, customerName, item.enchantLevel, item
    )

    /** Message: "[customerName] purchased [amount] [item]." */
    data class OtherHasPurchasedStackable(val customerName: String, val item: Item, val amount: Int): SystemMessageResponse(
        systemMessageId = 380, customerName, item, amount
    )

    /** Message: "You have purchased [item] from [sellerName]."*/
    data class YouHavePurchasedNonStackable(val sellerName: String, val item: Item): SystemMessageResponse(
        systemMessageId = 559, sellerName, item
    )

    /** Message: "You have purchased +[item.item.enchantLevel][item] from [sellerName]."*/
    data class YouHavePurchasedEnchanted(val sellerName: String, val item: Item): SystemMessageResponse(
        systemMessageId = 560, sellerName, item.enchantLevel, item
    )

    /** Message: "You have purchased [amount] [item] from [sellerName]."*/
    data class YouHavePurchasedStackable(val sellerName: String, val item: Item, val amount: Int): SystemMessageResponse(
        systemMessageId = 561, sellerName, item, amount
    )

    /** Message: "Your [item] has been successfully enchanted." */
    data class YourItemHasBeenSuccessfullyEnchanted(val item: Item): SystemMessageResponse(
        systemMessageId = 62, item
    )

    /** Message: "Your +[Item.enchantLevel][item] has been successfully enchanted." */
    data class YourEnchantedItemHasBeenSuccessfullyEnchanted(val item: Item): SystemMessageResponse(
        systemMessageId = 63, item.enchantLevel - 1, item
    )

    override val data = littleEndianByteArray {
        putUByte(SYSTEM_MESSAGE_RESPONSE_PACKET_ID)
        putInt(systemMessageId)
        putInt(placeholders.size)

        placeholders.forEach {
            when (it) {
                is String -> {
                    putInt(PlaceholderType.TEXT)
                    putUTF16String(it)
                }

                is Int -> {
                    putInt(PlaceholderType.NUMBER)
                    putInt(it)
                }

                is Item -> {
                    putInt(PlaceholderType.ITEM)
                    putInt(it.templateId)
                }

                is Skill -> {
                    putInt(PlaceholderType.SKILL)
                    putInt(it.skillId)
                    putInt(it.skillLevel)
                }

                else -> throw UnsupportedOperationException("Message about ${it::class} cannot be sent")
            }
        }
    }

    override fun toString(): String {
        return "SystemMessageResponse(id=$systemMessageId, placeholders=${placeholders.contentToString()})"
    }

}

private object PlaceholderType {
    const val TEXT = 0
    const val NUMBER = 1
    const val NPC = 2
    const val ITEM = 3
    const val SKILL = 4
    const val ZONE = 7
}
