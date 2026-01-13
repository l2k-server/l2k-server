package org.l2kserver.game.service

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.l2kserver.game.model.skill.instance.SkillTargetType
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.extensions.model.actor.hasEnoughConsumableItemFor
import org.l2kserver.game.extensions.model.actor.hasEnoughHpToCast
import org.l2kserver.game.extensions.model.actor.hasEnoughMpToCast
import org.l2kserver.game.extensions.model.skill.isOnCooldown
import org.l2kserver.game.handler.dto.request.UseSkillRequest
import org.l2kserver.game.handler.dto.response.TemporalEffectsResponse
import org.l2kserver.game.handler.dto.response.ActionFailedResponse
import org.l2kserver.game.handler.dto.response.FullCharacterResponse
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
import org.l2kserver.game.model.actor.NpcInstanceImpl
import org.l2kserver.game.model.actor.PlayerCharacterInstanceImpl
import org.l2kserver.game.model.actor.character.PlayerCharacterInstance
import org.l2kserver.game.model.actor.npc.NpcInstance
import org.l2kserver.game.model.extensions.forEachInstance
import org.l2kserver.game.model.extensions.safePlus
import org.l2kserver.game.model.item.template.SpiritshotType
import org.l2kserver.game.model.skill.context.SkillContext
import org.l2kserver.game.model.skill.effect.DamageEffect
import org.l2kserver.game.model.skill.effect.Effect
import org.l2kserver.game.model.skill.effect.EffectOnTimeAbnormalEffect
import org.l2kserver.game.model.skill.effect.HealEffect
import org.l2kserver.game.model.skill.effect.ResistedEffect
import org.l2kserver.game.model.skill.effect.TemporalAbnormalEffect
import org.l2kserver.game.model.skill.instance.ActiveSkillInstance
import org.l2kserver.game.model.skill.instance.PassiveSkillInstance
import org.l2kserver.game.model.skill.instance.SkillConsumables
import org.l2kserver.game.model.skill.instance.SkillInstance
import org.l2kserver.game.model.skill.instance.ToggleSkillInstance
import org.l2kserver.game.model.skill.template.SkillRegistry
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sendTo
import org.l2kserver.game.network.session.sessionContext
import org.l2kserver.game.repository.GameObjectRepository
import org.l2kserver.game.utils.time.withDelay
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
    private val itemService: ItemService,
    private val asyncTaskService: AsyncTaskService,
    private val npcService: NpcService,
    private val actorStateService: ActorStateService,

    override val gameObjectRepository: GameObjectRepository
) : AbstractService() {

    override val log = logger()

    /** Sends a full list of skills to the player in the current session */
    suspend fun getSkillList() = suspendTransaction {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        send { SkillListResponse(character.skillsAndMagic) }

        log.info("Successfully sent skill list to character {}", character)
    }

    suspend fun learnSkill(character: PlayerCharacterInstanceImpl, skillId: Int, skillLevel: Int) = suspendTransaction {
        //TODO checks if skill can be learned by this class, etc.
        val skillTemplate = SkillRegistry.findById(skillId)
        require(skillLevel in 0..skillTemplate.maxLevel) {
            "Cannot learn skill ${skillTemplate.skillName} on level $skillLevel " +
                    "- it's max level is ${skillTemplate.maxLevel}"
        }

        val learnedSkill = character.skillsAndMagic.learn(skillId, skillLevel)

        sendTo(character.id) { SkillListResponse(character.skillsAndMagic) }
        sendTo(character.id) { SystemMessageResponse.LearnedSkill(learnedSkill) }
    }

    /** Handles request to use skill */
    suspend fun useSkill(request: UseSkillRequest): Unit = suspendTransaction {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        val skill = character.skillsAndMagic.findById(request.skillId)

        useSkill(character, skill, request.forced, request.holdPosition)
    }

    /** Handles [actor]'s intent to use the [skill] */
    suspend fun useSkill(
        actor: MutableActorInstance, skill: SkillInstance, forced: Boolean = false, holdPosition: Boolean = false
    ) {
        log.debug("'{}' tries to use skill '{}'", actor, skill)
        when (skill) {
            is ActiveSkillInstance -> useActiveSkill(
                actor, skill, forced = forced && skill.forcedUsageAllowed, holdPosition
            )

            is PassiveSkillInstance -> send { ActionFailedResponse }
            is ToggleSkillInstance -> {
                //TODO Toggle skills
                send { SystemMessageResponse("Toggle skills are not implemented yet") }
                send { PlaySoundResponse(Sound.ITEMSOUND_SYS_SHORTAGE) }
                send { ActionFailedResponse }
            }
        }
    }

    /**
     * Handles [actor]'s intent to use `ACTIVE` [skill]
     *
     * @param forced Skill will be applied even to wrong target (if possible)
     * @param holdPosition actor won't move closer to use skill
     */
    private suspend fun useActiveSkill(
        actor: MutableActorInstance, skill: ActiveSkillInstance, forced: Boolean, holdPosition: Boolean
    ) {
        //TODO Check if actor is already casting

        val target = when {
            skill.targetType == SkillTargetType.SELF -> actor
            actor.targetId == null -> {
                send { SystemMessageResponse.YouMustSelectTarget }
                send { PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE) }
                return
            }

            else -> actor.targetId?.let { gameObjectRepository.findActorByIdOrNull(it) } ?: run {
                send { SystemMessageResponse.TargetCannotBeFound }
                send { PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE) }
                return
            }
        }

        //TODO Introduce parameter - if target is enemy, but "friendly" skill used - fail using or use it on yourself
        // https://github.com/orgs/l2k-server/projects/1/views/3?pane=issue&itemId=124732573&issue=l2k-server%7Cl2k-server%7C47

        if (actor.canUseSkill(skill, target, forced)) asyncTaskService.launchAction(actor.id) {
            // If skill must be used on target - move to target
            if (skill.targetType != SkillTargetType.SELF) {
                //canUseSkill method also checks that target exists, so here we can use unsafe call
                val requiredDistance =
                    skill.castRange + (actor.collisionBox.radius + target.collisionBox.radius).roundToInt()
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
            castSkill(actor, target, skill)
        }
    }

    /** Cast [skill] and apply cooldown */
    suspend fun castSkill(
        caster: MutableActorInstance, target: MutableActorInstance, skill: ActiveSkillInstance
    ) {
        var castingSpeed = if (skill.isMagic) caster.stats.castingSpd.toDouble()
        else caster.stats.atkSpd.toDouble()

        val blessedSpiritshotCharged = (caster as? PlayerCharacterInstanceImpl)
            ?.inventory?.weapon?.spiritshotChargedType == SpiritshotType.BLESSED_SPIRITSHOT

        if (skill.isMagic && blessedSpiritshotCharged) castingSpeed *= 1.5

        var castTime = skill.castTime
        var repriseTime = skill.repriseTime
        var reuseDelay = skill.reuseDelay

        if (skill.usesCasterStats) {
            castTime = (castTime * CAST_TIME_COEFFICIENT / castingSpeed).roundToInt()
            repriseTime = (repriseTime * CAST_TIME_COEFFICIENT / castingSpeed).roundToInt()
            reuseDelay = (reuseDelay * CAST_TIME_COEFFICIENT / castingSpeed).roundToInt()
        }

        skill.nextUsageTime = Instant.now().plusMillis(reuseDelay.toLong())
        caster.spendResources(skill.consumesToStart)

        withContext(currentCoroutineContext() + NonCancellable) {
            send { SystemMessageResponse.YouUse(skill) }
            send { GaugeResponse(GaugeColor.BLUE, castTime) }

            this@SkillService.broadcastAround(caster.position) {
                SkillUsedResponse(
                    casterId = caster.id,
                    targetId = target.id,
                    skillId = skill.skillId,
                    skillLevel = skill.skillLevel,
                    castTime = castTime,
                    reuseDelay = reuseDelay,
                    casterPosition = caster.position
                )
            }

            //Time, needed to cast a skill
            delay(castTime.toLong())
            applyEffects(skill, caster, target)

            caster.spendResources(skill.consumes)

            when (skill.targetType) {
                SkillTargetType.ENEMY -> {
                    actorStateService.activateCombatState(caster)
                    actorStateService.activateCombatState(target)
                    if (
                        caster is PlayerCharacterInstanceImpl &&
                        target is PlayerCharacterInstanceImpl &&
                        target.karma == 0
                    ) {
                        actorStateService.activatePvpState(caster)
                    }
                }

                SkillTargetType.FRIEND -> (caster as? PlayerCharacterInstanceImpl)?.let { character ->
                    if (target.isEnemyOf(character)) actorStateService.activatePvpState(character)
                }

                SkillTargetType.DEAD_NPC -> npcService.remove(target as NpcInstanceImpl)
                else -> {}
            }

            //Time to finish cast animation
            delay(repriseTime.toLong())
        }
    }

    /** Subtract HP, MP or items, required to use skill */
    private suspend fun MutableActorInstance.spendResources(
        consumables: SkillConsumables?
    ) = suspendTransaction {
        val actor = this@spendResources

        var statusUpdated = false

        consumables?.hp?.let {
            if (it > 0) {
                actor.currentHp -= it
                statusUpdated = true
            }
        }

        consumables?.mp?.let {
            if (it > 0) {
                actor.currentMp -= it
                statusUpdated = true
            }
        }

        if (actor is PlayerCharacterInstanceImpl) consumables?.item?.let {
            val resourceItem = actor.inventory.findAllByTemplateId(it.templateId).first()
            val reducedItem = actor.inventory.reduceAmount(resourceItem.id, it.amount)

            if (reducedItem == null) send { UpdateItemsResponse().wasDeleted(resourceItem) }
            else send { UpdateItemsResponse().wasModified(reducedItem) }
        }

        if (actor is PlayerCharacterInstanceImpl && statusUpdated) send {
            UpdateStatusResponse(
                objectId = actor.id,
                StatusAttribute.CUR_HP to actor.currentHp,
                StatusAttribute.CUR_MP to actor.currentMp,
                StatusAttribute.CUR_CP to actor.currentCp,
                StatusAttribute.MAX_CP to actor.stats.maxCp.roundToInt()
            )
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
    @Suppress("CyclomaticComplexMethod")
    private suspend fun ActorInstance.canUseSkill(
        skill: ActiveSkillInstance, target: ActorInstance, forced: Boolean
    ): Boolean = when {
        //TODO Physical/Magical silence
        this.isParalyzed || this.isDead() -> {
            send { ActionFailedResponse }
            false
        }

        skill.requires?.weaponTypes?.contains(this.weaponType) == false -> {
            send { PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE) }
            send { ActionFailedResponse }
            false
        }

        skill.isOnCooldown() -> {
            send { SystemMessageResponse.IsBeingPreparedForReuse(skill) }
            send { ActionFailedResponse }
            false
        }

        !this.hasEnoughHpToCast(skill) -> {
            send { PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE) }
            send { SystemMessageResponse.NotEnoughHp }
            send { ActionFailedResponse }
            false
        }

        !this.hasEnoughMpToCast(skill) -> {
            send { PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE) }
            send { SystemMessageResponse.NotEnoughMp }
            send { ActionFailedResponse }
            false
        }

        this is PlayerCharacterInstanceImpl && !this.hasEnoughConsumableItemFor(skill) -> {
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

        skill.targetType !in listOf(SkillTargetType.SELF, SkillTargetType.FRIEND) && this == target -> {
            send { SystemMessageResponse.CannotUseThisOnYourself }
            send { PlaySoundResponse(Sound.ITEMSOUND_SYS_IMPOSSIBLE) }
            send { ActionFailedResponse }
            false
        }

        skill.targetType == SkillTargetType.FRIEND && (target.isDead() || target.isEnemyOf(this) && !forced) -> {
            send { SystemMessageResponse.IncorrectTarget }
            send { ActionFailedResponse }
            false
        }

        skill.targetType == SkillTargetType.DEAD_NPC && (!target.isDead() || target !is NpcInstance) -> {
            send { SystemMessageResponse.IncorrectTarget }
            send { ActionFailedResponse }
            false
        }

        skill.targetType == SkillTargetType.DEAD_PLAYER && (!target.isDead() || target !is PlayerCharacterInstance) -> {
            send { SystemMessageResponse.IncorrectTarget }
            send { ActionFailedResponse }
            false
        }

        skill.targetType == SkillTargetType.ENEMY && (target.isDead() || (!target.isEnemyOf(this) && !forced)) -> {
            send { SystemMessageResponse.IncorrectTarget }
            send { ActionFailedResponse }
            false
        }

        //TODO Check PeaceZone
        //TODO Check geodata (can see target)
        else -> true
    }

    /** Applies cast by [caster] [skill] effects on [target] */
    private suspend fun applyEffects(
        skill: ActiveSkillInstance, caster: MutableActorInstance, target: MutableActorInstance
    ) = suspendTransaction {
        val context = SkillContext(
            caster = caster,
            mainTarget = target,
            skillLevel = skill.skillLevel,
            additionalEnemyTargets = emptyList(), //TODO
            additionalFriendlyTargets = emptyList(), //TODO
            usedSoulshot = (caster as? PlayerCharacterInstanceImpl)?.inventory?.weapon?.soulshotCharged ?: false,
            usedSpiritshotType = (caster as? PlayerCharacterInstanceImpl)?.inventory?.weapon?.spiritshotChargedType
        )

        val effects = try {
            skill.affect(context)
        } catch (e: Exception) {
            log.error("An error occurred while trying to apply effect of {}", skill, e)
            emptyList()
        }

        //Use SS and re-enable it if auto-use enabled
        if (skill.isMagic) {
            if (context.usedSpiritshotType != null && caster is PlayerCharacterInstanceImpl) {
                caster.inventory.weapon?.spiritshotChargedType = null
                caster.autoUsesSpiritshot?.let { itemService.useSpiritshot(caster, it) }
            }
        } else {
            if (context.usedSoulshot && caster is PlayerCharacterInstanceImpl) {
                caster.inventory.weapon?.soulshotCharged = false
                caster.autoUsesSoulshot?.let { itemService.useSoulshot(caster, it) }
            }
        }

        applyEffects(effects, caster, skill)
    }

    private suspend fun applyEffects(
        effects: Iterable<Effect>, caster: MutableActorInstance, skill: ActiveSkillInstance
    ) = effects.forEach { effect ->
        //TODO Abnormals should not show caster in system message (???)
        val target = gameObjectRepository.findActorByIdOrNull(effect.targetId) ?: return@forEach

        when (effect) {
            is DamageEffect -> combatService.applyDamageEffect(caster, effect, skill)
            is HealEffect -> applyHealEffect(caster, effect)
            is TemporalAbnormalEffect -> applyAbnormalEffect(caster, effect, skill)
            is ResistedEffect -> {
                sendTo(caster.id) {
                    SystemMessageResponse.HasResisted(target.name, skill)
                }
                sendTo(target.id) {
                    SystemMessageResponse.YouHaveResistedMagic(caster.name)
                }
            }
        }
    }

    private suspend fun applyHealEffect(caster: MutableActorInstance, effect: HealEffect) = suspendTransaction {
        val target = gameObjectRepository.findActorByIdOrNull(effect.targetId) ?: return@suspendTransaction

        target.currentHp = minOf(target.currentHp safePlus effect.value, target.stats.maxHp.roundToInt())
        val healerName = if (caster == target) null else caster.name

        val updateStatusResponse by lazy { UpdateStatusResponse.hpMpCpOf(target) }

        sendTo(target.id) { updateStatusResponse }
        sendTo(target.id) { SystemMessageResponse.HpRestored(effect.value, healerName) }

        if (target is NpcInstance) target.targetedBy.forEachInstance<PlayerCharacterInstanceImpl> {
            sendTo(it.id) { updateStatusResponse }
        }
    }

    private suspend fun applyAbnormalEffect(
        caster: MutableActorInstance, effect: TemporalAbnormalEffect, skill: ActiveSkillInstance
    ): Unit = suspendTransaction {
        val target = gameObjectRepository.findActorByIdOrNull(effect.targetId) ?: return@suspendTransaction
        if (target.temporalEffects.add(effect) && target is PlayerCharacterInstanceImpl) {
            sendTo(target.id) { FullCharacterResponse(target) }
            sendTo(target.id) { TemporalEffectsResponse(target.temporalEffects) }
            //TODO Summon abnormals must be shown to master
            //TODO Party notification
        }

        if (effect is EffectOnTimeAbnormalEffect) asyncTaskService.launchOnce {
            while (target.exists() && target.temporalEffects.contains(effect)) withDelay(effect.frequency) {
                val context = SkillContext(
                    caster = caster,
                    mainTarget = target,
                    skillLevel = effect.effectLevel,
                    additionalEnemyTargets = emptyList(),
                    additionalFriendlyTargets = emptyList()
                )
                applyEffects(effect.effects(context), caster, skill)
            }
        }

        if (effect.abnormalVisualEffect != null) broadcastActorInfo(target)
    }

}
