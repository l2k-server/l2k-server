package org.l2kserver.game.service

import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.l2kserver.game.model.skill.SkillTargetType
import org.l2kserver.game.model.skill.SkillType
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.handler.dto.request.UseSkillRequest
import org.l2kserver.game.handler.dto.response.ActionFailedResponse
import org.l2kserver.game.handler.dto.response.PlaySoundResponse
import org.l2kserver.game.handler.dto.response.GaugeColor
import org.l2kserver.game.handler.dto.response.GaugeResponse
import org.l2kserver.game.handler.dto.response.SkillListResponse
import org.l2kserver.game.handler.dto.response.SkillUsedResponse
import org.l2kserver.game.handler.dto.response.Sound
import org.l2kserver.game.handler.dto.response.StatusAttribute
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.model.actor.Actor
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.skill.Skill
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sessionContext
import org.l2kserver.game.repository.GameObjectRepository
import org.springframework.stereotype.Service
import java.time.Instant
import kotlin.collections.contains
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val REUSE_DELAY_COEFFICIENT = 333.0

/**
 * Handles learning, enchanting, using skills, etc.
 */
@Service
class SkillService(
    override val gameObjectRepository: GameObjectRepository,
    private val moveService: MoveService,
    private val asyncTaskService: AsyncTaskService
) : AbstractService() {

    override val log = logger()

    /**
     * Sends a full list of skills to the player in the current session
     */
    suspend fun getSkillList() = newSuspendedTransaction {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        send(SkillListResponse(character.skills.values))
        log.info("Successfully sent skill list to character {}", character)
    }

    /**
     * Handles request to use skill
     */
    suspend fun useSkill(request: UseSkillRequest): Unit = newSuspendedTransaction {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        val skill = character.getSkillById(request.skillId)

        useSkill(character, skill)
    }

    /**
     * Handles [actor]'s intent to use the [skill]
     */
    suspend fun useSkill(actor: Actor, skill: Skill) {
        log.debug("'{}' tries to use skill '{}'", actor, skill)
        when (skill.skillType) {
            SkillType.ACTIVE, SkillType.MAGIC -> useActiveSkill(actor, skill)
            SkillType.PASSIVE -> send(ActionFailedResponse)
            SkillType.TOGGLE -> {
                //TODO Toggle skills
                send(SystemMessageResponse("Toggle skills are not implemented yet"))
                send(PlaySoundResponse(Sound.ITEMSOUND_SYS_SHORTAGE))
                send(ActionFailedResponse)
            }
        }
    }

    /**
     * Handles [actor]'s intent to use `ACTIVE` [skill]
     */
    suspend fun useActiveSkill(actor: Actor, skill: Skill) = asyncTaskService.launchAction(actor.id) {
        //TODO Check casting
        if (Instant.now().isBefore(skill.nextUsageTime)) {
            send(SystemMessageResponse.IsBeingPreparedForReuse(skill))
            send(ActionFailedResponse)
        }
        else when (skill.targetType) {
            SkillTargetType.ENEMY -> useOffensiveTargetSkill(actor, skill)
        }
    }

    private suspend fun useOffensiveTargetSkill(actor: Actor, skill: Skill) = newSuspendedTransaction {
        //Check if actor can use skill
        if (!actor.canUseSkill(skill)) return@newSuspendedTransaction
        val target = actor.targetId?.let { gameObjectRepository.findActorByIdOrNull(it) } ?: run {
            send(SystemMessageResponse.TargetCannotBeFound)
            actor.targetId = null
            return@newSuspendedTransaction
        }

        try {
            moveService.move(actor, target, skill.castRange)

            if (!actor.position.isCloseTo(target.position, skill.castRange)) {
                send(SystemMessageResponse.TargetOutOfRange)
                return@newSuspendedTransaction
            }

            //Check if actor can use skill again - immediately before use
            if (actor.canUseSkill(skill)) {
                //TODO calculate spent resources
                if (actor is PlayerCharacter) {
                    val characterStatusResponse = UpdateStatusResponse(
                        objectId = actor.id,
                        StatusAttribute.CUR_HP to actor.currentHp,
                        StatusAttribute.CUR_MP to actor.currentMp,
                        StatusAttribute.CUR_CP to actor.currentCp,
                        StatusAttribute.MAX_CP to actor.stats.maxCp
                    )
                    send(characterStatusResponse)
                }

                val castTime = skill.castTime //TODO calculate depending on casting/attack speed

                val castingSpeed = if (skill.skillType == SkillType.MAGIC)
                    actor.stats.castingSpd else actor.stats.atkSpd
                val reuseDelay = (skill.reuseDelay * REUSE_DELAY_COEFFICIENT / castingSpeed).roundToInt()

                skill.nextUsageTime = Instant.now().plusMillis(reuseDelay.toLong())

                withContext(coroutineContext + NonCancellable) {
                    send(SystemMessageResponse.YouUse(skill))
                    send(GaugeResponse(GaugeColor.BLUE, castTime))
                    broadcastPacket(SkillUsedResponse(
                        casterId = actor.id,
                        targetId = target.id,
                        skillId = skill.skillId,
                        skillLevel = skill.skillLevel,
                        castTime = castTime,
                        reuseDelay = reuseDelay,
                        casterPosition = actor.position
                    ))
                    //Time, needed to cast a skill and finish skill animation TODO At L2J this time is set for every skill
                    val castingSkillTime = (castTime * 1.7).roundToLong()
                    delay(castingSkillTime)
                }

                //TODO Perform skill effect
                ///////////////////////////////////////////////////////////////////////////////
            }
        } catch (e: CancellationException) {
            log.debug("Casting skill '{}' by '{}' was interrupted for reason {}", skill, actor, e.message)
            send(ActionFailedResponse)
        }
    }

    /**
     * Checks if actor can use [skill]. If not - performs needed actions (sends system message, plays sound, etc.)
     * and returns false
     *
     * @param skill Skill that the actor is trying to use
     * @return true - if actor can use [skill], false if not
     */
    private suspend fun Actor.canUseSkill(skill: Skill): Boolean = when {
        skill.requires?.weaponTypes?.contains(this.weaponType) == false -> {
            send(PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE))
            send(ActionFailedResponse)
            false
        }
        skill.targetType == SkillTargetType.ENEMY && this.targetId == null -> {
            send(SystemMessageResponse.YouMustSelectTarget)
            send(PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE))
            send(ActionFailedResponse)
            false
        }
        skill.targetType == SkillTargetType.ENEMY && this.targetId == this.id -> {
            send(SystemMessageResponse.CannotUseThisOnYourself)
            send(PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE))
            send(ActionFailedResponse)
            false
        }
        skill.targetType == SkillTargetType.ENEMY && this.targetId?.let { gameObjectRepository.existsById(it) } != true -> {
            send(SystemMessageResponse.TargetCannotBeFound)
            send(PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE))
            send(ActionFailedResponse)
            this.targetId = null
            false
        }

        else -> true
    }

}
