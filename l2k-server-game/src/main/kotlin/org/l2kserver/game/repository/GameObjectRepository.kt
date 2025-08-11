package org.l2kserver.game.repository

import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.actor.GameWorldObject
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import org.l2kserver.game.model.actor.Actor
import org.l2kserver.game.service.VISION_RANGE

/**
 * Storage of game entities in game world
 */
@Component
class GameObjectRepository: Iterable<GameWorldObject> {

    private val objectMap = ConcurrentHashMap<Int, GameWorldObject>()

    override fun iterator() = objectMap.values.iterator()

    /**
     * Loads character from database and places it to game world
     *
     * @throws IllegalArgumentException if no character with given id exists
     * @return loaded Character
     */
    fun loadCharacter(character: PlayerCharacter): PlayerCharacter {
        objectMap[character.id] = character
        return character
    }

    fun <T: GameWorldObject> save(gameObject: T?) = if (gameObject == null) null else {
        objectMap[gameObject.id] = gameObject
        gameObject
    }

    fun findByIdOrNull(id: Int) = objectMap[id]

    fun findById(id: Int) = requireNotNull(objectMap[id]) { "No GameObject found by id=$id" }

    fun findActorByIdOrNull(id: Int) = objectMap[id] as? Actor

    fun findActorById(id: Int) = requireNotNull(objectMap[id] as? Actor) {
        "No actor found by id = '$id'"
    }

    fun findCharacterByIdOrNull(id: Int) = objectMap[id] as? PlayerCharacter

    fun findCharacterById(id: Int) = requireNotNull(findCharacterByIdOrNull(id)) {
        "No character found by id=$id"
    }

    fun findCharacterByName(characterName: String) =
        requireNotNull(objectMap.values.find { it is PlayerCharacter && it.name == characterName } as PlayerCharacter) {
            "No character found by name '$characterName'"
        }

    fun findAllNear(gameObject: GameWorldObject) = objectMap.values.filter {
        it.position.isCloseTo(gameObject.position, VISION_RANGE) && it.id != gameObject.id
    }

    /**
     * Finds all characters near GameObject.
     * @return all the characters near provided GameObject except provided GameObject
     */
    fun findAllCharactersNear(gameObject: GameWorldObject, distance: Int = VISION_RANGE) = objectMap.values.filter {
        it.position.isCloseTo(gameObject.position, distance) && it.id != gameObject.id && it is PlayerCharacter
    }

    /**
     * Finds all characters near given Position.
     * @return all the characters near provided position
     */
    @Suppress("UNCHECKED_CAST")
    fun findAllCharactersNear(position: Position, distance: Int = VISION_RANGE) = objectMap.values.filter {
        it.position.isCloseTo(position, distance) && it is PlayerCharacter
    } as List<PlayerCharacter>

    /**
     * Finds all characters in game
     */
    fun findAllCharacters() = objectMap.values.filterIsInstance<PlayerCharacter>()

    fun existsById(id: Int) = objectMap.containsKey(id)

    @Suppress("UNCHECKED_CAST")
    fun <T: GameWorldObject> delete(gameObject: T) = objectMap.remove(gameObject.id) as T?

    fun deleteById(id: Int) = objectMap.remove(id)

    fun deleteAll() = objectMap.clear()

}
