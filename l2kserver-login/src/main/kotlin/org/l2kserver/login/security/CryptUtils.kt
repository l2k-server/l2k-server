package org.l2kserver.login.security

import com.l2jserver.commons.security.crypt.NewCrypt
import org.l2kserver.login.handler.dto.response.ResponsePacket
import java.nio.ByteBuffer
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.RSAKeyGenParameterSpec
import kotlin.random.Random

object CryptUtils {
    private const val BLOWFISH_KEY_SIZE = 16
    private const val RSA_KEY_SIZE = 1024
    private const val XOR_KEY_SIZE = Int.SIZE_BYTES
    private const val CHECKSUM_SIZE = Int.SIZE_BYTES

    private val INITIAL_BLOWFISH_KEY = byteArrayOf(
        107, 96, -53, 91, -126, -50, -112, -79, -52, 43, 108, 85, 108, 108, 108, 108
    )

    private val initCrypt = NewCrypt(INITIAL_BLOWFISH_KEY)
    private val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        .also { it.initialize(RSAKeyGenParameterSpec(RSA_KEY_SIZE, RSAKeyGenParameterSpec.F4)) }

    fun getRandomBlowfishKey(): ByteArray = Random.nextBytes(BLOWFISH_KEY_SIZE)
    fun getRandomKeyPair(): KeyPair = keyPairGenerator.generateKeyPair()


    //TODO Refactor to make data immutable
    /**
     * Encrypts initial packet. Initial packet must be encrypted with XOR and then initial blowfish key.
     * WARNING: Data is encrypted DIRECTLY at the data array
     *
     * @param dataBuffer data to encrypt
     *
     * @return data size after encryption
     */
    fun encryptInitPacketData(dataBuffer: ByteBuffer): Int {
        val data = dataBuffer.array()

        val offset = ResponsePacket.HEADER_SIZE

        var newSize = dataBuffer.position() + XOR_KEY_SIZE + CHECKSUM_SIZE
        newSize += 8 - (newSize % 8) //padding

        if ((offset + newSize) > data.size) throw IndexOutOfBoundsException("Packet is too large")

        NewCrypt.encXORPass(data, offset, newSize, Random.nextInt())
        initCrypt.crypt(data, offset, newSize)

        return newSize
    }

    //TODO Refactor to make data immutable
    /**
     * Encrypts response data.
     * WARNING: Data is encrypted DIRECTLY at the data array
     *
     * @param key - BlowfishKey for encryption
     * @param dataBuffer - Packet data buffer to encrypt
     *
     * @return encrypted data
     */
    fun encrypt(
        key: ByteArray,
        dataBuffer: ByteBuffer,
    ): Int {
        val data = dataBuffer.array()
        val offset = ResponsePacket.HEADER_SIZE
        val crypt = NewCrypt(key)

        var newSize = dataBuffer.position() - offset + CHECKSUM_SIZE
        newSize += 8 - newSize % 8 // padding

        if ((offset + newSize) > data.size) throw IndexOutOfBoundsException("Packet is too large")
        NewCrypt.appendChecksum(data, offset, newSize)
        crypt.crypt(data, offset, newSize)

        return newSize
    }

    //TODO Refactor to make data immutable
    /**
     * Decrypts request data
     *
     * @param key - BlowfishKey for decryption
     * @param data - Data to decrypt
     * @param offset offset where the encrypted data is located
     * @param size number of bytes of encrypted data
     *
     * @return decrypted data
     */
    fun decrypt(key: ByteArray, data: ByteArray, offset: Int = 0, size: Int = data.size - offset): ByteArray {
        val crypt = NewCrypt(key)

        crypt.decrypt(data, offset, size)
        return data
    }

}
