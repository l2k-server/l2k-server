package org.l2kserver.game.service

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
import org.l2kserver.game.handler.dto.response.UpdateItemsResponse
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.model.actor.Actor
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.item.ConsumableItem
import org.l2kserver.game.model.skill.Skill
import org.l2kserver.game.model.skill.effect.SingleTargetSkillEffect
import org.l2kserver.game.model.skill.effect.event.DamageEvent
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sessionContext
import org.l2kserver.game.repository.GameObjectRepository
import org.springframework.stereotype.Service
import java.time.Instant
import kotlin.collections.contains
import kotlin.math.roundToInt

private const val CAST_TIME_COEFFICIENT = 333

/** Handles learning, enchanting, using skills, etc. */
@Service
class SkillService(
    private val combatService: CombatService,
    private val moveService: MoveService,
    private val asyncTaskService: AsyncTaskService,
    override val gameObjectRepository: GameObjectRepository
) : AbstractService() {

    override val log = logger()

    /** Sends a full list of skills to the player in the current session */
    suspend fun getSkillList() = newSuspendedTransaction {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        send(SkillListResponse(character.skills.values))
        log.info("Successfully sent skill list to character {}", character)
    }

    /** Handles request to use skill */
    suspend fun useSkill(request: UseSkillRequest): Unit = newSuspendedTransaction {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        val skill = character.getSkillById(request.skillId)

        useSkill(character, skill, request.forced, request.holdPosition)
    }

    /** Handles [actor]'s intent to use the [skill] */
    suspend fun useSkill(actor: Actor, skill: Skill, forced: Boolean = false, holdPosition: Boolean = false) {
        log.debug("'{}' tries to use skill '{}'", actor, skill)
        when (skill.skillType) {
            SkillType.ACTIVE, SkillType.MAGIC -> useActiveSkill(actor, skill, forced, holdPosition)
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
     *
     * @param forced Skill will be applied even to wrong target (if possible)
     * @param holdPosition actor won't move closer to use skill
     */
    suspend fun useActiveSkill(
        actor: Actor, skill: Skill, forced: Boolean, holdPosition: Boolean
    ) = asyncTaskService.launchAction(actor.id) {
        //TODO Check if actor is already casting
        val target = actor.targetId?.let { gameObjectRepository.findActorByIdOrNull(it) }

        //TODO Introduce parameter - if target is enemy, but "friendly" skill used - fail using or use it on yourself
        // https://github.com/orgs/l2k-server/projects/1/views/3?pane=issue&itemId=124732573&issue=l2k-server%7Cl2k-server%7C47

        // If skill must be used on target - move to target
        if (skill.targetType != SkillTargetType.SELF) {
            //Check that actor can use skill - before moving to it
            if (!actor.canUseSkill(skill, target, forced)) return@launchAction

            //canUseSkill method also checks that target exists, so here we can use unsafe call
            val requiredDistance =
                skill.castRange + (actor.collisionBox.radius + target!!.collisionBox.radius).roundToInt()
            if (!holdPosition) moveService.move(actor, target, requiredDistance)

            //Check if movement was interrupted or stopped at some obstacle
            if (!actor.position.isCloseTo(target.position, requiredDistance)) {
                send(SystemMessageResponse.TargetOutOfRange)
                return@launchAction
            }
        }

        // Check if actor can use skill - before casting skill
        if (!actor.canUseSkill(skill, target, forced)) return@launchAction

        // Consume resources
        actor.spendResourcesFor(skill)

        // Casting animation
        // All skills that do not require a target are essentially cast on yourself
        actor.castSkillOn(skill, target ?: actor)
    }

    /** Subtract HP, MP or items, required to use [skill] */
    private suspend fun Actor.spendResourcesFor(skill: Skill) = newSuspendedTransaction {
        val actor = this@spendResourcesFor

        var statusUpdated = false
        skill.consumes?.hp?.let { actor.currentHp -= it; statusUpdated = true }
        skill.consumes?.mp?.let { actor.currentMp -= it; statusUpdated = true }
        if (actor is PlayerCharacter) skill.consumes?.item?.let {
            val resourceItem = actor.inventory.findById(it.id)
            val reducedItem = actor.inventory.reduceAmount(it.id, it.amount)
            if (reducedItem == null) send(UpdateItemsResponse.operationRemove(resourceItem))
            else send(UpdateItemsResponse.operationModify(reducedItem))
        }

        if (actor is PlayerCharacter && statusUpdated) send(
            UpdateStatusResponse(
                objectId = actor.id,
                StatusAttribute.CUR_HP to actor.currentHp,
                StatusAttribute.CUR_MP to actor.currentMp,
                StatusAttribute.CUR_CP to actor.currentCp,
                StatusAttribute.MAX_CP to actor.stats.maxCp
            )
        )
    }

    /** Cast [skill] and apply cooldown */
    private suspend fun Actor.castSkillOn(skill: Skill, target: Actor) {
        val castingSpeed = if (skill.skillType == SkillType.MAGIC) this.stats.castingSpd else this.stats.atkSpd

        val castTime = skill.castTime * CAST_TIME_COEFFICIENT / castingSpeed
        val repriseTime = skill.repriseTime * CAST_TIME_COEFFICIENT / castingSpeed
        val reuseDelay = skill.reuseDelay * CAST_TIME_COEFFICIENT / castingSpeed

        newSuspendedTransaction { skill.nextUsageTime = Instant.now().plusMillis(reuseDelay.toLong()) }

        withContext(kotlin.coroutines.coroutineContext + NonCancellable) {
            send(SystemMessageResponse.YouUse(skill), GaugeResponse(GaugeColor.BLUE, castTime))
            broadcastPacket(
                SkillUsedResponse(
                    casterId = this@castSkillOn.id,
                    targetId = target.id,
                    skillId = skill.skillId,
                    skillLevel = skill.skillLevel,
                    castTime = castTime,
                    reuseDelay = reuseDelay,
                    casterPosition = this@castSkillOn.position
                )
            )

            //Time, needed to cast a skill
            delay(castTime.toLong())

            skill.applyEffects(this@castSkillOn, target)

            //Time to finish cast animation
            delay(repriseTime.toLong())
        }
    }

    /**
     * Checks if actor can use [skill]. If not - performs needed actions (sends system message, plays sound, etc.)
     * and returns false
     *
     * @param skill Skill that the actor is trying to use
     * @param target Skill target
     * @param forced Is this skill forced to use (ctrl pressed)
     * @return true - if actor can use [skill], false if not
     */
    private suspend fun Actor.canUseSkill(skill: Skill, target: Actor?, forced: Boolean): Boolean = when {
        this.isParalyzed || this.isDead() -> false //TODO Physical/Magical silence
        skill.requires?.weaponTypes?.contains(this.weaponType) == false -> {
            send(PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE), ActionFailedResponse)
            false
        }
        Instant.now().isBefore(skill.nextUsageTime) -> {
            send(SystemMessageResponse.IsBeingPreparedForReuse(skill), ActionFailedResponse)
            false
        }
        (skill.consumes?.hp ?: 0) > this.currentHp -> {
            send(
                PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE),
                SystemMessageResponse.NotEnoughHp,
                ActionFailedResponse
            )
            false
        }
        (skill.consumes?.mp ?: 0) > this.currentMp -> {
            send(
                PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE),
                SystemMessageResponse.NotEnoughMp,
                ActionFailedResponse
            )
            false
        }
        this is PlayerCharacter && !this.hasEnoughConsumable(skill.consumes?.item) -> {
            send(
                PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE),
                SystemMessageResponse.NotEnoughItems,
                ActionFailedResponse
            )
            false
        }
        skill.targetType != SkillTargetType.SELF && this.targetId == null -> {
            send(SystemMessageResponse.YouMustSelectTarget)
            send(PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE))
            send(ActionFailedResponse)
            false
        }
        skill.targetType != SkillTargetType.SELF && target == null -> {
            send(SystemMessageResponse.TargetCannotBeFound)
            send(PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE))
            send(ActionFailedResponse)
            false
        }
        skill.targetType != SkillTargetType.FRIEND && this.targetId == this.id -> {
            send(SystemMessageResponse.CannotUseThisOnYourself)
            send(PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE))
            send(ActionFailedResponse)
            false
        }
        skill.targetType == SkillTargetType.ENEMY && target?.isEnemyOf(this) == false && !forced -> {
            send(ActionFailedResponse)
            false
        }

        skill.castsOnCorpse() xor (target?.isDead() == true) -> {
            send(SystemMessageResponse.IncorrectTarget, ActionFailedResponse)
            false
        }

        skill.targetType == SkillTargetType.DEAD_ENEMY && (target?.isDead() == false || target?.isEnemyOf(this) == false) -> {
            send(SystemMessageResponse.IncorrectTarget, ActionFailedResponse)
            false
        }

        skill.targetType == SkillTargetType.DEAD_FRIEND && (target?.isDead() == false || target?.isEnemyOf(this) == true) -> {
            send(SystemMessageResponse.IncorrectTarget, ActionFailedResponse)
            false
        }
        //TODO Check PeaceZone
        //TODO Check geodata
        else -> true
    }

    /** Checks if PlayerCharacter has enough consumable item in the inventory. If [consumable] is null - returns true */
    private fun PlayerCharacter.hasEnoughConsumable(consumable: ConsumableItem?): Boolean {
        return if (consumable == null) true
        else this.inventory.existsByIdAndAmount(consumable.id, consumable.amount)
    }

    /** Applies cast by [caster] skill effects on [target] */
    private suspend fun Skill.applyEffects(caster: Actor, target: Actor) = this.effects.forEach { effect ->
        val events = try {
            when (effect) {
                is SingleTargetSkillEffect -> effect.apply(caster, target, this.skillLevel)
                //TODO AOE skills
            }
        }
        catch (e: Exception) {
            log.error("An error occurred while trying to apply effect {}", effect, e)
            emptyList()
        }

        events.forEach { event ->
            when (event) {
                is DamageEvent -> combatService.performDamage(event, caster)
            }
        }
    }

}
