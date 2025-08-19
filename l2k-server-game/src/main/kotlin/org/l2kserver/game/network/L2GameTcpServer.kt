package org.l2kserver.game.network

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.bits.reverseByteOrder
import io.ktor.utils.io.readShort
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writeShort
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.network.session.SessionContext
import org.l2kserver.game.extensions.readBytes
import org.l2kserver.game.handler.L2GameRequestHandler
import org.l2kserver.game.handler.dto.response.ResponsePacket
import org.l2kserver.game.security.GameCrypt
import org.l2kserver.game.utils.CyclicIdIterator
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import kotlinx.coroutines.slf4j.MDCContext
import org.l2kserver.game.handler.dto.request.RequestPacket
import org.l2kserver.game.network.session.sessionContext
import org.slf4j.MDC

private const val MIN_PACKET_SIZE = 3
private const val MAX_PACKET_SIZE = 65535

@Component
class L2GameTcpServer(
    private val l2GameRequestHandler: L2GameRequestHandler,

    @Value("\${server.port}") private val port: Int,
    @Value("\${server.readTimeout}") private val readTimeout: Long
) {

    private val log = logger()

    private val selectorManager = SelectorManager(Dispatchers.IO)
    private val executor = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    private val idIterator = CyclicIdIterator()

    @PostConstruct
    fun start() = executor.launch {
        val serverSocket = aSocket(selectorManager).tcp().bind(port = port)
        log.info("Server is listening on port $port")

        while (isActive) {
            val socket = serverSocket.accept()
            log.info("Got connection {}", socket.remoteAddress)

            val readChannel = socket.openReadChannel()
            val sendChannel = socket.openWriteChannel(autoFlush = true)

            val gameCrypt = GameCrypt()
            val sessionId = idIterator.next()

            val context = createContext(sessionId)

            MDC.put("remote", "[${socket.remoteAddress}] ")

            val sendingJob = context.launchSendingJob(sessionId, gameCrypt, sendChannel)
            context.launch {
                try {
                    while (coroutineContext.isActive) {
                        //L2 uses LittleEndian byte order
                        val dataSize = readChannel.readShort().reverseByteOrder().toUShort().toInt()
                        require(dataSize in MIN_PACKET_SIZE..MAX_PACKET_SIZE) {
                            "Client tries to send too huge packet"
                        }

                        val data = withTimeout(readTimeout) {
                            readChannel.readBytes(dataSize - Short.SIZE_BYTES)
                        }

                        val request = runCatching { RequestPacket(gameCrypt.decrypt(data)) }.getOrNull()
                        l2GameRequestHandler.handle(gameCrypt.initialKey, request)
                    }
                } catch (_: ClosedReceiveChannelException) {
                } catch (e: Throwable) {
                    log.error("An error occurred on handling connection with {}", socket.remoteAddress, e)
                } finally {
                    log.info("Disconnected {}", socket.remoteAddress)
                    l2GameRequestHandler.handleDisconnect()

                    delay(2000) // wait for 2 seconds to send remaining packets
                    sendingJob.cancel()
                    socket.close()
                }
            }
        }

        log.info("GameServer executor is cancelled")
        serverSocket.close()
    }

    @PreDestroy
    fun stop() {
        executor.cancel("The server is stopped")

        selectorManager.close()
        log.info("The server is stopped")
    }

    private fun CoroutineScope.launchSendingJob(
        sessionId: Int,
        gameCrypt: GameCrypt,
        sendChannel: ByteWriteChannel
    ) = launch {
        log.debug("Started response sending job for session {}", sessionId)
        val context = sessionContext()
        for (responsePacket in context.responseChannel) {
            log.debug("Sending response '{}'", responsePacket)
            val responseData = gameCrypt.encrypt(responsePacket.data)

            //L2 uses LittleEndian byte order
            sendChannel.writeShort((responseData.size + ResponsePacket.HEADER_SIZE).toShort().reverseByteOrder())
            sendChannel.writeFully(responseData)
        }
    }

    private fun createContext(sessionId: Int) = CoroutineScope(
        Dispatchers.Default + SessionContext(sessionId) + MDCContext()
    )

}
