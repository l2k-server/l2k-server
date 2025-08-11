package org.l2kserver.game.configuration

import com.hazelcast.client.HazelcastClient
import com.hazelcast.client.config.ClientConfig
import com.hazelcast.core.HazelcastInstance
import org.l2kserver.game.extensions.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class HazelcastConfiguration(
    @Value("\${server.name}") private val serverName: String
) {
    private val log = logger()

    @Bean
    fun hazelcast(): HazelcastInstance {
        val config = ClientConfig()
        config.instanceName = serverName
        config.clusterName = "L2K"

        return HazelcastClient.newHazelcastClient(config).also { log.info("Started GameServer hazelcast client") }
    }

}
