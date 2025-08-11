package org.l2kserver.game.extensions

import io.ktor.util.reflect.instanceOf
import kotlinx.coroutines.channels.Channel
import org.l2kserver.game.handler.dto.response.ResponsePacket
import kotlin.reflect.KClass

/**
 * Receive next packet from channel, ignoring instances of provided classes
 */
suspend fun Channel<ResponsePacket>.receiveIgnoring(
    vararg ignoredResponseClasses: KClass<out ResponsePacket>
): ResponsePacket {
    return this.receive().takeIf { packet -> ignoredResponseClasses.none { packet.instanceOf(it)  }}
        ?: receiveIgnoring(*ignoredResponseClasses)
}
