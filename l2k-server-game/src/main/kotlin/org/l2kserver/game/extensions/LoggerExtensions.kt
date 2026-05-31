package org.l2kserver.game.extensions

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging

inline fun <reified T> T.logger(): KLogger = KotlinLogging.logger(T::class.java.name)
