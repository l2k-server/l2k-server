package org.l2kserver.game.service

import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.lang.System.currentTimeMillis
import java.util.concurrent.ConcurrentHashMap
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.handler.dto.response.TemporalEffectsResponse
import org.l2kserver.game.handler.dto.response.ChangeMoveTypeResponse
import org.l2kserver.game.handler.dto.response.FullCharacterResponse
import org.l2kserver.game.handler.dto.response.PvPStatusResponse
import org.l2kserver.game.handler.dto.response.StartFightingResponse
import org.l2kserver.game.handler.dto.response.StatusAttribute
import org.l2kserver.game.handler.dto.response.StopFightingResponse
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.NpcInstanceImpl
import org.l2kserver.game.model.actor.PlayerCharacterInstanceImpl
import org.l2kserver.game.model.actor.MoveType
import org.l2kserver.game.model.actor.MutableActorInstance
import org.l2kserver.game.model.actor.character.PlayerCharacterInstance
import org.l2kserver.game.model.actor.character.PvpState
import org.l2kserver.game.network.session.sendTo
import org.l2kserver.game.repository.GameObjectRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.time.Instant
import kotlin.math.roundToInt

private const val COMBAT_TIME_MS = 15_000

private const val ACTOR_STATE_JOB = "ACTOR_STATE_JOB"
private const val REGENERATION_JOB = "REGENERATION_JOB"
private const val UPDATE_ABNORMALS_JOB = "UPDATE_ABNORMALS_JOB"

private const val UPDATE_COMBAT_STATE_DELAY_MS = 1000L
private const val REGENERATION_TASK_DELAY_MS = 3000L //TODO 5 minutes for doors
private const val UPDATE_ABNORMALS_DELAY_MS = 1000L

