package org.l2kserver.game.security

import kotlin.random.Random

private val STATIC_KEY_PART = byteArrayOf(-56, 39, -109, 1, -95, 108, 49, -105)

class GameCrypt {

    // Key for gameserver packets encryption/decryption.
    // Contains 16 bytes - first 8 bytes are randomly generated, last 8 are static
    val initialKey = Random.nextBytes(8) + STATIC_KEY_PART

    private val inKey = initialKey.clone()
    private val outKey = initialKey.clone()

    //Initial Response should not be encrypted
    private var enabled = false

    /**
     * Decrypts provided data and shifts key
     * @param data data to decrypt
     *
     * @return decrypted data
     */
    fun decrypt(data: ByteArray): ByteArray {
        if (!enabled) return data.clone()

        val decrypted = data.clone()

        var temp = 0
        for(i: Int in data.indices) {
            val temp2 = decrypted[i].toInt() and 0xFF
            decrypted[i] = (temp2 xor inKey[i and 0xF].toInt() xor temp).toByte()
            temp = temp2
        }

        shiftKey(inKey, decrypted.size)
        return decrypted
    }

    /**
     * Encrypts provided data and shifts key
     * @param data data to encrypt
     *
     * @return encrypted data
     */
    fun encrypt(data: ByteArray): ByteArray {
        if (!enabled) {
            enabled = true
            return data.clone()
        }

        val encrypted = data.clone()

        var temp = 0
        for(i: Int in data.indices) {
            val temp2 = encrypted[i].toInt() and 0xFF
            temp = (temp2 xor outKey[i and 0xF].toInt() xor temp).toByte().toInt()
            encrypted[i] = temp.toByte()
        }

        shiftKey(outKey, encrypted.size)
        return encrypted
    }

    private fun shiftKey(key: ByteArray, size: Int) {
        var old = key[8].toInt() and 0xff
        old = old or ((key[9].toInt() shl 8) and 0xff00)
        old = old or ((key[10].toInt() shl 16) and 0xff0000)
        old = old or ((key[11].toInt() shl 24) and -0x1000000)

        old += size

        key[8] = (old and 0xff).toByte()
        key[9] = ((old shr 8) and 0xff).toByte()
        key[10] = ((old shr 16) and 0xff).toByte()
        key[11] = ((old shr 24) and 0xff).toByte()
    }

}
