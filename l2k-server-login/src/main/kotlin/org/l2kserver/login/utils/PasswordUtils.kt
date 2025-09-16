package org.l2kserver.login.utils

import java.security.MessageDigest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object PasswordUtils {

    private const val ALGORITHM = "SHA"

    @OptIn(ExperimentalEncodingApi::class)
    fun encode(password: String): String = Base64.encode(
        MessageDigest.getInstance(ALGORITHM).digest(password.toByteArray())
    )

}
