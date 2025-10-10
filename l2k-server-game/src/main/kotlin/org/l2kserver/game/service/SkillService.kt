package org.l2kserver.game.service

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.l2kserver.game.model.skill.instance.SkillTargetType
import org.l2kserver.game.model.skill.instance.ActiveSkillType
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
import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.MutableActorInstance
import org.l2kserver.game.model.actor.Npc
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.actor.npc.NpcInstance
import org.l2kserver.game.model.extensions.forEachInstance
import org.l2kserver.game.model.item.template.SpiritshotType
import org.l2kserver.game.model.skill.ActiveSkill
import org.l2kserver.game.model.skill.PassiveSkill
import org.l2kserver.game.model.skill.ToggleSkill
import org.l2kserver.game.model.skill.action.SingleTargetMagicSkillAction
import org.l2kserver.game.model.skill.action.SingleTargetPhysicalSkillAction
import org.l2kserver.game.model.skill.effect.DamageEffect
import org.l2kserver.game.model.skill.effect.HealEffect
import org.l2kserver.game.model.skill.instance.ActiveSkillInstance
import org.l2kserver.game.model.skill.instance.SkillConsumables
import org.l2kserver.game.model.skill.instance.SkillInstance
import org.l2kserver.game.model.skill.template.SkillTemplateRegistry
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sendTo
import org.l2kserver.game.network.session.sessionContext
import org.l2kserver.game.repository.GameObjectRepository
import org.springframework.stereotype.Service
import java.time.Instant
import kotlin.collections.contains
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val CAST_TIME_COEFFICIENT = 333

