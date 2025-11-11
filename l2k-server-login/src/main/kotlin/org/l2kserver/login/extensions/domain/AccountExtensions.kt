package org.l2kserver.login.extensions.domain

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.l2kserver.login.domain.Account
import org.l2kserver.login.domain.AccountsTable
import org.l2kserver.login.utils.PasswordUtils
import java.time.LocalDateTime

/**
 * Creates new account with provided login, password and accessLevel
 */
suspend fun Account.Companion.create(
    login: String,
    password: String,
    accessLevel: Short = 0
) = suspendTransaction {
    val accountId = AccountsTable.insertAndGetId { statement ->
        statement[AccountsTable.login] = login
        statement[AccountsTable.password] = PasswordUtils.encode(password)
        statement[creationTime] = LocalDateTime.now()
        statement[lastActive] = LocalDateTime.now()
        statement[AccountsTable.accessLevel] = accessLevel
    }

    findById(accountId)!!
}

suspend fun Account.Companion.findByLogin(login: String) = suspendTransaction {
    find { AccountsTable.login eq login }.firstOrNull()
}

suspend fun Account.updateLastActive() = suspendTransaction {
    lastActive = LocalDateTime.now()
}
