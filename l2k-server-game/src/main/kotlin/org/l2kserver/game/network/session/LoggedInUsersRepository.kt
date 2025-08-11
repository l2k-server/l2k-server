package org.l2kserver.game.network.session

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hazelcast.core.HazelcastInstance
import org.l2kserver.game.model.session.LoggedInUserData
import org.springframework.stereotype.Component

@Component
class LoggedInUsersRepository(
    hazelcast: HazelcastInstance
) {
    private val loggedInUsers = hazelcast.getMap<String, String>("${hazelcast.name}-loggedInUsers")
    private val objectMapper = jacksonObjectMapper()

    fun save(login: String, loggedInUser: LoggedInUserData) {
        loggedInUsers[login] = objectMapper.writeValueAsString(loggedInUser)
    }

    /**
     * Finds login session data by the username
     *
     * @param login User name
     * @return SessionKeyDAO if there was session key, else null
     */
    fun findByLogin(login: String): LoggedInUserData? {
        val sessionKeysJson = loggedInUsers[login] ?: return null

        return objectMapper.readValue<LoggedInUserData>(sessionKeysJson)
    }

    /**
     * Deletes login session data by the username
     *
     * @param login User name
     */
    fun deleteByLogin(login: String) = loggedInUsers.remove(login)

}
