package org.l2kserver.login.handler.dto.request

import org.l2kserver.login.exception.L2LoginException
import org.l2kserver.login.security.CryptUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.interfaces.RSAPrivateKey
import java.util.*
import javax.crypto.Cipher

sealed interface RequestPacket {

    object OpCode {
        const val AUTH_GAME_GUARD: Byte = 0x07
        const val REQUEST_AUTH_LOGIN: Byte = 0x00
        const val REQUEST_SELECT_GAMESERVER: Byte = 0x02
        const val REQUEST_SERVER_LIST: Byte = 0x05
    }

    companion object {

        /**
         * Transforms received bytes to RequestPacket
         * @param bytes - decoded request data in bytes
         * @return RequestPacket of certain type or null, if parsing failed
         */
        fun fromByteArray(bytes: ByteArray, blowfishKey: ByteArray, rsaPrivateKey: RSAPrivateKey): RequestPacket {

            val decryptedData = CryptUtils.decrypt(
                key = blowfishKey,
                data = bytes
            )

            val byteBuffer = ByteBuffer.wrap(decryptedData).order(ByteOrder.LITTLE_ENDIAN)
            byteBuffer.position(0)

            return when(byteBuffer.get()/* read opcode */) {
                OpCode.AUTH_GAME_GUARD -> {
                    // AuthGameGuardPacket contains more data, but it is unused
                    AuthGameGuardRequest(byteBuffer.int)
                }
                OpCode.REQUEST_AUTH_LOGIN -> {
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
                OpCode.REQUEST_SERVER_LIST -> {
                    val loginSessionKey1 = byteBuffer.getInt()
                    val loginSessionKey2 = byteBuffer.getInt()

                    ServerListRequest(loginSessionKey1, loginSessionKey2)
                }
                OpCode.REQUEST_SELECT_GAMESERVER -> {
                    val loginSessionKey1 = byteBuffer.getInt()
                    val loginSessionKey2 = byteBuffer.getInt()
                    val selectedGameserverId = byteBuffer.get()

                    SelectGameserverRequest(loginSessionKey1, loginSessionKey2, selectedGameserverId)
                }
                else -> throw L2LoginException("Failed decoding packet ${bytes.contentToString()}")
            }
        }
    }

}
