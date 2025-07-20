package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer
import java.util.*
import org.l2kserver.game.extensions.getUTF16String
import org.l2kserver.game.model.session.AuthorizationKey

const val AUTH_REQUEST_PACKET_ID: UByte = 8u

/**
 * Authorization request
 *
 * @param login Account login
 * @param authorizationKey authorization key, given by LoginServer
 */
data class AuthorizationRequest(
    val login: String,
    val authorizationKey: AuthorizationKey
): RequestPacket {

    constructor(data: ByteBuffer): this(
        login = data.getUTF16String().trim { it <= ' ' }.lowercase(Locale.getDefault()),
        authorizationKey = AuthorizationKey(
            gameSessionKey2 = data.getInt(),
            gameSessionKey1 = data.getInt(),
            loginSessionKey1 = data.getInt(),
            loginSessionKey2 = data.getInt()
        )
    )

}
