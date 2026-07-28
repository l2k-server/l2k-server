/**
 * This code is part of L2K project and is licensed under GPL v3.
 * See the LICENSE file in the root directory for details.
 */
package org.l2kserver.game

import org.jetbrains.exposed.v1.spring.boot4.autoconfigure.ExposedAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication
@ImportAutoConfiguration(
    value = [ExposedAutoConfiguration::class],
    exclude = [DataSourceTransactionManagerAutoConfiguration::class]
)
class L2KServerGameApplication

fun main(args: Array<String>) {
    runApplication<L2KServerGameApplication>(*args)
}
