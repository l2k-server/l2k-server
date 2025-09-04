package org.l2kserver.game.service

import java.lang.System.currentTimeMillis
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.l2kserver.game.model.extensions.forEachInstance
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.handler.dto.response.ChangeMoveTypeResponse
import org.l2kserver.game.handler.dto.response.PvPStatusResponse
import org.l2kserver.game.handler.dto.response.StartFightingResponse
import org.l2kserver.game.handler.dto.response.StatusAttribute
import org.l2kserver.game.handler.dto.response.StopFightingResponse
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.Npc
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.actor.MoveType
import org.l2kserver.game.model.actor.MutableActorInstance
import org.l2kserver.game.model.actor.Posture
import org.l2kserver.game.model.actor.character.PvpState
import org.l2kserver.game.repository.GameObjectRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import kotlin.math.roundToInt

private const val COMBAT_TIME_MS = 15_000

const val ACTOR_STATE_JOB = "ACTOR_STATE_JOB"
const val REGENERATION_JOB = "REGENERATION_JOB"

private const val REGENERATION_TASK_DELAY_MS = 3_000L // 5 minutes for doors
private const val REGENERATION_MULTIPLIER_ON_SITTING = 1.5
private const val REGENERATION_MULTIPLIER_ON_STAYING = 1.1
private const val REGENERATION_MULTIPLIER_ON_RUNNING = 0.7

