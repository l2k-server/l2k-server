package org.l2kserver.login.configuration

import jakarta.annotation.PostConstruct
import org.l2kserver.login.extensions.logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "server")
@Configuration
class GameserversConfiguration @Autowired constructor(
    val gameservers: List<GameserverSettings>
) {

    private val log = logger()

    @PostConstruct
    fun init() = gameservers.forEach { gameserverSettings ->
        require(gameserverSettings.ip.split('.').map { it.toByte() }.toByteArray().size == 4) {
            "Wrong ip address ${gameserverSettings.ip}"
        }
        log.info("Registered gameserver: {}", gameserverSettings)
    }

    @Bean(name = ["gameserverSettings"])
    fun gameserverSettings() = gameservers

}

/**
 * Information about registered gameserver settings
 *
 * @param name Gameserver name - Name of gameserver
 * (needed for hazelcast, it is not the name displayed at server list on game client)
 * @param id Gameserver id - would be mapped to serverName at client side (for example 1 is Bartz by default)
 * @param ip Gameserver ip - gameserver IPv4 address (for example 127.0.0.1)
 * @param port Gameserver port
 * @param ageLimit AgeLimit to join this server (seems useless). Should be 0, 15 or 18. Default - 0
 * @param isPvp Is this server a pvp server. Default - false
 * @param maxPlayers How many players are allowed to play simultaneously on your server. Default - 100
 * @param accessLevel Required minimum access level to join server. Default - 0
 */
data class GameserverSettings(
    val name: String,
    val id: Byte,
    val ip: String,
    val port: Int,
    val ageLimit: Byte = 0,
    val isPvp: Boolean = false,
    val maxPlayers: Short = 100,
    val accessLevel: Byte = 0
)
