package org.l2kserver.game.configuration

import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import org.l2kserver.game.extensions.logger
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class HazelcastInstanceTestConfiguration {
    private val log = logger()

    @Bean(destroyMethod = "shutdown")
    fun hazelcast(): HazelcastInstance {
        val config = Config()
        config.clusterName = "L2K"

        return Hazelcast.newHazelcastInstance(config).also { log.info("Started TEST hazelcast instance") }
    }

}
