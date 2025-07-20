package org.l2kserver.game.domain

import org.jetbrains.exposed.sql.Table
import org.postgresql.util.PGobject

class PostgresEnum<T : Enum<T>>(enumTypeName: String, enumValue: T?) : PGobject() {
    init {
        value = enumValue?.name
        type = enumTypeName
    }
}

/**
 * Create enumeration colum by existing in postgres database enum type
 *
 * @param columnName Name of column
 * @param enumerationName Name of enum type at database
 */
inline fun <reified E: Enum<E>> Table.postgresEnumeration(
    columnName: String, enumerationName: String
) = customEnumeration(
    name = columnName,
    sql = enumerationName,
    fromDb = { value -> enumValues<E>().find { it.name == value }!! },
    toDb = { PostgresEnum(enumerationName, it) }
)
