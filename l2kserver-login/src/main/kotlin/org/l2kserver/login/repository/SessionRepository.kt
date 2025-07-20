package org.l2kserver.login.repository

import org.l2kserver.login.repository.domain.SessionData
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class SessionRepository {

    private val sessions = ConcurrentHashMap<Int, SessionData>()

    /**
     * Saves session data to the storage
     *
     * @param session session data
     *
     * @return Saved entity
     */
    fun save(session: SessionData): SessionData {
        sessions[session.sessionId] = session

        return session
    }

    /**
     * @return SessionData for this id
     *
     * @throws IllegalStateException if no session found for this id
     */
    fun findById(sessionId: Int): SessionData = checkNotNull(sessions[sessionId]) {
        "No session with id=$sessionId found!!"
    }

    /**
     * @return true if session with this username exists, false if not
     */
    fun existsByLogin(username: String): Boolean = sessions.values.any { it.login == username }

    fun delete(session: SessionData) = sessions.remove(session.sessionId)

    fun deleteById(sessionId: Int) = sessions.remove(sessionId)

}