@Service
class ActorStateService(
    private val asyncTaskService: AsyncTaskService,
    override val gameObjectRepository: GameObjectRepository,

    @Value("\${pvp.pvpFlagTimeMs}")
    private val pvpFlagTimeMs: Int,

    @Value("\${pvp.pvpFlagEndingTimeMs}")
    private val pvpFlagEndingTimeMs: Int
) : AbstractService() {
    override val log = logger()

    /**
     * Key - actor, value - time when actor must stop standing in combat stance
     */
    private val fightingActors = ConcurrentHashMap<MutableActorInstance, Long>()

    /**
     * Key - character, value - time when character's PVP state ends
     */
    private val charactersInPvpState = ConcurrentHashMap<PlayerCharacter, Long>()

    @EventListener(ApplicationReadyEvent::class)
    fun init() {
        asyncTaskService.launchTask(ACTOR_STATE_JOB) {
            while (isActive) {
                try {
                    updateActorsFightingState()
                    updateCharactersPvpState()
                } catch (e: Throwable) {
                    log.error("An error occurred while updating actor states, ", e)
                }

                delay(1_000)
            }
        }

        asyncTaskService.launchTask(REGENERATION_JOB) {
            while (isActive) {
                regenerate()
                delay(REGENERATION_TASK_DELAY_MS)
            }
        }
    }

    /**
     * If player was not in PVP state - sets this actor pvp state to PVP,
     * broadcasts to all surrounding characters that actor is in PVP,
     * else - updates this actor combat time
     */
    suspend fun activatePvpState(character: PlayerCharacter) {
        log.debug("Enabling (or updating) PVP state of '{}'", character)
        charactersInPvpState[character] = currentTimeMillis() + pvpFlagTimeMs

        if (character.pvpState != PvpState.PVP) {
            character.pvpState = PvpState.PVP
            broadcastPacket(PvPStatusResponse(character), character.position)
        }
    }

    /**
     * If actor was not in combat stance - sets this actor isFighting to true,
     * broadcasts to all surrounding characters that actor is fighting,
     * else - updates this actor combat time
     */
    suspend fun activateCombatState(actor: MutableActorInstance) {
        log.debug("Enabling (or updating) combat state of '{}'", actor)
        if (!actor.isFighting) {
            actor.isFighting = true

            //TODO This is part of AI, not combat service
            if (actor is Npc) {
                actor.moveType = MoveType.RUN
                broadcastPacket(ChangeMoveTypeResponse(actor.id, actor.moveType))
            }

            broadcastPacket(StartFightingResponse(actor.id), actor.position)
        }

        fightingActors[actor] = currentTimeMillis() + COMBAT_TIME_MS
    }

    /**
     * Disables this actor's combat state.
     * Notifies surrounding characters about this and flushes combatState end time
     */
    suspend fun disableCombatState(actor: MutableActorInstance) {
        broadcastPacket(StopFightingResponse(actor.id), actor.position)
        actor.isFighting = false
        fightingActors.remove(actor)
    }

    /**
     * Stop updating actor's states, for example, if he dies or exits game
     */
    fun stopUpdatingStates(actor: ActorInstance) {
        fightingActors.remove(actor)
        charactersInPvpState.remove(actor)
        log.debug("Stopped updating state of '{}'", actor)
    }

    private suspend fun updateActorsFightingState() = fightingActors.forEach { (actor, inCombatEndTimeMs) ->
        if (inCombatEndTimeMs <= currentTimeMillis()) {
            //TODO This is part of AI, not combat service
            if (actor is Npc) {
                actor.moveType = MoveType.WALK
                broadcastPacket(ChangeMoveTypeResponse(actor.id, actor.moveType))
            }

            disableCombatState(actor)
        }
    }

    private suspend fun updateCharactersPvpState() = charactersInPvpState.forEach { (character, pvpStateEndsTime) ->
        val pvpTimeLeft = pvpStateEndsTime - currentTimeMillis()
        when {
            pvpTimeLeft <= 0 -> newSuspendedTransaction {
                character.pvpState = PvpState.NOT_IN_PVP
                broadcastPacket(PvPStatusResponse(character), character.position)
                charactersInPvpState.remove(character)
                log.debug("'{}' is now not in PVP", character)
            }

            pvpTimeLeft <= pvpFlagEndingTimeMs -> newSuspendedTransaction {
                character.pvpState = PvpState.PVP_ENDING
                broadcastPacket(PvPStatusResponse(character), character.position)
                log.debug("Switched PVP state of '{}' to '{}'", character, character.pvpState)
            }
        }
    }

    private suspend fun regenerate() = gameObjectRepository.forEachInstance<MutableActorInstance> { actor ->
        newSuspendedTransaction {
            if (actor.isDead()) return@newSuspendedTransaction

            val updatedStatuses = mutableMapOf<StatusAttribute, Int>()

            val postureBonus = when {
                actor is PlayerCharacter && actor.posture == Posture.SITTING -> REGENERATION_MULTIPLIER_ON_SITTING
                !actor.isMoving -> REGENERATION_MULTIPLIER_ON_STAYING
                actor.isRunning -> REGENERATION_MULTIPLIER_ON_RUNNING
                else -> 1.0
            }

            // Regenerate HP
            if (actor.stats.maxHp > actor.currentHp) {
                val hpRegeneration = actor.stats.hpRegen * postureBonus //TODO apply buffs and zones
                actor.currentHp = minOf(actor.stats.maxHp, actor.currentHp + hpRegeneration.roundToInt())

                //Both hp and mp must be sent, otherwise client does not update status
                updatedStatuses[StatusAttribute.CUR_HP] = actor.currentHp
                updatedStatuses[StatusAttribute.CUR_MP] = actor.currentMp
            }

            // Regenerate MP
            if (actor.stats.maxMp > actor.currentMp) {
                val mpRegeneration = actor.stats.mpRegen * postureBonus //TODO apply buffs and zones
                actor.currentMp = minOf(actor.stats.maxMp, actor.currentMp + mpRegeneration.roundToInt())

                //Both hp and mp must be sent, otherwise client does not update status
                updatedStatuses[StatusAttribute.CUR_HP] = actor.currentHp
                updatedStatuses[StatusAttribute.CUR_MP] = actor.currentMp
            }

            // Regenerate CP
            if (actor is PlayerCharacter && actor.stats.maxCp > actor.currentCp) {
                val cpRegeneration = actor.stats.cpRegen * postureBonus //TODO apply buffs and zones
                actor.currentCp = minOf(actor.stats.maxCp, actor.currentCp + cpRegeneration.roundToInt())

                updatedStatuses[StatusAttribute.CUR_CP] = actor.currentCp
            }

            if (updatedStatuses.isNotEmpty()) broadcastPacket(UpdateStatusResponse(actor.id, updatedStatuses))
        }
    }

}