/** Handles learning, enchanting, using skills, etc. */
@Service
class SkillService(
    private val combatService: CombatService,
    private val moveService: MoveService,
    private val itemService: ItemService,
    private val asyncTaskService: AsyncTaskService,
    private val npcService: NpcService,

    override val gameObjectRepository: GameObjectRepository
) : AbstractService() {

    override val log = logger()

    /** Sends a full list of skills to the player in the current session */
    suspend fun getSkillList() = newSuspendedTransaction {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        send { SkillListResponse(character.skillsAndMagic.values) }

        log.info("Successfully sent skill list to character {}", character)
    }

    suspend fun learnSkill(character: PlayerCharacter, skillId: Int, skillLevel: Int) = newSuspendedTransaction {
        //TODO checks if skill can be learned by this class, etc.
        val skillTemplate = SkillTemplateRegistry.findById(skillId)
        require(skillLevel in 0..skillTemplate.maxLevel) {
            "Cannot learn skill ${skillTemplate.skillName} on level $skillLevel- it's max level is ${skillTemplate.maxLevel}"
        }

        val learnedSkill = character.skillsAndMagic.learn(skillId, skillLevel)

        sendTo(character.id) { SkillListResponse(character.skillsAndMagic.values) }
        sendTo(character.id) { SystemMessageResponse.LearnedSkill(learnedSkill) }
    }

    /** Handles request to use skill */
    suspend fun useSkill(request: UseSkillRequest): Unit = newSuspendedTransaction {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        val skill = character.skillsAndMagic[request.skillId]

        useSkill(character, skill, request.forced, request.holdPosition)
    }

    /** Handles [actor]'s intent to use the [skill] */
    suspend fun useSkill(
        actor: MutableActorInstance,
        skill: SkillInstance,
        forced: Boolean = false,
        holdPosition: Boolean = false
    ) {
        log.debug("'{}' tries to use skill '{}'", actor, skill)
        when (skill) {
            is ActiveSkill -> useActiveSkill(actor, skill, forced, holdPosition)
            is PassiveSkill -> send { ActionFailedResponse }
            is ToggleSkill -> {
                //TODO Toggle skills
                send { SystemMessageResponse("Toggle skills are not implemented yet") }
                send { PlaySoundResponse(Sound.ITEMSOUND_SYS_SHORTAGE) }
                send { ActionFailedResponse }
            }

            else -> throw IllegalArgumentException("Unknown type of skill '$skill'")
        }
    }

    /**
     * Handles [actor]'s intent to use `ACTIVE` [skill]
     *
     * @param forced Skill will be applied even to wrong target (if possible)
     * @param holdPosition actor won't move closer to use skill
     */
    suspend fun useActiveSkill(
        actor: MutableActorInstance, skill: ActiveSkill, forced: Boolean, holdPosition: Boolean
    ) {
        //TODO Check if actor is already casting

        val target =
            if (skill.targetType == SkillTargetType.SELF) actor
            else actor.targetId?.let { gameObjectRepository.findActorByIdOrNull(it) }

        //TODO Introduce parameter - if target is enemy, but "friendly" skill used - fail using or use it on yourself
        // https://github.com/orgs/l2k-server/projects/1/views/3?pane=issue&itemId=124732573&issue=l2k-server%7Cl2k-server%7C47

        if (actor.canUseSkill(skill, target, forced)) asyncTaskService.launchAction(actor.id) {
            // If skill must be used on target - move to target
            if (skill.targetType != SkillTargetType.SELF) {
                //canUseSkill method also checks that target exists, so here we can use unsafe call
                val requiredDistance =
                    skill.castRange + (actor.collisionBox.radius + target!!.collisionBox.radius).roundToInt()
                if (!holdPosition) moveService.move(actor, target, requiredDistance)

                //Check if movement was interrupted or stopped at some obstacle
                if (!actor.position.isCloseTo(target.position, requiredDistance)) {
                    send { SystemMessageResponse.TargetOutOfRange }
                    return@launchAction
                }
            }

            // Check if actor can use skill - before casting skill
            if (!actor.canUseSkill(skill, target, forced)) return@launchAction

            // Casting animation
            // All skills that do not require a target are essentially cast on yourself
            actor.castSkillOn(skill, target ?: actor)
        }
    }

    /** Subtract HP, MP or items, required to use skill */
    private suspend fun MutableActorInstance.spendResources(consumables: SkillConsumables?) = newSuspendedTransaction {
        val actor = this@spendResources

        var statusUpdated = false
        consumables?.hp?.let { actor.currentHp -= it; statusUpdated = true }
        consumables?.mp?.let { actor.currentMp -= it; statusUpdated = true }

        if (actor is PlayerCharacter) consumables?.item?.let {
            val resourceItem = actor.inventory.findById(it.id)
            val reducedItem = actor.inventory.reduceAmount(it.id, it.amount)

            if (reducedItem == null) send { UpdateItemsResponse().wasDeleted(resourceItem) }
            else send { UpdateItemsResponse().wasModified(reducedItem) }
        }

        if (actor is PlayerCharacter && statusUpdated) send {
            UpdateStatusResponse(
                objectId = actor.id,
                StatusAttribute.CUR_HP to actor.currentHp,
                StatusAttribute.CUR_MP to actor.currentMp,
                StatusAttribute.CUR_CP to actor.currentCp,
                StatusAttribute.MAX_CP to actor.stats.maxCp
            )
        }
    }

    /** Cast [skill] and apply cooldown */
    private suspend fun MutableActorInstance.castSkillOn(
        skill: ActiveSkill, target: MutableActorInstance
    ) {
        val castingSpeed = when (skill.skillType) {
            ActiveSkillType.ACTIVE -> this.stats.atkSpd
            ActiveSkillType.MAGIC -> this.stats.castingSpd
        }

        val blessedSpiritshotCharged = (this as? PlayerCharacter)
            ?.inventory?.weapon?.spiritshotChargedType == SpiritshotType.BLESSED_SPIRITSHOT

        val blessedSpiritshotCastSpeedBonus =
            if (skill.skillType == ActiveSkillType.MAGIC && blessedSpiritshotCharged) 1.5
            else 1.0

        val castTime = skill.castTime * CAST_TIME_COEFFICIENT / castingSpeed / blessedSpiritshotCastSpeedBonus
        val repriseTime = skill.repriseTime * CAST_TIME_COEFFICIENT / castingSpeed / blessedSpiritshotCastSpeedBonus
        val reuseDelay = skill.reuseDelay * CAST_TIME_COEFFICIENT / castingSpeed / blessedSpiritshotCastSpeedBonus

        skill.nextUsageTime = Instant.now().plusMillis(reuseDelay.roundToLong())
        this.spendResources(skill.consumesToStart)

        withContext(kotlin.coroutines.coroutineContext + NonCancellable) {
            send { SystemMessageResponse.YouUse(skill) }
            send { GaugeResponse(GaugeColor.BLUE, castTime.roundToInt()) }

            this@SkillService.broadcastAround(this@castSkillOn.position) {
                SkillUsedResponse(
                    casterId = this@castSkillOn.id,
                    targetId = target.id,
                    skillId = skill.skillId,
                    skillLevel = skill.skillLevel,
                    castTime = castTime.roundToInt(),
                    reuseDelay = reuseDelay.roundToInt(),
                    casterPosition = this@castSkillOn.position
                )
            }

            //Time, needed to cast a skill
            delay(castTime.toLong())

            skill.applyEffects(this@castSkillOn, target)

            if (skill.targetType == SkillTargetType.DEAD_NPC) npcService.remove(target as Npc)
            this@castSkillOn.spendResources(skill.consumes)

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
    private suspend fun ActorInstance.canUseSkill(
        skill: ActiveSkillInstance, target: ActorInstance?, forced: Boolean
    ): Boolean = when {
        this.isParalyzed || this.isDead() -> false //TODO Physical/Magical silence

        skill.requires?.weaponTypes?.contains(this.weaponType) == false -> {
            send { PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE) }
            send { ActionFailedResponse }
            false
        }

        Instant.now().isBefore(skill.nextUsageTime) -> {
            send { SystemMessageResponse.IsBeingPreparedForReuse(skill) }
            send { ActionFailedResponse }
            false
        }

        (skill.consumes?.hp ?: 0) + (skill.consumesToStart?.hp ?: 0) > this.currentHp -> {
            send { PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE) }
            send { SystemMessageResponse.NotEnoughHp }
            send { ActionFailedResponse }
            false
        }

        (skill.consumes?.mp ?: 0) + (skill.consumesToStart?.mp ?: 0) > this.currentMp -> {
            send { PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE) }
            send { SystemMessageResponse.NotEnoughMp }
            send { ActionFailedResponse }
            false
        }

        this is PlayerCharacter && !this.hasEnoughConsumableItemFor(skill) -> {
            send { PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE) }
            send { SystemMessageResponse.NotEnoughItems }
            send { ActionFailedResponse }
            false
        }

        skill.targetType != SkillTargetType.SELF && this.targetId == null -> {
            send { SystemMessageResponse.YouMustSelectTarget }
            send { PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE) }
            send { ActionFailedResponse }
            false
        }

        skill.targetType != SkillTargetType.SELF && target == null -> {
            send { SystemMessageResponse.TargetCannotBeFound }
            send { PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE) }
            send { ActionFailedResponse }
            false
        }

        skill.targetType != SkillTargetType.FRIEND && this.targetId == this.id -> {
            send { SystemMessageResponse.CannotUseThisOnYourself }
            send { PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE) }
            send { ActionFailedResponse }
            false
        }

        skill.targetType == SkillTargetType.ENEMY && target?.isEnemyOf(this) == false && !forced -> {
            send { ActionFailedResponse }
            false
        }

        skill.targetType in listOf(
            SkillTargetType.DEAD_NPC,
            SkillTargetType.DEAD_PLAYER
        ) && target?.isDead() != true -> {
            send { SystemMessageResponse.IncorrectTarget }
            send { ActionFailedResponse }
            false
        }

        skill.targetType == SkillTargetType.DEAD_NPC && (target?.isDead() == false || target !is NpcInstance) -> {
            send { SystemMessageResponse.IncorrectTarget }
            send { ActionFailedResponse }
            false
        }

        skill.targetType == SkillTargetType.DEAD_PLAYER && (target?.isDead() == false || target !is PlayerCharacter) -> {
            send { SystemMessageResponse.IncorrectTarget }
            send { ActionFailedResponse }
            false
        }

        //TODO Check PeaceZone
        //TODO Check geodata (can see target)
        else -> true
    }

    /** Checks if PlayerCharacter has enough consumable item in the inventory */
    private fun PlayerCharacter.hasEnoughConsumableItemFor(skill: ActiveSkillInstance?): Boolean {
        val consumableToStart = skill?.consumesToStart?.item
        val consumable = skill?.consumes?.item

        return when {
            consumableToStart == null && consumable == null -> true

            consumableToStart?.id == consumable?.id -> this.inventory.existsByIdAndAmount(
                consumable!!.id,
                consumable!!.amount + consumableToStart!!.amount
            )

            else -> {
                consumableToStart?.let { this.inventory.existsByIdAndAmount(it.id, it.amount) } != false
                        && consumable?.let { this.inventory.existsByIdAndAmount(it.id, it.amount) } != false
            }
        }
    }

    /** Applies cast by [caster] skill effects on [target] */
    private suspend fun ActiveSkill.applyEffects(
        caster: MutableActorInstance, target: MutableActorInstance
    ) = newSuspendedTransaction {
        val effects = try {
            when (val action = this@applyEffects.skillAction) {
                is SingleTargetPhysicalSkillAction -> {
                    val soulshotUsed = (caster as? PlayerCharacter)?.inventory?.weapon?.soulshotCharged ?: false
                    action.apply(target, caster, this@applyEffects.skillLevel, soulshotUsed)
                        .also {
                            if (soulshotUsed) caster.inventory.weapon?.soulshotCharged = false

                            //Enable SS if auto-use soulshot enabled
                            (caster as? PlayerCharacter)?.autoUsesSoulshot?.let {
                                itemService.useSoulshot(caster, it)
                            }
                        }
                }

                is SingleTargetMagicSkillAction -> {
                    val usedSpiritshotType = (caster as? PlayerCharacter)?.inventory?.weapon?.spiritshotChargedType
                    action.apply(target, caster, this@applyEffects.skillLevel, usedSpiritshotType).also {
                        if (usedSpiritshotType != null) caster.inventory.weapon?.spiritshotChargedType = null

                        //Enable SS if auto-use spiritshot enabled
                        (caster as? PlayerCharacter)?.autoUsesSpiritshot?.let {
                            itemService.useSpiritshot(caster, it)
                        }
                    }
                }

                else -> emptyList()
            }
        } catch (e: Exception) {
            log.error("An error occurred while trying to apply effect {}", this@applyEffects.skillAction, e)
            emptyList()
        }

        effects.forEach { effect ->
            when (effect) {
                is DamageEffect -> combatService.applyDamageEffect(
                    caster, effect, this@applyEffects, overhitPossible
                )

                is HealEffect -> applyHealEffect(caster, effect)
                else -> {}
            }
        }
    }

    private suspend fun applyHealEffect(caster: MutableActorInstance, effect: HealEffect) {
        val target = gameObjectRepository.findActorByIdOrNull(effect.targetId) ?: return

        target.currentHp = minOf(target.currentHp + effect.value, target.stats.maxHp)
        val healerName = if (caster == target) null else caster.name

        val updateStatusResponse by lazy { UpdateStatusResponse.hpMpCpOf(target) }

        send { updateStatusResponse }
        send { SystemMessageResponse.HpRestored(effect.value, healerName) }

        if (target is NpcInstance) target.targetedBy.forEachInstance<PlayerCharacter> {
            sendTo(it.id) { updateStatusResponse }
        }
    }

}
