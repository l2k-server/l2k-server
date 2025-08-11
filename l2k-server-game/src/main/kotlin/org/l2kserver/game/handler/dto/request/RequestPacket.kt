package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer
import org.l2kserver.game.extensions.getUByte
import org.l2kserver.game.extensions.getUShort
import org.l2kserver.game.extensions.littleEndianByteBuffer

private const val EXTENDED_PACKET: UByte = 208u

sealed interface RequestPacket
sealed interface ExtendedRequestPacket: RequestPacket

fun RequestPacket(data: ByteArray): RequestPacket {
    val buffer = littleEndianByteBuffer(data)

    return when (val requestPacketId = buffer.getUByte()) {
        INITIAL_REQUEST_PACKET_ID -> InitialRequest(buffer)
        AUTH_REQUEST_PACKET_ID -> AuthorizationRequest(buffer)
        LOGOUT_REQUEST_PACKET_ID -> LogoutRequest
        RESTART_REQUEST_PACKET_ID -> RestartRequest

        CHARACTER_TEMPLATES_REQUEST_PACKET_ID -> CharacterTemplatesRequest
        CREATE_CHARACTER_REQUEST_PACKET_ID -> CreateCharacterRequest(buffer)
        DELETE_CHARACTER_REQUEST_PACKET_ID -> DeleteCharacterRequest(buffer)
        RESTORE_CHARACTER_REQUEST_PACKET_ID -> RestoreCharacterRequest(buffer)
        SELECT_CHARACTER_REQUEST_PACKET_ID -> SelectCharacterRequest(buffer)
        ENTER_WORLD_REQUEST_PACKET_ID -> EnterWorldRequest

        CHAT_MESSAGE_REQUEST_PACKET_ID -> ChatMessageRequest(buffer)
        BASIC_ACTION_REQUEST_PACKET_ID -> BasicActionRequest(buffer)
        SOCIAL_ACTION_REQUEST_PACKET_ID -> SocialActionRequest(buffer)

        RESPAWN_REQUEST_PACKET_ID -> RespawnRequest(buffer)

        USE_ITEM_REQUEST_PACKET_ID -> UseItemRequest(buffer)
        DISARM_ITEM_REQUEST_PACKET_ID -> TakeOffItemRequest(buffer)
        DELETE_ITEM_REQUEST_PACKET_ID -> DeleteItemRequest(buffer)
        DROP_ITEM_REQUEST_PACKET_ID -> DropItemRequest(buffer)

        USE_SKILL_REQUEST_PACKET_ID -> UseSkillRequest(buffer)
        SKILL_LIST_REQUEST_PACKET_ID -> SkillListRequest
        REGISTER_SHORTCUT_REQUEST_PACKET_ID -> CreateShortcutRequest(buffer)
        DELETE_SHORTCUT_REQUEST_PACKET_ID -> DeleteShortcutRequest(buffer)

        MOVE_REQUEST_PACKET_ID -> MoveRequest(buffer)
        VALIDATE_POSITION_REQUEST_PACKET_ID -> ValidatePositionRequest(buffer)

        ATTACK_REQUEST_PACKET_ID -> AttackRequest(buffer)
        ACTION_REQUEST_PACKET_ID -> ActionRequest(buffer)
        CANCEL_ACTION_REQUEST_PACKET_ID -> CancelActionRequest

        SHOW_MAP_REQUEST_PACKET_ID -> ShowMapRequest

        ADMIN_COMMAND_REQUEST_PACKET_ID -> AdminCommandRequest(buffer)
        USER_COMMAND_REQUEST_PACKET_ID -> UserCommandRequest(buffer)

        EXCHANGE_REQUEST_PACKET_ID -> ExchangeRequest(buffer)

        ITEM_LIST_FOR_PRIVATE_STORE_SELL_REQUEST_PACKET_ID -> ItemListForPrivateStoreSellRequest
        START_PRIVATE_STORE_SELL_REQUEST_PACKET_ID -> PrivateStoreSellStartRequest(buffer)
        STOP_PRIVATE_STORE_SELL_REQUEST_PACKET_ID -> PrivateStoreSellStopRequest
        SET_PRIVATE_STORE_SELL_MESSAGE_REQUEST_PACKET_ID -> PrivateStoreSellSetMessageRequest(buffer)
        BUY_IN_PRIVATE_STORE_REQUEST_PACKET_ID -> BuyInPrivateStoreRequest(buffer)

        ITEM_LIST_FOR_PRIVATE_STORE_BUY_REQUEST_PACKET_ID -> ItemListForPrivateStoreBuyRequest
        START_PRIVATE_STORE_BUY_REQUEST_PACKET_ID -> PrivateStoreBuyStartRequest(buffer)
        STOP_PRIVATE_STORE_BUY_REQUEST_PACKET_ID -> PrivateStoreBuyStopRequest
        SET_PRIVATE_STORE_BUY_MESSAGE_REQUEST_PACKET_ID -> PrivateStoreBuySetMessageRequest(buffer)
        SELL_TO_PRIVATE_STORE_BUY_REQUEST_PACKET_ID -> SellToPrivateStoreRequest(buffer)

        EXTENDED_PACKET -> ExtendedRequestPacket(buffer)

        else -> {
            System.err.println("Unknown packet type $requestPacketId")
            throw IllegalArgumentException("Unknown packet type '$requestPacketId'")
        }
    }
}

private fun ExtendedRequestPacket(
    data: ByteBuffer
): ExtendedRequestPacket = when(val extendedPacketRequestId = data.getUShort()) {
    MANOR_REQUEST_PACKET_ID -> ManorListRequest
    else -> {
        System.err.println("Unknown extended packet type $extendedPacketRequestId")
        throw IllegalArgumentException("Unknown extended packet type '$extendedPacketRequestId'")
    }
}
