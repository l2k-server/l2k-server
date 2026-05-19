package org.l2kserver.game.extensions

import io.ktor.util.reflect.instanceOf
import kotlinx.coroutines.withTimeout
import org.l2kserver.game.handler.dto.response.ResponsePacket
import org.l2kserver.game.network.session.SessionContext
import kotlin.reflect.KClass

/**
 * Receive next packet from channel, ignoring instances of provided classes
 */
suspend fun SessionContext.pullResponse(
    vararg ignoredResponseClasses: KClass<out ResponsePacket>,
    timeout: Long = 1000L
): ResponsePacket {
    return this.pullResponse(timeout).takeIf { packet -> ignoredResponseClasses.none { packet.instanceOf(it)  }}
        ?: pullResponse(*ignoredResponseClasses)
}

/**
 * Receive next packet from channel
 *
 * @param timeout Timeout of receiving packet
 */
suspend fun SessionContext.pullResponse(timeout: Long = 5_000L) = withTimeout(timeout) {
    this@pullResponse.responseChannel.receive()
}
