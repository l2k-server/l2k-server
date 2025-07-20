package org.l2kserver.login.extensions.domain

import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
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
) = newSuspendedTransaction {
    val accountId = AccountsTable.insertAndGetId { statement ->
        statement[AccountsTable.login] = login
        statement[AccountsTable.password] = PasswordUtils.encode(password)
        statement[creationTime] = LocalDateTime.now()
        statement[lastActive] = LocalDateTime.now()
        statement[AccountsTable.accessLevel] = accessLevel
    }

    findById(accountId)!!
}

suspend fun Account.Companion.findByLogin(login: String) = newSuspendedTransaction {
    find { AccountsTable.login eq login }.firstOrNull()
}

suspend fun Account.updateLastActive() = newSuspendedTransaction {
    lastActive = LocalDateTime.now()
}
