package org.l2kserver.login.utils

import java.util.concurrent.atomic.AtomicInteger

object IdUtils {
    val id = AtomicInteger(Int.MIN_VALUE)

    fun getId() = id.getAndIncrement()
}
