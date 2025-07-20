package org.l2kserver.login.repository

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.hazelcast.core.HazelcastInstance
import org.l2kserver.login.configuration.GameserverSettings
import org.l2kserver.login.repository.domain.LoggedInUserData
import org.springframework.stereotype.Component

@Component
class LoggedInUsersRepository (
    hazelcast: HazelcastInstance,
    gameserverSettings: List<GameserverSettings>
) {
    private val loggedInUsersMap = gameserverSettings
        .associate { it.name to hazelcast.getMap<String, String>("${it.name}-loggedInUsers") }
    private val objectMapper = jacksonObjectMapper()

    fun save(login:String, gameserverName: String, loggedInUserData: LoggedInUserData) {
        loggedInUsersMap[gameserverName]!![login] = objectMapper.writeValueAsString(loggedInUserData)
    }

    fun existsByLogin(login: String) = loggedInUsersMap.any { it.value.containsKey(login) }

    fun findByLogin(login: String, gameserverName:String): LoggedInUserData? = loggedInUsersMap[gameserverName]!![login]
        ?.let {
        objectMapper.readValue(it, LoggedInUserData::class.java)
    }

    fun deleteByLogin(login: String, gameserverName: String) = loggedInUsersMap[gameserverName]?.remove(login)
}
