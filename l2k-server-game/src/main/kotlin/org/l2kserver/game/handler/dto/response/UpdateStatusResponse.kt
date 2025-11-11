package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.PlayerCharacter

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
        fun hpMpCpOf(actor: ActorInstance): UpdateStatusResponse {
            val attributes = mutableMapOf(
                StatusAttribute.CUR_HP to actor.currentHp,
                StatusAttribute.CUR_MP to actor.currentMp
            )
            if (actor is PlayerCharacter) attributes[StatusAttribute.CUR_CP] = actor.currentCp

            return UpdateStatusResponse(actor.id, attributes)
        }

        /** Create UpdateStatusResponse for updating character's weight on client side */
        fun weightOf(character: PlayerCharacter) = UpdateStatusResponse(
            character.id,
            StatusAttribute.CUR_LOAD to character.inventory.weight
        )
    }

}

enum class StatusAttribute(val id: Int) {
    LEVEL(id = 1),
    EXP(id = 2),

    STR(id = 3),
    DEX(id = 4),
    CON(id = 5),
    INT(id = 6),
    WIT(id = 7),
    MEN(id = 8),

    CUR_HP(id = 9),
    MAX_HP(id = 10),
    CUR_MP(id = 11),
    MAX_MP(id = 12),

    SP(id = 13),

    CUR_LOAD(id = 14),
    MAX_LOAD(id = 15),

    P_ATK(id = 17),
    ATK_SPD(id = 18),
    P_DEF(id = 19),
    EVASION(id = 20),
    ACCURACY(id = 21),
    CRITICAL(id = 22),
    M_ATK(id = 23),
    CAST_SPD(id = 24),
    M_DEF(id = 25),

    PVP_FLAG(id = 26),
    KARMA(id = 27),

    CUR_CP(id = 33),
    MAX_CP(id = 34),
}
