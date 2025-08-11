package org.l2kserver.login.configuration

import org.junit.jupiter.api.Order
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

private const val DATABASE_USER = "l2k_test"
private const val DATABASE_PASSWORD = "l2k_test"
private const val DATABASE_NAME = "l2k_game_test_db"

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
class PostgresContainerConfiguration {

    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> =
        PostgreSQLContainer(DockerImageName.parse(PostgreSQLContainer.IMAGE))
            .withUsername(DATABASE_USER)
            .withPassword(DATABASE_PASSWORD)
            .withDatabaseName(DATABASE_NAME)
            .withEnv(mapOf("POSTGRES_HOST_AUTH_METHOD" to "trust"))

}
