/**
 * This code is part of L2K project and is licensed under GPL v3.
 * See the LICENSE file in the root directory for details.
 */
package org.l2kserver.login

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class L2kServerLoginApplication

fun main(args: Array<String>) {
    runApplication<L2kServerLoginApplication>(*args)
}
