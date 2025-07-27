package org.l2kserver.login.handler.dto.request

import org.l2kserver.login.exception.L2LoginException
import org.l2kserver.login.security.CryptUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.interfaces.RSAPrivateKey
import java.util.*
import javax.crypto.Cipher

sealed interface RequestPacket

fun RequestPacket(bytes: ByteArray, blowfishKey: ByteArray, rsaPrivateKey: RSAPrivateKey): RequestPacket {

    val decryptedData = CryptUtils.decrypt(
        key = blowfishKey,
        data = bytes
    )

    val byteBuffer = ByteBuffer.wrap(decryptedData).order(ByteOrder.LITTLE_ENDIAN)
    byteBuffer.position(0)

    return when(byteBuffer.get()/* read opcode */) {
        AUTH_GAME_GUARD_REQUEST_PACKET_ID -> {
            // AuthGameGuardPacket contains more data, but it is unused
            AuthGameGuardRequest(byteBuffer.int)
        }
        AUTH_LOGIN_REQUEST_PACKET_ID -> {
            val rsaCipher = Cipher.getInstance("RSA/ECB/nopadding")
            rsaCipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey)

            if (byteBuffer.remaining() < 128)
                throw L2LoginException("Too small AuthLoginRequestPacket data")

            val packetData = ByteArray(128)
            byteBuffer.get(packetData)

            val decrypted = rsaCipher.doFinal(packetData, 0x00, 0x80)

            val user = String(decrypted, 0x5E, 14).trim { it <= ' ' }.lowercase(Locale.getDefault())
            val password = String(decrypted, 0x6C, 16).trim { it <= ' ' }

            AuthLoginRequest(user, password)
        }
        SERVER_LIST_REQUEST_PACKET_ID -> {
            val loginSessionKey1 = byteBuffer.getInt()
            val loginSessionKey2 = byteBuffer.getInt()

            ServerListRequest(loginSessionKey1, loginSessionKey2)
        }
        SELECT_GAMESERVER_REQUEST_PACKET_ID -> {
            val loginSessionKey1 = byteBuffer.getInt()
            val loginSessionKey2 = byteBuffer.getInt()
            val selectedGameserverId = byteBuffer.get()

            SelectGameserverRequest(loginSessionKey1, loginSessionKey2, selectedGameserverId)
        }
        else -> throw L2LoginException("Failed decoding packet ${bytes.contentToString()}")
    }
}
