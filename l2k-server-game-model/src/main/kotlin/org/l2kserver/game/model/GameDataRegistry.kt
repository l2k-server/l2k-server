package org.l2kserver.game.model

import org.l2kserver.game.model.extensions.logger
import java.util.concurrent.ConcurrentHashMap

/**
 * Common class for game entity templates.
 *
 * @property id Template identifier
 */
interface GameData {
    val id: Int
}

/**
 * Storage for game entity templates
 */
abstract class GameDataRegistry<T: GameData>: Collection<T> {

    private val gameDataStorage = ConcurrentHashMap<Int, T>()
    private val log = logger()

    override val size: Int get() = gameDataStorage.size
    override fun contains(element: T) = gameDataStorage.contains(element)
    override fun containsAll(elements: Collection<T>) = elements.all { gameDataStorage.contains(it) }
    override fun isEmpty() = gameDataStorage.isEmpty()
    override fun iterator(): Iterator<T> = gameDataStorage.values.iterator()

    /** Saves [gameData] to storage and returns it */
    fun register(gameData: T): T {
        val prevData = gameDataStorage.put(gameData.id, gameData)

        if (prevData == null) log.info("Successfully registered '{}'", gameData)
        else log.warn("{} was overridden with {}", prevData, gameData)

        return gameData
    }

    /** Saves [gameData] to storage and returns it */
    fun register(vararg gameData: T): List<T> {
        return gameData.map { register(it) }
    }

    /** Finds template by its identifier */
    fun findByIdOrNull(id: Int) = gameDataStorage[id]

    /** Finds template by its identifier, or throws [IllegalArgumentException] if no data with [id] exists */
    fun findById(id: Int) = requireNotNull(findByIdOrNull(id)) { "No GameData found by id=$id" }

    /** Checks if game data object with provided [id] is registered */
    fun existsById(id: Int) = gameDataStorage.containsKey(id)
}
