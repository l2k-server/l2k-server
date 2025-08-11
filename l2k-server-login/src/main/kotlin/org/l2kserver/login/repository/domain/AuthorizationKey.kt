package org.l2kserver.login.repository.domain

import org.l2kserver.login.utils.IdUtils

data class AuthorizationKey(
    val loginSessionKey1: Int,
    val loginSessionKey2: Int,
    val gameSessionKey1: Int,
    val gameSessionKey2: Int
) {
    constructor(): this(IdUtils.getId(), IdUtils.getId(), IdUtils.getId(), IdUtils.getId())
}
