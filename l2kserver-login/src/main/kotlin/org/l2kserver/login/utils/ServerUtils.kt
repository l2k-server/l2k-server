package org.l2kserver.login.utils

import java.net.Socket

object ServerUtils {

    /**
     * Checks if remote is available
     *
     * @param ip Remote host
     * @param port Remote post
     *
     * @return true if remote is available, false - if not
     */
    fun checkOnline(ip: String, port: Int): Boolean = try {
        val socket = Socket(ip, port)
        socket.close()
        true
    }
    catch(_: Exception) {
        false
    }

}
