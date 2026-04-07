package org.l2kserver.game.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.handler.dto.response.NpcChatWindowResponse
import org.l2kserver.game.model.actor.NpcInstanceImpl
import org.l2kserver.game.repository.GameObjectRepository
import org.l2kserver.game.handler.dto.response.DeleteObjectResponse
import org.l2kserver.game.model.actor.npc.Npc
import org.l2kserver.game.model.actor.npc.NpcRegistry
import org.l2kserver.game.model.actor.npc.SpawnedAt
import org.l2kserver.game.model.actor.position.Heading
import org.l2kserver.game.model.actor.position.SpawnPosition
import org.l2kserver.game.model.zone.SpawnZone
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sessionContext
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
    override val gameObjectRepository: GameObjectRepository
): AbstractService() {

    override val log = logger()

    @EventListener(ApplicationReadyEvent::class)
    fun init() = asyncTaskService.launchOnce {
        NpcRegistry.forEach { template ->
            template.spawn?.positions?.forEach { spawnAtPosition(template, it) }
            template.spawn?.zones?.forEach { zone -> repeat(zone.npcAmount) { spawnAtZone(template, zone) }}
        }
    }

    /** Opens chat window with [npc] */
    suspend fun talkTo(npc: NpcInstanceImpl) = send {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        val replica = npc.onTalkWith(character) ?: getNoTextMessage(npc.id, npc.name)

        NpcChatWindowResponse(npcId = npc.id, message = replica )
    }

    /** Handles [npc]'s death - schedules corpse disappearing and respawn */
    suspend fun handleNpcDeath(npc: NpcInstanceImpl) {
        CoroutineScope(Dispatchers.Default).launch {
            //Delete corpse from game world after delay
            delay(CORPSE_DISAPPEARANCE_DELAY_MS)
            remove(npc)
        }
        CoroutineScope(Dispatchers.Default).launch {
            //Respawn this NPC after delay
            val template = NpcRegistry.findByIdOrNull(npc.templateId)!!

            template.spawn?.let { spawn ->
                delay(spawn.respawnDelay)

                //Spawn NPC at position or zone, depending on what is present
                npc.spawnedAt.spawnPosition?.let { spawnAtPosition(template, it) }
                npc.spawnedAt.spawnZone?.let { spawnAtZone(template, it) }
            }
        }
    }

    suspend fun remove(npc: NpcInstanceImpl) = gameObjectRepository.delete(npc)?.let {
        broadcastAround(it) { DeleteObjectResponse(it.id) }
    }

    /**
     * Spawns npc by [template] at requested [spawnPosition]
     *
     * @return Spawned NPC
     */
    suspend fun spawnAtPosition(template: Npc, spawnPosition: SpawnPosition): NpcInstanceImpl {
        val (position, heading) = spawnPosition.toPositionAndHeading()
        val npc = NpcInstanceImpl(template, SpawnedAt(spawnPosition), position, heading)
        spawnNpc(npc)

        log.debug("Spawned {} at {}", npc, position)
        return npc
    }

    /**
     * Spawns npc by [template] at requested [zone].
     * Npc will be spawned at random free position inside the zone with random heading direction
     *
     * @return Spawned NPC
     */
    suspend fun spawnAtZone(template: Npc, zone: SpawnZone): NpcInstanceImpl {
        val position = geoDataService.getRandomSpawnPosition(template.collisionBox, zone)
        val heading = Heading(Random.nextInt(0..65535))

        val npc = NpcInstanceImpl(template, SpawnedAt(zone), position, heading)
        spawnNpc(npc)

        log.debug("Spawned {} at {} inside of {}", npc, position, zone)
        return npc
    }

    /** Saved NPC to gameObjectRepository and notified surrounding players about spawn */
    private suspend fun spawnNpc(npc: NpcInstanceImpl) {
        gameObjectRepository.save(npc)
        updateObjectsAround(npc)
    }


    private fun getNoTextMessage(id: Int, name: String = "") =
        "<html><body>${name}_${id}:<br/> My text is missing!</body></html>"

}
