package org.l2kserver.game.service

import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.extensions.model.actor.asMutable
import org.l2kserver.game.extensions.model.actor.isAttacking
import org.l2kserver.game.extensions.model.actor.toFighting
import org.l2kserver.game.handler.dto.ChatTab
import org.l2kserver.game.handler.dto.response.ChatMessageResponse
import org.l2kserver.game.model.actor.NpcInstanceImpl
import org.l2kserver.game.repository.GameWorld
import org.l2kserver.game.handler.dto.response.DeleteObjectResponse
import org.l2kserver.game.model.actor.CollisionBox
import org.l2kserver.game.model.actor.Intention
import org.l2kserver.game.model.actor.npc.Npc
import org.l2kserver.game.model.actor.npc.NpcRegistry
import org.l2kserver.game.model.actor.npc.NpcState
import org.l2kserver.game.model.actor.npc.SpawnedAt
import org.l2kserver.game.model.actor.npc.ai.AiDesire
import org.l2kserver.game.model.actor.npc.ai.AiEvent
import org.l2kserver.game.model.actor.position.Heading
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.position.SpawnPosition
import org.l2kserver.game.model.zone.SpawnZone
import org.l2kserver.game.model.zone.Zone
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.time.Duration.Companion.milliseconds

private const val CORPSE_DISAPPEARANCE_DELAY_MS = 8_500L
//TODO Raid boss mechanics
// private const val RAID_BOSS_CORPSE_DISAPPEARANCE_DELAY_MS = 30_000L

@Service
class NpcService(
    private val geoDataService: GeoDataService,
    private val asyncTaskService: AsyncTaskService,
    private val intentionExecutorService: IntentionExecutorService,
    private val idGenerationService: IdGenerationService,
    override val gameWorld: GameWorld
): AbstractService() {

    override val log = logger()

    @EventListener(ApplicationStartedEvent::class, condition = "!@environment.acceptsProfiles('test')")
    fun spawnNpcs() = runBlocking {
        NpcRegistry.flatMap { template ->
            val byPos = template.spawn?.positions?.map {
                asyncTaskService.launchOnce { spawnAtPosition(template, it) }
            } ?: emptyList()

            val byZone = template.spawn?.zones?.flatMap { zone ->
                List(zone.npcAmount) { asyncTaskService.launchOnce { spawnAtZone(template, zone) } }
            } ?: emptyList()

            byPos + byZone
        }.joinAll()
    }

    @EventListener(ApplicationReadyEvent::class)
    fun launchAi() = gameWorld.getRegions().forEach { region ->
        asyncTaskService.launchRepeated("${region.xId}_${region.yId}_AI_JOB", iterationsDelay = 1001) {
            region.npcs.asSequence()
                .filter { !it.isDead() }
                .forEach { npc -> npc.ai?.let { ai ->
                    try {
                        val objectEvents = if (ai.perceptionRadius() > 0)
                            scanPlayersAround(npc, ai.perceptionRadius())
                                .first.map { AiEvent.ObjectAppeared(it) }
                        else emptySet()

                        val events = mutableListOf<AiEvent>()

                        while (true) npc.eventsHappened.poll()?.let { events.add(it) } ?: break
                        ai.onTick(events + objectEvents).forEach { performDesiredAction(npc, it) }
                    }
                    catch (e: Throwable) {
                        log.error(e) { "An error occurred when handling $npc's ai" }
                    }
                }}
        }
    }

    /** Handles [npc]'s death - schedules corpse disappearing and respawn */
    suspend fun handleNpcDeath(npc: NpcInstanceImpl) = asyncTaskService.launchOnce {
        //Set npc state to default
        npc.state = NpcState.Idle()

        npc.targetedBy.forEach { it.asMutable().targetId = null }

        //Delete corpse from game world after delay
        delay(CORPSE_DISAPPEARANCE_DELAY_MS.milliseconds)
        remove(npc)

        //Respawn this NPC after delay
        val template = NpcRegistry.findById(npc.templateId)
        delay(template.spawn!!.respawnDelay.milliseconds)

        //Spawn NPC at position or zone, depending on what is present
        npc.spawnedAt.spawnZone?.let { spawnAtZone(template, it) }
            ?: spawnAtPosition(template, npc.spawnedAt.spawnPosition)


        log.debug { "Respawned $npc at ${npc.spawnedAt}" }
    }

    suspend fun remove(npc: NpcInstanceImpl) = gameWorld.delete(npc)?.let {
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
            spawnedAt = SpawnedAt(
                SpawnPosition(position.x, position.y, position.z, heading.toInt()),
                zone
            ),
            position = position,
            heading = heading
        )
        spawnNpc(npc)

        log.debug { "Spawned $npc at $position inside of $zone" }
        return npc
    }

    /** Saved NPC to gameObjectRepository and notified surrounding players about spawn */
    private suspend fun spawnNpc(npc: NpcInstanceImpl) {
        gameWorld.save(npc)
        if (npc.ai != null) intentionExecutorService.launchIntentionQueueListener(npc)
    }

    /** Returns random available position and heading inside provided [zone] */
    private fun getPositionAndHeading(zone: Zone, collisionBox: CollisionBox): Pair<Position, Heading> {
        val position = geoDataService.getRandomSpawnPosition(collisionBox, zone)
        val heading = Heading(Random.nextInt(0..65535))

        return position to heading
    }

    private suspend fun performDesiredAction(npc: NpcInstanceImpl, desire: AiDesire) {
        log.trace { "Started handling $desire of '$npc'" }
        when (desire) {
            is AiDesire.Attack -> {
                val target = desire.target.asMutable()
                npc.state.toFighting(target)

                if (npc.isAttacking(target)) log.debug { "$npc is already attacking $target" }
                else npc.intentionQueue.enqueue(
                    Intention.Move(target, requiredDistance = npc.stats.attackRange),
                    Intention.Attack(desire.target.asMutable())
                )
            }
            is AiDesire.Move -> {
                npc.intentionQueue.enqueue(Intention.Move(desire.position))
            }
            is AiDesire.Say -> broadcastAround(npc.position) {
                ChatMessageResponse(
                    speakerId = npc.id,
                    chatTab = ChatTab.GENERAL,
                    speakerName = npc.name,
                    message = desire.message
                )
            }
        }
    }

}
