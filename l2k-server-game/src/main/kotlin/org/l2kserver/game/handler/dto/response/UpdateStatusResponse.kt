package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.model.item.countWeightByOwnerId
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.actor.Actor
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.item.Item

private const val UPDATE_STATUS_RESPONSE_PACKET_ID: UByte = 14u

/**
 * Updated data of some object in game world
 *
 * @property objectId ID of object in game world
 * @property attributes updated attributes. Key - Attribute ID, Value - updated attribute value
 */
data class UpdateStatusResponse(
    val objectId: Int,
    val attributes: Map<StatusAttribute, Int>
): ResponsePacket {

    constructor(objectId: Int, vararg attributes: Pair<StatusAttribute, Int>): this(objectId, attributes.toMap())

    override val data = littleEndianByteArray {
        putUByte(UPDATE_STATUS_RESPONSE_PACKET_ID)
        putInt(objectId)
        putInt(attributes.size)

        attributes.forEach { (attribute, value) ->
            putInt(attribute.id)
            putInt(value)
        }
    }

    companion object {

        /**
         * Create UpdateStatusResponse for updating actor's HP, MP and CP (if actor is PlayerCharacter) on client side
         */
        fun hpMpCpOf(actor: Actor): UpdateStatusResponse {
            val attributes = mutableMapOf(
                StatusAttribute.CUR_HP to actor.currentHp,
                StatusAttribute.CUR_MP to actor.currentMp
            )
            if (actor is PlayerCharacter) attributes[StatusAttribute.CUR_CP] = actor.currentCp

            return UpdateStatusResponse(actor.id, attributes)
        }

        /**
         * Create UpdateStatusResponse for updating character's weight on client side
         */
        fun weightOf(character: PlayerCharacter) = UpdateStatusResponse(
            character.id,
            StatusAttribute.CUR_LOAD to Item.countWeightByOwnerId(character.id)
        )
    }

}

enum class StatusAttribute(val id: Int) {
    LEVEL(1),
    EXP(2),

    STR(3),
    DEX(4),
    CON(5),
    INT(6),
    WIT(7),
    MEN(8),

    CUR_HP(9),
    MAX_HP(10),
    CUR_MP(11),
    MAX_MP(12),

    SP(13),

    CUR_LOAD(14),
    MAX_LOAD(15),

    P_ATK(17),
    ATK_SPD(18),
    P_DEF(19),
    EVASION(20),
    ACCURACY(21),
    CRITICAL(22),
    M_ATK(23),
    CAST_SPD(24),
    M_DEF(25),
    PVP_FLAG(26),
    KARMA(27),

    CUR_CP(33),
    MAX_CP(34),
}
