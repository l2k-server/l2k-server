package org.l2kserver.game.extensions

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readFully

/**
 * Creates new byte array of provided [size] and reads bytes to it
 */
suspend fun ByteReadChannel.readBytes(size: Int) = ByteArray(size).also { this.readFully(it) }
