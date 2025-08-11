package org.l2kserver.game.model

import java.util.concurrent.ConcurrentHashMap

/**
 * Common class for game entity templates.
 *
 * @property id Template identifies
 */
interface GameData {
    val id: Int
}

/**
 * Storage for game entity templates
 */
abstract class GameDataRegistry<T: GameData>: Iterable<T> {

    private val gameDataStorage = ConcurrentHashMap<Int, T>()
    override fun iterator(): Iterator<T> = gameDataStorage.values.iterator()

    /** Saves [gameData] to storage and returns it */
    fun register(gameData: T): T {
        gameDataStorage[gameData.id] = gameData
        return gameData
    }

    /** Saves [gameData] to storage and returns it */
    fun register(vararg gameData: T): Iterable<T> {
        gameData.forEach { gameDataStorage[it.id] = it }
        return gameData.asList()
    }

    /**
     * Finds template by its identifier
     */
    fun findById(id: Int) = gameDataStorage[id]
}
