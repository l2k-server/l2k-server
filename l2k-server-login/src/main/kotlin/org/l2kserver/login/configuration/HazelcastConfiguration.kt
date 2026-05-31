package org.l2kserver.login.configuration

import com.hazelcast.client.Client
import com.hazelcast.client.ClientListener
import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import org.l2kserver.login.extensions.logger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class HazelcastConfiguration {

    @Bean(destroyMethod = "shutdown")
    fun hazelcast(): HazelcastInstance {
        val config = Config()
        config.clusterName = "L2K"
        val hazelcast = Hazelcast.newHazelcastInstance(config)

        hazelcast.clientService.addClientListener(object: ClientListener {
            private val log = logger()

            override fun clientConnected(client: Client) = log.info { "Connected gameserver ${client.name}" }

            override fun clientDisconnected(client: Client) {
                log.info { "Disconnected gameserver '${client.name}'" }
                hazelcast.getMap<String, String>("${client.name}-loggedInUsers").clear()
            }
        })

        return hazelcast
    }

}
