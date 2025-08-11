package org.l2kserver.login.utils

import java.security.MessageDigest
import java.util.*

object PasswordUtils {

    private const val ALGORITHM = "SHA"

    fun encode(password: String): String = Base64
        .getEncoder()
        .encodeToString(
            MessageDigest
                .getInstance(ALGORITHM)
                .digest(password.toByteArray(Charsets.UTF_8))
        )

}
