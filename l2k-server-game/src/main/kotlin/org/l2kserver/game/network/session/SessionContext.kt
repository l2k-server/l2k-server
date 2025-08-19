package org.l2kserver.game.network.session

import kotlinx.coroutines.channels.BufferOverflow
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.channels.Channel
import org.l2kserver.game.handler.dto.response.ResponsePacket
import org.l2kserver.game.model.session.AuthorizationKey
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

private const val RESPONSE_CHANNEL_CAPACITY = 127

/** Storage for all sessions. Key - session id, value - session context */
private val sessionsMap = ConcurrentHashMap<Int, SessionContext>()

/** Storage for sessions of players, who entered game. Key - character id, value - session context */
private val inGameSessionsMap = ConcurrentHashMap<Int, SessionContext>()

/**
 * Coroutine context with session data
 *
 * @property sessionId Session identifier
 * @property accountName Name of the player's account
 * @property authorizationKey Authorization key (given from Login Server)
 * @property characterId Selected character's id
 */
class SessionContext(val sessionId: Int) : CoroutineContext.Element, Closeable {

    companion object Key : CoroutineContext.Key<SessionContext>, Iterable<SessionContext> {
        override fun iterator() = sessionsMap.values.iterator()

        fun getById(sessionId: Int) = checkNotNull(sessionsMap[sessionId]) {
            "No session exist by id $sessionId"
        }

        fun clear() = sessionsMap.clear()
    }

    init {
        sessionsMap[sessionId] = this
    }

    override val key: CoroutineContext.Key<*> = SessionContext

    private var accountName: String? = null

    /**
     * @return account name
     * @throws IllegalStateException if played is not authorized
     */
    fun getAccountName() = checkNotNull(accountName) { "No account name found in session. Is user authorized?" }

    /** Set account name */
    fun setAccountName(value: String) { accountName = value }

    /** Returns account name or null, if there is no account name in session */
    fun getAccountNameOrNull() = accountName

    private var authorizationKey: AuthorizationKey? = null

    /**
     * @return authorization key
     * @throws IllegalStateException if player is not authorized
     */
    fun getAuthorizationKey() = checkNotNull(authorizationKey) { "No authorization key found in session. Is user authorized?" }

    /** Set authorization key */
    fun setAuthorizationKey(value: AuthorizationKey) { authorizationKey = value }

    private var characterId: Int? = null

    /**
     * @return Selected character ID
     * @throws IllegalStateException if player has not selected character
     */
    fun getCharacterId() = checkNotNull(characterId) { "Player $accountName has not selected character" }

    /** Set selected character id */
    fun setCharacterId(value: Int?) {
        if (value == null) characterId?.let { inGameSessionsMap.remove(it) }
        else inGameSessionsMap[value] = this

        characterId = value
    }

    /** Returns selected character id or null, if there is no selected character id in session */
    fun getCharacterIdOrNull() = characterId

    val responseChannel = Channel<ResponsePacket>(RESPONSE_CHANNEL_CAPACITY, BufferOverflow.DROP_OLDEST)

    /** Returns`true` if player is now in characters menu or creates character */
    fun inCharacterMenu(): Boolean = accountName != null && characterId == null

    /** Flush player's session data */
    override fun close() {
        characterId?.let { inGameSessionsMap.remove(it) }
        sessionsMap.remove(sessionId)
    }
}

/**
 * Get SessionContext
 *
 * @throws IllegalStateException if current coroutine scope has no SessionContext
 */
suspend inline fun sessionContext() = checkNotNull(coroutineContext[SessionContext]) {
    "Coroutine is not in SessionContext"
}

/** Sends [responses] to the current session owner. If no SessionContext found - does nothing */
suspend inline fun send(vararg responses: ResponsePacket) = responses.forEach {
    coroutineContext[SessionContext]?.responseChannel?.send(it)
}

/** Sends [responses] to character with id = [addresseeCharacterId]. If no characters session found, does nothing */
suspend fun sendTo(addresseeCharacterId: Int, vararg responses: ResponsePacket) = responses.forEach {
    inGameSessionsMap[addresseeCharacterId]?.responseChannel?.send(it)
}
