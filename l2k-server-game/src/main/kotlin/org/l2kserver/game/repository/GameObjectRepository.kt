package org.l2kserver.game.repository

import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.PlayerCharacterInstanceImpl
import org.l2kserver.game.model.actor.GameWorldObject
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import org.l2kserver.game.model.actor.NpcInstanceImpl
import org.l2kserver.game.service.VISION_RANGE
import kotlin.collections.asSequence

/** Storage of game entities in game world */
@Component
class GameObjectRepository {

    private val objects = ConcurrentHashMap<Int, GameWorldObject>()
    private val characters = ConcurrentHashMap<Int, PlayerCharacterInstanceImpl>()
    private val npcs = ConcurrentHashMap<Int, NpcInstanceImpl>()

    fun <T: GameWorldObject> save(gameObject: T): T {
        when (gameObject) {
            is PlayerCharacterInstanceImpl -> characters[gameObject.id] = gameObject
            is NpcInstanceImpl -> npcs[gameObject.id] = gameObject
            else -> objects[gameObject.id] = gameObject
        }

        return gameObject
    }

    fun findByIdOrNull(id: Int) = characters[id] ?: npcs[id] ?: objects[id]

    fun findById(id: Int) = requireNotNull(characters[id] ?:objects[id]) {
        "No GameObject found by id=$id"
    }

    fun findActorByIdOrNull(id: Int) = npcs[id] ?: characters[id]

    fun findActorById(id: Int) = requireNotNull(npcs[id] ?: characters[id]) {
        "No actor found by id = '$id'"
    }

    fun findCharacterByIdOrNull(id: Int) = characters[id]

    fun findCharacterById(id: Int) = requireNotNull(characters[id]) {
        "No character found by id=$id"
    }

    fun findCharacterByName(characterName: String) =
        requireNotNull(characters.values.find { it.name == characterName }) {
            "No character found by name '$characterName'"
        }

    fun findAllNear(gameObject: GameWorldObject) =
        sequenceOf(objects.values, characters.values, npcs.values)
            .flatten()
            .filter {
                it.position.isCloseTo(gameObject.position, VISION_RANGE) && it.id != gameObject.id
            }

    /**
     * Finds all the actors near provided [gameObject].
     * @return all the characters near provided GameObject except provided GameObject
     */
    fun findAllActorsNear(
        gameObject: GameWorldObject, distance: Int = VISION_RANGE
    ) = sequenceOf(characters.values, npcs.values)
        .flatten()
        .filter { it.position.isCloseTo(gameObject.position, distance) && it.id != gameObject.id }

    /**
     * Finds all the characters near provided [gameObject].
     * @return all the characters near provided GameObject except provided GameObject
     */
    fun findAllCharactersNear(
        gameObject: GameWorldObject, distance: Int = VISION_RANGE
    ) = characters.values.asSequence().filter {
        it.position.isCloseTo(gameObject.position, distance) && it.id != gameObject.id
    }

    /**
     * Finds all characters near given Position.
     * @return all the characters near provided position
     */
    fun findAllCharactersNear(
        position: Position, distance: Int = VISION_RANGE
    ) = characters.values.asSequence().filter {
        it.position.isCloseTo(position, distance)
    }

    /** Finds all the characters in game */
    fun findAllCharacters() = characters.values.asSequence()

    /** Finds all the actors in game */
    fun findAllActors() = sequenceOf(npcs.values + characters.values).flatten()

    /** Finds all the NPCs in game */
    fun findAllNpc() = npcs.values.asSequence()

    fun existsById(id: Int) = objects.containsKey(id) || characters.containsKey(id) || npcs.containsKey(id)

    /**
     * Removes gameObject.
     *
     * @return removed object, or `null` if there was no such object
     */
    @Suppress("UNCHECKED_CAST")
    fun <T: GameWorldObject> delete(gameObject: T): T? = when (gameObject) {
        is PlayerCharacterInstanceImpl -> characters.remove(gameObject.id)
        is NpcInstanceImpl -> npcs.remove(gameObject.id)
        else -> objects.remove(gameObject.id)
    } as T?

    fun deleteById(id: Int) =
        characters.remove(id) ?: npcs.remove(id) ?: objects.remove(id)

    fun deleteAll() {
        characters.clear()
        npcs.clear()
        objects.clear()
    }

}
