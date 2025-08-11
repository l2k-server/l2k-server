package org.l2kserver.game.extensions

import java.nio.ByteBuffer
import java.nio.ByteOrder

inline fun littleEndianByteArray(bufferBuilderFunction: ByteBuffer.() -> Unit): ByteArray {
    val buffer = ByteBuffer.allocate(UShort.MAX_VALUE.toInt()).order(ByteOrder.LITTLE_ENDIAN)
    buffer.bufferBuilderFunction()

    return ByteArray(buffer.position()).also { buffer.position(0).get(it) }
}

fun littleEndianByteBuffer(data: ByteArray): ByteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

/**
 * Put string to this buffer
 */
fun ByteBuffer.putUTF16String(str: CharSequence?): ByteBuffer {
    str?.forEach { this.putShort(it.code.toShort()) }
    this.putShort(Char.MIN_VALUE.code.toShort())

    return this
}

/**
 * Put unsigned byte to this buffer
 */
fun ByteBuffer.putUByte(uByte: UByte): ByteBuffer = this.put(uByte.toByte())

/**
 * Reads UByte from this buffer
 */
fun ByteBuffer.getUByte() = this.get().toUByte()

/**
 * Reads UTF-16 line from this buffer
 */
fun ByteBuffer.getUTF16String(): String {
    val sb = StringBuilder()

    while (true) {
        val charCode = this.getShort().toInt()
        if (charCode == Char.MIN_VALUE.code) break
        sb.append(Char(charCode))
    }

    return sb.toString()
}

/**
 * Reads Boolean from this packet
 */
fun ByteBuffer.getBoolean() = this.get().toInt() != 0

/**
 * Reads UShort from this buffer
 */
fun ByteBuffer.getUShort() = this.getShort().toUShort()

/**
 * Put unsigned short to this buffer
 */
fun ByteBuffer.putUShort(uShort: UShort): ByteBuffer = this.putShort(uShort.toShort())
