package org.l2kserver.login.utils

import java.net.Socket

object ServerUtils {

    /**
     * Checks if remote is available
     *
     * @param ip Remote host
     * @param port Remote port
     *
     * @return true if remote is available, false - if not
     */
    fun checkOnline(ip: String, port: Int) = runCatching { Socket(ip, port).close() }.isSuccess
}
