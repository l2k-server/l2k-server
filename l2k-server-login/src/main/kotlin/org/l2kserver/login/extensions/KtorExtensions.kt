package org.l2kserver.login.extensions

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readFully

suspend fun ByteReadChannel.readByteArray(size: Int) = ByteArray(size).also { this.readFully(it) }
