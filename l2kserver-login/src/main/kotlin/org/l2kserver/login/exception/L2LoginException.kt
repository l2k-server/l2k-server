package org.l2kserver.login.exception

/**
 * Light exception with no stacktrace
 *
 * @param message Error message
 */
class L2LoginException(message: String): RuntimeException(message) {

    override fun fillInStackTrace() = this

}
