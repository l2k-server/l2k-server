package org.l2kserver.game.service

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.extensions.model.actor.asMutable
import org.l2kserver.game.model.actor.NpcInstanceImpl
import org.l2kserver.game.repository.GameObjectRepository
import org.l2kserver.game.handler.dto.response.DeleteObjectResponse
import org.l2kserver.game.model.actor.CollisionBox
import org.l2kserver.game.model.actor.npc.Npc
import org.l2kserver.game.model.actor.npc.NpcRegistry
import org.l2kserver.game.model.actor.npc.NpcState
import org.l2kserver.game.model.actor.npc.SpawnedAt
import org.l2kserver.game.model.actor.position.Heading
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.position.SpawnPosition
import org.l2kserver.game.model.zone.SpawnZone
import org.l2kserver.game.model.zone.Zone
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import kotlin.random.Random
import kotlin.random.nextInt

private const val CORPSE_DISAPPEARANCE_DELAY_MS = 8_500L
//TODO Raid boss mechanics
// private const val RAID_BOSS_CORPSE_DISAPPEARANCE_DELAY_MS = 30_000L

@Service
class NpcService(
    private val geoDataService: GeoDataService,
    private val asyncTaskService: AsyncTaskService,
    private val aiService: AiService,
    private val intentionExecutorService: IntentionExecutorService,
    private val idGenerationService: IdGenerationService,
    override val gameObjectRepository: GameObjectRepository
): AbstractService() {

    override val log = logger()

    @EventListener(ApplicationReadyEvent::class)
    fun init() = runBlocking {
        NpcRegistry.forEach { template ->
            template.spawn?.positions?.forEach { spawnAtPosition(template, it) }
            template.spawn?.zones?.forEach { zone -> repeat(zone.npcAmount) { spawnAtZone(template, zone) }}
        }
    }

    /** Handles [npc]'s death - schedules corpse disappearing and respawn */
    suspend fun handleNpcDeath(npc: NpcInstanceImpl) = asyncTaskService.launchOnce {
        //Set npc state to default
        npc.state = NpcState.Idle()

        npc.targetedBy.forEach { it.asMutable().targetId = null }

        //Delete corpse from game world after delay
        delay(CORPSE_DISAPPEARANCE_DELAY_MS)
        remove(npc)

        //Respawn this NPC after delay
        val template = NpcRegistry.findById(npc.templateId)
        delay(template.spawn!!.respawnDelay)

        //Spawn NPC at position or zone, depending on what is present
        npc.spawnedAt.spawnPosition?.let { spawnAtPosition(template, it) }
        npc.spawnedAt.spawnZone?.let { spawnAtZone(template, it) }

        log.debug { "Respawned $npc at ${npc.spawnedAt.spawnPosition?: npc.spawnedAt.spawnZone}" }
    }

    suspend fun remove(npc: NpcInstanceImpl) = gameObjectRepository.delete(npc)?.let {
        gameObjectRepository.findAllCharactersNear(npc).forEach { character ->
            character.knownGameWorldObjects.remove(npc)
        }
        intentionExecutorService.disableIntentionQueueListener(npc.id)
        broadcastAround(it) { DeleteObjectResponse(it.id) }
    }

    /**
     * Spawns npc by [template] at requested [spawnPosition]
     *
     * @return Spawned NPC
     */
    suspend fun spawnAtPosition(template: Npc, spawnPosition: SpawnPosition): NpcInstanceImpl {
        val (position, heading) = spawnPosition.toPositionAndHeading()
        val npc = NpcInstanceImpl(
            id = idGenerationService.next(),
            template = template,
            spawnedAt = SpawnedAt(spawnPosition),
            position = position,
            heading = heading
        )
        spawnNpc(npc)

        log.debug { "Spawned $npc at $position" }
        return npc
    }

    /**
     * Spawns npc by [template] at requested [zone].
     * Npc will be spawned at random free position inside the zone with random heading direction
     *
     * @return Spawned NPC
     */
    suspend fun spawnAtZone(template: Npc, zone: SpawnZone): NpcInstanceImpl {
        val (position, heading) = getPositionAndHeading(zone, template.collisionBox)
        val npc = NpcInstanceImpl(
            id = idGenerationService.next(),
            template = template,
            spawnedAt = SpawnedAt(zone),
            position = position,
            heading = heading
        )
        spawnNpc(npc)

        log.debug { "Spawned $npc at $position inside of $zone" }
        return npc
    }

    /** Saved NPC to gameObjectRepository and notified surrounding players about spawn */
    private suspend fun spawnNpc(npc: NpcInstanceImpl) {
        gameObjectRepository.save(npc)
        updateObjectsAround(npc)

        if (npc.ai != null) intentionExecutorService.launchIntentionQueueListener(npc)
    }

    /** Returns random available position and heading inside provided [zone] */
    private fun getPositionAndHeading(zone: Zone, collisionBox: CollisionBox): Pair<Position, Heading> {
        val position = geoDataService.getRandomSpawnPosition(collisionBox, zone)
        val heading = Heading(Random.nextInt(0..65535))

        return position to heading
    }

}
