package org.l2kserver.game.repository

import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.actor.GameWorldObject
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import org.l2kserver.game.model.actor.Npc
import org.l2kserver.game.service.VISION_RANGE

/**
 * Storage of game entities in game world
 */
@Component
class GameObjectRepository {

    private val objectMap = ConcurrentHashMap<Int, GameWorldObject>()
    private val charactersMap = ConcurrentHashMap<Int, PlayerCharacter>()
    private val npcMap = ConcurrentHashMap<Int, Npc>()

    /**
     * Loads character to game world
     * @return loaded Character
     */
    fun loadCharacter(character: PlayerCharacter): PlayerCharacter {
        charactersMap[character.id] = character
        return character
    }

    fun <T: GameWorldObject> save(gameObject: T?) = if (gameObject == null) null else {
        when (gameObject) {
            is PlayerCharacter -> charactersMap[gameObject.id] = gameObject
            is Npc -> npcMap[gameObject.id] = gameObject
            else -> objectMap[gameObject.id] = gameObject
        }

        gameObject
    }

    fun findByIdOrNull(id: Int) = charactersMap[id] ?: npcMap[id] ?: objectMap[id]

    fun findById(id: Int) = requireNotNull(charactersMap[id] ?:objectMap[id]) {
        "No GameObject found by id=$id"
    }

    fun findActorByIdOrNull(id: Int) = npcMap[id] ?: charactersMap[id]

    fun findActorById(id: Int) = requireNotNull(npcMap[id] ?: charactersMap[id]) {
        "No actor found by id = '$id'"
    }

    fun findCharacterByIdOrNull(id: Int) = charactersMap[id]

    fun findCharacterById(id: Int) = requireNotNull(charactersMap[id]) {
        "No character found by id=$id"
    }

    fun findCharacterByName(characterName: String) =
        requireNotNull(charactersMap.values.find { it.name == characterName }) {
            "No character found by name '$characterName'"
        }

    fun findAllNear(gameObject: GameWorldObject) =
        sequenceOf(objectMap.values, charactersMap.values, npcMap.values)
            .flatten()
            .filter {
                it.position.isCloseTo(gameObject.position, VISION_RANGE) && it.id != gameObject.id
            }

    /**
     * Finds all characters near GameObject.
     * @return all the characters near provided GameObject except provided GameObject
     */
    fun findAllCharactersNear(gameObject: GameWorldObject, distance: Int = VISION_RANGE) =
        charactersMap.values.filter {
            it.position.isCloseTo(gameObject.position, distance) && it.id != gameObject.id
        }

    /**
     * Finds all characters near given Position.
     * @return all the characters near provided position
     */
    @Suppress("UNCHECKED_CAST")
    fun findAllCharactersNear(position: Position, distance: Int = VISION_RANGE) = charactersMap.values.filter {
        it.position.isCloseTo(position, distance)
    }

    /** Finds all the characters in game */
    fun findAllCharacters() = charactersMap.values.toList()

    /** Finds all the actors in game */
    fun findAllActors() = npcMap.values + charactersMap.values

    /** Finds all the NPCs in game */
    fun findAllNpc() = npcMap.values.toList()

    fun existsById(id: Int) = objectMap.containsKey(id) || charactersMap.containsKey(id) || npcMap.containsKey(id)

    @Suppress("UNCHECKED_CAST")
    fun <T: GameWorldObject> delete(gameObject: T): T? = when (gameObject) {
        is PlayerCharacter -> charactersMap.remove(gameObject.id)
        is Npc -> npcMap.remove(gameObject.id)
        else -> objectMap.remove(gameObject.id)
    } as T?

    fun deleteById(id: Int) =
        charactersMap.remove(id) ?: npcMap.remove(id) ?: objectMap.remove(id)

    fun deleteAll() {
        charactersMap.clear()
        npcMap.clear()
        objectMap.clear()
    }

}