@Service
class ActorStateService(
    private val asyncTaskService: AsyncTaskService,
    override val gameObjectRepository: GameObjectRepository,

    @param:Value($$"${pvp.pvpFlagTimeMs}") private val pvpFlagTimeMs: Int,
    @param:Value($$"${pvp.pvpFlagEndingTimeMs}") private val pvpFlagEndingTimeMs: Int
) : AbstractService() {
    override val log = logger()

    /**
     * Key - actor, value - time when actor must stop standing in combat stance
     */
    private val fightingActors = ConcurrentHashMap<MutableActorInstance, Long>()

    /**
     * Key - character, value - time when character's PVP state ends
     */
    private val charactersInPvpState = ConcurrentHashMap<PlayerCharacterInstance, Long>()

    @EventListener(ApplicationReadyEvent::class)
    fun init() {
        asyncTaskService.launchRepeated(ACTOR_STATE_JOB, UPDATE_COMBAT_STATE_DELAY_MS) {
            updateActorsFightingState()
            updateCharactersPvpState()
        }

       asyncTaskService.launchRepeated(REGENERATION_JOB, REGENERATION_TASK_DELAY_MS) {
            regenerate()
        }

        asyncTaskService.launchRepeated(UPDATE_ABNORMALS_JOB, UPDATE_ABNORMALS_DELAY_MS) {
            updateAbnormals()
        }
    }

    /**
     * If player was not in PVP state - sets this actor pvp state to PVP,
     * broadcasts to all surrounding characters that actor is in PVP,
     * else - updates this actor combat time
     */
    suspend fun activatePvpState(character: PlayerCharacterInstance) {
        log.debug("Enabling (or updating) PVP state of '{}'", character)
        charactersInPvpState[character] = currentTimeMillis() + pvpFlagTimeMs

        if (character.pvpState != PvpState.PVP) {
            character.pvpState = PvpState.PVP
            this@ActorStateService.broadcastAround(character.position) { PvPStatusResponse(character) }
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
            if (actor is NpcInstanceImpl) {
                actor.moveType = MoveType.RUN
                this@ActorStateService.broadcastAround(actor.position) {
                    ChangeMoveTypeResponse(actor.id, actor.moveType)
                }
            }

            broadcastAround(actor.position) { StartFightingResponse(actor.id) }
        }

        fightingActors[actor] = currentTimeMillis() + COMBAT_TIME_MS
    }

    /**
     * Disables this actor's combat state.
     * Notifies surrounding characters about this and flushes combatState end time
     */
    suspend fun disableCombatState(actor: MutableActorInstance) {
        broadcastAround(actor.position) { StopFightingResponse(actor.id) }
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
            if (actor is NpcInstanceImpl) {
                actor.moveType = MoveType.WALK
                this@ActorStateService.broadcastAround(actor.position) {
                    ChangeMoveTypeResponse(actor.id, actor.moveType)
                }
            }

            disableCombatState(actor)
        }
    }

    private suspend fun updateCharactersPvpState() = charactersInPvpState.forEach { (character, pvpStateEndsTime) ->
        val pvpTimeLeft = pvpStateEndsTime - currentTimeMillis()
        when {
            pvpTimeLeft <= 0 -> suspendTransaction {
                character.pvpState = PvpState.NOT_IN_PVP
                this@ActorStateService.broadcastAround(character.position) { PvPStatusResponse(character) }
                charactersInPvpState.remove(character)
                log.debug("'{}' is now not in PVP", character)
            }

            pvpTimeLeft <= pvpFlagEndingTimeMs -> suspendTransaction {
                character.pvpState = PvpState.PVP_ENDING
                this@ActorStateService.broadcastAround(character.position) { PvPStatusResponse(character) }
                log.debug("Switched PVP state of '{}' to '{}'", character, character.pvpState)
            }
        }
    }

    private suspend fun regenerate() = gameObjectRepository.findAllActors().forEach { actor ->
        suspendTransaction {
            if (actor.isDead()) return@suspendTransaction

            val updatedStatuses = mutableMapOf<StatusAttribute, Int>()

            // Regenerate HP
            if (actor.stats.maxHp > actor.currentHp) {
                val hpRegeneration = actor.stats.hpRegen
                actor.currentHp = minOf(
                    actor.stats.maxHp.roundToInt(), actor.currentHp + hpRegeneration.roundToInt())

                //Both hp and mp must be sent, otherwise client does not update status
                updatedStatuses[StatusAttribute.CUR_HP] = actor.currentHp
                updatedStatuses[StatusAttribute.CUR_MP] = actor.currentMp
            }

            // Regenerate MP
            if (actor.stats.maxMp > actor.currentMp) {
                val mpRegeneration = actor.stats.mpRegen
                actor.currentMp = minOf(
                    actor.stats.maxMp.roundToInt(), actor.currentMp + mpRegeneration.roundToInt())

                //Both hp and mp must be sent, otherwise client does not update status
                updatedStatuses[StatusAttribute.CUR_HP] = actor.currentHp
                updatedStatuses[StatusAttribute.CUR_MP] = actor.currentMp
            }

            // Regenerate CP
            if (actor is PlayerCharacterInstanceImpl && actor.stats.maxCp > actor.currentCp) {
                val cpRegeneration = actor.stats.cpRegen
                actor.currentCp = minOf(
                    actor.stats.maxCp.roundToInt(), actor.currentCp + cpRegeneration.roundToInt())

                updatedStatuses[StatusAttribute.CUR_CP] = actor.currentCp
            }

            if (updatedStatuses.isNotEmpty())
                this@ActorStateService.broadcastAround(actor.position) {
                    UpdateStatusResponse(actor.id, updatedStatuses)
                }
        }
    }

    private suspend fun updateAbnormals() = gameObjectRepository.findAllActors().forEach { actor ->
        val outdatedEffects = actor.temporalEffects.filter { it.expiresAt.isBefore(Instant.now()) }

        if (outdatedEffects.isNotEmpty()) {
            if (actor.temporalEffects.removeAll(outdatedEffects)) suspendTransaction {
                log.debug("Successfully removed '{}' from '{}'", outdatedEffects, actor)

                if (outdatedEffects.any { it.abnormalVisualEffect != null }) broadcastActorInfo(actor)
                else if (actor is PlayerCharacterInstanceImpl) sendTo(actor.id) {
                    FullCharacterResponse(actor)
                }

                //TODO Notify about summon and party members effects
                sendTo(actor.id) { TemporalEffectsResponse(actor.temporalEffects) }
            }
        }
    }

}
