package org.l2kserver.login.domain

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object AccountsTable: LongIdTable() {
    val login = varchar("login", 16)
    val password = varchar("password", 32)
    val email = varchar("email", 64).nullable()
    val creationTime = datetime("creation_time")
    val lastActive = datetime("last_active")
    val accessLevel = short("access_level")
    val lastIp = varchar("last_ip", 15)
    val lastServer = short("last_server").nullable()
}

class Account(id: EntityID<Long>): LongEntity(id) {
    companion object: EntityClass<Long, Account>(AccountsTable)

    var login by AccountsTable.login
    var password by AccountsTable.password
    var email by AccountsTable.email
    var creationTime by AccountsTable.creationTime
    var lastActive by AccountsTable.lastActive
    var accessLevel by AccountsTable.accessLevel
    var lastIp by AccountsTable.lastIp
    var lastServer by AccountsTable.lastServer

}
