package org.l2kserver.login.configuration

import jakarta.annotation.PostConstruct
import org.junit.jupiter.api.Order
import org.l2kserver.login.extensions.logger
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
class PostgresContainerConfiguration {
    private val log = logger()
    private val databaseContainer = createDatabaseContainer()

    @PostConstruct
    fun init() {
        databaseContainer.start()

        val host = databaseContainer.host
        val port = databaseContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)
        val databaseName = databaseContainer.databaseName

        System.setProperty("database.url", "$host:$port/$databaseName")
        System.setProperty("database.user", databaseContainer.username)
        System.setProperty("database.password", databaseContainer.password)

        databaseContainer.followOutput(Slf4jLogConsumer(log))
    }

    private fun createDatabaseContainer(): PostgreSQLContainer<*> {
        val envMap = HashMap<String, String>()
        envMap["POSTGRES_HOST_AUTH_METHOD"] = "trust"

        return KtPostgresDbContainer()
            .withUsername("postgres")
            .withPassword("postgres")
            .withDatabaseName("l2k_login_test_db")
            .waitingFor(HostPortWaitStrategy())
            .withEnv(envMap)
    }
}

class KtPostgresDbContainer: PostgreSQLContainer<KtPostgresDbContainer>(IMAGE)
