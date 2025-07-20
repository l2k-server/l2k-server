package org.l2kserver.login.network

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.bits.reverseByteOrder
import io.ktor.utils.io.readShort
import io.ktor.utils.io.writeFully
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.l2kserver.login.extensions.getScrambledModulus
import org.l2kserver.login.extensions.logger
import org.l2kserver.login.extensions.readByteArray
import org.l2kserver.login.handler.L2LoginHandler
import org.l2kserver.login.handler.dto.request.RequestPacket
import org.l2kserver.login.handler.dto.response.InitPacket
import org.l2kserver.login.repository.SessionRepository
import org.l2kserver.login.repository.domain.SessionData
import org.l2kserver.login.security.CryptUtils
import org.l2kserver.login.utils.IdUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.Executors

@Component
class L2LoginTcpServer(
    private val handler: L2LoginHandler,
    private val sessionRepository: SessionRepository,

    @Value("\${server.port}") private val port: Int,
    @Value("\${server.readTimeout}") private val readTimeout: Long
) {
    private val log = logger()

    private val selectorManager = SelectorManager(Dispatchers.IO)
    private val executor = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    @PostConstruct
    fun start() = executor.launch {
        val serverSocket = aSocket(selectorManager).tcp().bind(port = port)
        log.info("Server is listening on port $port")

        while (isActive) {
            val socket = serverSocket.accept()
            log.info("Got connection {}", socket.remoteAddress)

            val readChannel = socket.openReadChannel()
            val sendChannel = socket.openWriteChannel(autoFlush = true)

            val keyPair = CryptUtils.getRandomKeyPair()
            val blowfishKey = CryptUtils.getRandomBlowfishKey()
            val sessionId = IdUtils.getId()

            sessionRepository.save(SessionData(sessionId = sessionId))

            sendChannel.writeFully(
                InitPacket(
                    publicKey = (keyPair.public as RSAPublicKey).getScrambledModulus(),
                    blowfishKey = blowfishKey,
                    sessionId = sessionId
                ).getEncryptedData()
            )

            CoroutineScope(Dispatchers.Default).launch {
                try {
                    while (coroutineContext.isActive) {
                        val dataSize = readChannel.readShort().reverseByteOrder()

                        val data = withTimeout(readTimeout) {
                            readChannel.readByteArray(dataSize - Short.SIZE_BYTES)
                        }

                        val request = RequestPacket.fromByteArray(
                            data, blowfishKey, keyPair.private as RSAPrivateKey
                        )

                        log.debug("Got request {}", request)
                        sendChannel.writeFully(handler.handle(sessionId, request).getEncryptedData(blowfishKey))
                    }
                } catch (_: ClosedReceiveChannelException) {
                } catch (e: Exception) {
                    log.error(
                        "An error occurred on handling connection with {}",
                        socket.remoteAddress, e
                    )
                } finally {
                    log.info("Disconnected {}", socket.remoteAddress)
                    sessionRepository.deleteById(sessionId)
                    socket.close()
                }
            }
        }
    }

    @PreDestroy
    fun stop() {
        executor.cancel("The server is stopped")

        selectorManager.close()
        log.debug("The server is stopped")
    }

}
