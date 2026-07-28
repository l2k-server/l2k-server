package org.l2kserver.game.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
class PostgresContainerConfiguration(
    @param:Value($$"${database.url}") private val databaseUrl: String,
    @param:Value($$"${database.user}") private val databaseUser: String,
    @param:Value($$"${database.password}") private val databasePass: String
) {

    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> =
        PostgreSQLContainer(DockerImageName.parse(PostgreSQLContainer.IMAGE))
            .withUsername(databaseUser)
            .withPassword(databasePass)
            .withDatabaseName(databaseUrl.substringAfter("/"))
            .withEnv(mapOf("POSTGRES_HOST_AUTH_METHOD" to "trust"))

}
