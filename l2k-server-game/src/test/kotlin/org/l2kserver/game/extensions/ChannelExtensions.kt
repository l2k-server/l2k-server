package org.l2kserver.game.extensions

import io.ktor.util.reflect.instanceOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import org.l2kserver.game.handler.dto.response.ResponsePacket
import kotlin.reflect.KClass

/**
 * Receive next packet from channel, ignoring instances of provided classes
 */
suspend fun Channel<ResponsePacket>.receiveIgnoring(
    vararg ignoredResponseClasses: KClass<out ResponsePacket>,
    timeout: Long = 1000L
): ResponsePacket {
    return this.next(timeout).takeIf { packet -> ignoredResponseClasses.none { packet.instanceOf(it)  }}
        ?: receiveIgnoring(*ignoredResponseClasses)
}

/**
 * Receive next packet from channel
 *
 * @param timeout Timeout of receiving packet
 */
suspend fun Channel<ResponsePacket>.next(timeout: Long = 5_000L) = withTimeout(timeout) {
    this@next.receive()
}
