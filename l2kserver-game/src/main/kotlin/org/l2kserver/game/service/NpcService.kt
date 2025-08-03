package org.l2kserver.game.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.extensions.model.actor.toNpc
import org.l2kserver.game.handler.dto.response.NpcChatWindowResponse
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.Npc
import org.l2kserver.game.repository.GameObjectRepository
import org.l2kserver.game.model.actor.npc.NpcTemplate
import org.l2kserver.game.extensions.model.actor.toInfoResponse
import org.l2kserver.game.handler.dto.response.DeleteObjectResponse
import org.l2kserver.game.model.actor.npc.SpawnedAt
import org.l2kserver.game.model.actor.position.Heading
import org.l2kserver.game.model.actor.position.SpawnPosition
import org.l2kserver.game.model.actor.position.toPositionAndHeading
import org.l2kserver.game.model.zone.SpawnZone
import org.l2kserver.game.network.session.send
import org.l2kserver.game.utils.IdUtils
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import kotlin.random.Random
import kotlin.random.nextInt

private const val DEFAULT_INIT_REPLICA = "<html><body>My text is missing!</body></html>"

private const val CORPSE_DISAPPEARANCE_DELAY_MS = 8_500L
//TODO Raid boss mechanics
// private const val RAID_BOSS_CORPSE_DISAPPEARANCE_DELAY_MS = 30_000L

@Service
class NpcService(
    private val geoDataService: GeoDataService,
    private val asyncTaskService: AsyncTaskService,
    override val gameObjectRepository: GameObjectRepository
): AbstractService() {

    override val log = logger()

    @EventListener(ApplicationReadyEvent::class)
    fun init() = asyncTaskService.launchJob("INITIAL_SPAWN_TASK") {
        NpcTemplate.Registry.forEach { template ->
            template.spawn.positions?.forEach { spawnAtPosition(template, it) }
            template.spawn.zones?.forEach { zone -> repeat(zone.npcAmount) { spawnAtZone(template, zone) }}
        }
    }

    /**
     * Opens chat window with [npc]
     */
    suspend fun talkTo(npc: Npc) {
        send(NpcChatWindowResponse(
            npcId = npc.id,
            message = npc.replicas.firstOrNull() ?: DEFAULT_INIT_REPLICA
        ))
    }

    /**
     * Handles [npc]'s death - schedules corpse disappearing and respawn
     */
    suspend fun handleNpcDeath(npc: Npc) = CoroutineScope(Dispatchers.Default).launch {
        //Delete corpse from game world after delay
        delay(CORPSE_DISAPPEARANCE_DELAY_MS)
        broadcastPacket(DeleteObjectResponse(npc.id), npc)
        gameObjectRepository.delete(npc)

        //Respawn this NPC after delay
        val template = NpcTemplate.Registry.findById(npc.templateId)!!
        delay(template.spawn.respawnDelay)

        //Spawn NPC at position or zone, depending on what is present
        npc.spawnedAt.spawnPosition?.let { spawnAtPosition(template, it) }
        npc.spawnedAt.spawnZone?.let { spawnAtZone(template, it) }
    }

    /**
     * Spawns npc by [template] at requested [spawnPosition]
     *
     * @return Spawned NPC
     */
    suspend fun spawnAtPosition(template: NpcTemplate, spawnPosition: SpawnPosition): Npc {
        val (position, heading) = spawnPosition.toPositionAndHeading()
        val npc = template.toNpc(
            IdUtils.getNextNpcId(),
            position,
            heading,
            SpawnedAt(spawnPosition)
        )

        spawnNpc(npc)

        log.info("Spawned {} at {}", npc, position)
        return npc
    }

    /**
     * Spawns npc by [template] at requested [zone].
     * Npc will be spawned at random free position inside the zone with random heading direction
     *
     * @return Spawned NPC
     */
    suspend fun spawnAtZone(template: NpcTemplate, zone: SpawnZone): Npc {
        lateinit var position: Position
        do {
            position = geoDataService.getRandomSpawnPosition(template.collisionBox, zone)

            val positionIsFree = gameObjectRepository.none {
                it.position.isCloseTo(
                    position,
                    (template.collisionBox.radius + it.collisionBox.radius).toInt()
                )
            }
        } while (!positionIsFree)

        val npc = template.toNpc(
            IdUtils.getNextNpcId(),
            position,
            Heading(Random.nextInt(0..65535)),
            SpawnedAt(zone)
        )
        spawnNpc(npc)

        log.info("Spawned {} at {} inside of {}", npc, position, zone)
        return npc
    }

    /**
     * Saved NPC to gameObjectRepository and notified surrounding players about spawn
     */
    private suspend fun spawnNpc(npc: Npc) {
        gameObjectRepository.save(npc)
        broadcastPacket(npc.toInfoResponse(), npc)
    }

}
