package org.l2kserver.game.repository

import org.l2kserver.game.model.actor.PlayerCharacterInstanceImpl
import org.l2kserver.game.model.actor.GameWorldObject
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import org.l2kserver.game.model.actor.NpcInstanceImpl
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.service.GEO_TILE_X_MAX
import org.l2kserver.game.service.GEO_TILE_X_MIN
import org.l2kserver.game.service.GEO_TILE_Y_MAX
import org.l2kserver.game.service.GEO_TILE_Y_MIN
import kotlin.collections.asSequence
import kotlin.collections.set
import kotlin.sequences.plus

class GameWorldRegion(xId: Int, yId: Int) {

    val xId = xId + GEO_TILE_X_MIN
    val yId = yId + GEO_TILE_Y_MIN

    val objects: MutableSet<GameWorldObject> = ConcurrentHashMap.newKeySet()
    val characters: MutableSet<PlayerCharacterInstanceImpl> = ConcurrentHashMap.newKeySet()
    val npcs: MutableSet<NpcInstanceImpl> = ConcurrentHashMap.newKeySet()

    fun <T: GameWorldObject> add(gameObject: T): T {
        when (gameObject) {
            is PlayerCharacterInstanceImpl -> characters.add(gameObject)
            is NpcInstanceImpl -> npcs.add(gameObject)
            else -> objects.add(gameObject)
        }

        return gameObject
    }

    fun remove(gameObject: GameWorldObject): Boolean = when(gameObject) {
        is PlayerCharacterInstanceImpl -> characters.remove(gameObject)
        is NpcInstanceImpl -> npcs.remove(gameObject)
        else -> objects.remove(gameObject)
    }

    fun deleteAll() = objects.clear()

    fun asSequence(): Sequence<GameWorldObject> = objects.asSequence() + characters + npcs
}

/** Storage of game entities in game world */
@Component
class GameWorld {

    private val objects = ConcurrentHashMap<Int, GameWorldObject>()
    private val characters = ConcurrentHashMap<Int, PlayerCharacterInstanceImpl>()
    private val npcs = ConcurrentHashMap<Int, NpcInstanceImpl>()

    private val objectsByRegions: Array<Array<GameWorldRegion>> =
        Array(GEO_TILE_Y_MAX - GEO_TILE_Y_MIN + 1) { yId ->
            Array(GEO_TILE_X_MAX - GEO_TILE_X_MIN + 1) { xId -> GameWorldRegion(xId, yId) }
        }

    fun <T: GameWorldObject> save(gameObject: T): T {
        when (gameObject) {
            is PlayerCharacterInstanceImpl -> characters[gameObject.id] = gameObject
            is NpcInstanceImpl -> npcs[gameObject.id] = gameObject
            else -> objects[gameObject.id] = gameObject
        }

        val regionY = gameObject.position.regionYId() - GEO_TILE_Y_MIN
        val regionX = gameObject.position.regionXId() - GEO_TILE_X_MIN
        objectsByRegions[regionY][regionX].add(gameObject)

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

    fun findAllNear(
        gameObject: GameWorldObject,
        distance: Int
    ) = getRegionsNear(gameObject.position, distance).asSequence()
        .flatMap { it.asSequence() }
        .filter { it.id != gameObject.id }
        .filter { it.position.isCloseTo(gameObject.position, distance) }

    /**
     * Finds all the actors near provided [gameObject].
     * @return all the characters near provided GameObject except provided GameObject
     */
    fun findAllActorsNear(
        gameObject: GameWorldObject, distance: Int
    ) = getRegionsNear(gameObject.position, distance).asSequence()
        .flatMap { it.characters.asSequence() + it.npcs.asSequence() }
        .filter { it.id != gameObject.id }
        .filter { it.position.isCloseTo(gameObject.position, distance) }

    /**
     * Finds all the characters near provided [gameObject].
     * @return all the characters near provided GameObject except provided GameObject
     */
    fun findAllCharactersNear(
        gameObject: GameWorldObject, distance: Int
    ) = getRegionsNear(gameObject.position, distance)
        .asSequence()
        .flatMap { it.characters.asSequence() }
        .filter { it.id != gameObject.id }
        .filter { it.position.isCloseTo(gameObject.position, distance) }

    /**
     * Finds all characters near given Position.
     * @return all the characters near provided position
     */
    fun findAllCharactersNear(
        position: Position, distance: Int
    ) = getRegionsNear(position, distance)
        .asSequence()
        .flatMap { it.characters.asSequence() }
        .filter { it.position.isCloseTo(position, distance) }

    /** Finds all the characters in game */
    fun findAllCharacters() = characters.values.asSequence()

    /** Finds all the actors in game */
    fun findAllActors() = sequenceOf(npcs.values + characters.values).flatten()

    fun existsById(id: Int) = objects.containsKey(id) || characters.containsKey(id) || npcs.containsKey(id)

    /**
     * Removes gameObject.
     *
     * @return removed worldObject, or `null` if there was no such worldObject
     */
    @Suppress("UNCHECKED_CAST")
    fun <T: GameWorldObject> delete(gameObject: T): T? {
        val deletedObject = when (gameObject) {
            is PlayerCharacterInstanceImpl -> characters.remove(gameObject.id)
            is NpcInstanceImpl -> npcs.remove(gameObject.id)
            else -> objects.remove(gameObject.id)
        } as T?

        val deletedFromRegions = objectsByRegions
            .flatMap { objects -> objects.map { it.remove(gameObject) } }
            .reduce { acc, bool -> acc || bool }

        assert(deletedFromRegions) { "GAME OBJECT WAS DELETED BY ID, BUT WAS NOT DELETED FROM REGION!!!" }

        return deletedObject
    }

    fun deleteAll() = synchronized(this) {
        characters.clear()
        npcs.clear()
        objects.clear()

        objectsByRegions.forEach { it.forEach { region -> region.deleteAll() }}
    }

    /** @return [Sequence] of game world regions */
    fun getRegions() = objectsByRegions.asSequence().flatMap { it.asSequence() }

    /** Moves [gameObject] from old region (by [oldPosition]) to the new one (by current position of [gameObject]) */
    fun <T : GameWorldObject> updateRegion(gameObject: T, oldPosition: Position) {
        val oldRegionY = oldPosition.regionYId() - GEO_TILE_Y_MIN
        val oldRegionX = oldPosition.regionXId() - GEO_TILE_X_MIN

        val newRegionY = gameObject.position.regionYId() - GEO_TILE_Y_MIN
        val newRegionX = gameObject.position.regionXId() - GEO_TILE_X_MIN

        if (oldRegionX == newRegionX && oldRegionY == newRegionY) return

        val oldRegion = objectsByRegions.getOrNull(oldRegionY)?.getOrNull(oldRegionX)
        val newRegion = objectsByRegions.getOrNull(newRegionY)?.getOrNull(newRegionX)

        require (newRegion != null) {
            "Object ${gameObject.id} moved out of world bounds: ${gameObject.position}"
        }

        oldRegion?.remove(gameObject)
        newRegion.add(gameObject)
    }

    fun getRegionsNear(position: Position, distance: Int): List<GameWorldRegion> {
        val regions = ArrayList<GameWorldRegion>(3)

        val regionY = position.regionYId() - GEO_TILE_Y_MIN
        val regionX = position.regionXId() - GEO_TILE_X_MIN

        // If position is close to region border, check also nearby regions
        val xMin = if (position.inRegionX() < distance) regionX - 1 else regionX
        val xMax = if (position.inRegionX() > Position.GEO_REGION_SIZE - distance) regionX + 1 else regionX
        val yMin = if (position.inRegionY() < distance) regionY - 1 else regionY
        val yMax = if (position.inRegionY() > Position.GEO_REGION_SIZE - distance) regionY + 1 else regionY

        for (y: Int in yMin..yMax) {
            for (x: Int in xMin..xMax) {
                objectsByRegions.getOrNull(y)?.getOrNull(x)?.let { regions.add(it) }
            }
        }

        return regions
    }
}
