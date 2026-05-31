package org.l2kserver.game.service

import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.l2kserver.game.configuration.properties.LevelProperties
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.handler.dto.response.FullCharacterResponse
import org.l2kserver.game.handler.dto.response.PvPStatusResponse
import org.l2kserver.game.handler.dto.response.SocialActionResponse
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.model.SocialAction
import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.MutableActorInstance
import org.l2kserver.game.model.actor.NpcInstanceImpl
import org.l2kserver.game.model.actor.PlayerCharacterInstanceImpl
import org.l2kserver.game.model.actor.character.PlayerCharacterInstance
import org.l2kserver.game.model.actor.character.PvpState
import org.l2kserver.game.model.actor.npc.NpcInstance
import org.l2kserver.game.model.utils.withChance
import org.l2kserver.game.network.session.sendTo
import org.l2kserver.game.repository.GameObjectRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

/** Service for rewards calculation and management */
@Service
class RewardService(
    private val itemService: ItemService,
    override val gameObjectRepository: GameObjectRepository,
    private val levelProperties: LevelProperties,

    @param:Value($$"${game.karmaBaseAmount}") private val karmaBaseAmount: Int,
    @param:Value($$"${game.karmaMaxAmount}") private val karmaMaxAmount: Int,
    @param:Value($$"${game.karmaExpDivider}") private val karmaExpDivider: Int,
    @param:Value($$"${game.karmaLostMin}") private val karmaLostMin: Int,
    @param:Value($$"${reward.minLevelDifferenceForPenalty}") private val minLevelDifferenceForPenalty: Int,
    @param:Value($$"${reward.maxLevelDifferenceForPenalty}") private val maxLevelDifferenceForPenalty: Int,
    @param:Value($$"${reward.levelPenaltyBaseValue}") private val levelPenaltyBaseValue: Double
) : AbstractService() {

    override val log = logger()

    /** Gives [exp] and [sp] to character, reduces his karma, and manages LevelUp (if needed) */
    suspend fun giveExpAndSp(
        character: PlayerCharacterInstanceImpl, exp: Int, overhitExp: Int = 0, sp: Int = 0
    ) = suspendTransaction {
        val levelBefore = character.level

        character.exp = (character.exp + exp + overhitExp).coerceIn(levelProperties.getExpRange())
        character.sp += sp

        if (character.karma > 0)
            character.karma = maxOf(character.karma - calculateKarmaLossForExp(exp), 0)

        sendTo(character.id) { SystemMessageResponse.YouHaveEarnedExpAndSp(exp, sp) }

        if (overhitExp > 0) sendTo(character.id) {
            SystemMessageResponse.YouHaveAcquiredExpForOverHit(overhitExp)
        }

        sendTo(character.id) { FullCharacterResponse(character) }
        if (character.level > levelBefore) handleLevelUp(character)
    }

    suspend fun manageRewards(killer: MutableActorInstance, killed: MutableActorInstance) {
        if (killer !is PlayerCharacterInstanceImpl) return //Npc cannot get rewards for killing

        when (killed) {
            is NpcInstance -> manageRewardForKillingNpc(killer, killed)
            is PlayerCharacterInstanceImpl ->  manageRewardForKillingPlayer(killer, killed)
        }
    }

    /**
     * Manages rewards for killing NPC.
     * Calculates exp, sp, item drops, distributes the reward among the players
     */
    private suspend fun manageRewardForKillingNpc(killer: PlayerCharacterInstance, killed: NpcInstance) {
        manageItemRewards(killed)
        manageExpAndSpGain(killer, killed)
    }

    /**
     * Manages rewards for killing PlayerCharacter.
     * Calculates pvp and pk scores, karma gain, item drops
     */
    private suspend fun manageRewardForKillingPlayer(
        killer: PlayerCharacterInstanceImpl, killed: PlayerCharacterInstanceImpl
    ) {
        if (killed.pvpState != PvpState.NOT_IN_PVP || killed.karma > 0) {
            killer.pvpCount++
            log.debug { "Updated PVP score of '$killer': '${killer.pvpCount}'" }
        } else {
            //Apply karma points for killing player, not greater than karmaMaxAmount
            val newKarma = minOf(killer.karma + calculateKarmaGainForKillingPlayer(killer, killed), karmaMaxAmount)
            killer.karma = newKarma
            killer.pkCount++

            log.debug { "Updated PK state of '$killer': PK score = '${killer.pkCount}', Karma = '${killer.karma}'" }
            broadcastAround(killer.position) { PvPStatusResponse(killer) }
        }

        sendTo(killer.id) { FullCharacterResponse(killer) }
    }

    /**
     * Calculates item drops
     */
    private suspend fun manageItemRewards(killed: NpcInstance) {
        val mostValuableDamager = killed.opponents.maxBy { (_, damage) -> damage }.key
        if (mostValuableDamager is NpcInstanceImpl) return

        killed.reward?.itemGroups?.forEach { (chance, items) ->
            if (!isLvlDifferenceDropPenaltyApplied(killed.level, mostValuableDamager.level))
                withChance(chance) { itemService.dropRewardItem(items.random(), killed) }
        }
    }

    /**
     * Calculates exp and sp gain for all the attackers by level difference and damage dealt, and applies it to killer
     */
    private suspend fun manageExpAndSpGain(killer: PlayerCharacterInstance, killed: NpcInstance) {
        val allTheDamageReceived = killed.opponents.values.reduce { acc, i -> acc + i }

        for ((attacker: ActorInstance, damage: Int) in killed.opponents) {
            //TODO Manage damage dealt by pets and summons
            //TODO Share reward between party members
            //TODO Manage sp share between parties and solo players, who hit this monster

            // Monsters do not get exp/sp for monster hunt
            if (attacker !is PlayerCharacterInstanceImpl) continue
            if (!attacker.position.isCloseTo(killed.position, VISION_RANGE)) continue

            val killerLevel = attacker.level

            //TODO Manage exp gain of pets
            var expShare = (((killed.reward?.exp?.toDouble() ?: 0.0) * damage) / allTheDamageReceived)
            var spShare = (((killed.reward?.sp?.toDouble() ?: 0.0) * damage) / allTheDamageReceived)

            if (killerLevel - killed.level > minLevelDifferenceForPenalty) {
                val levelDifferenceModifier = (5.0 / 6.0).pow(killerLevel - killed.level - minLevelDifferenceForPenalty)

                expShare = maxOf(0.0, expShare * levelDifferenceModifier)
                spShare = maxOf(0.0, spShare * levelDifferenceModifier)
            }

            val overhitExp = if (attacker == killer && killed.overhitDamage > 0) {
                val killedMaxHp = killed.stats.maxHp.roundToInt()
                calculateOverhitExp(expShare.roundToInt(), killed.overhitDamage, killedMaxHp)
            }
            else 0

            giveExpAndSp(attacker, expShare.roundToInt(), overhitExp, spShare.roundToInt())
        }
    }

    /**
     * Calculates how much karma points must be subtracted when player killer kills monster
     */
    private fun calculateKarmaLossForExp(expGain: Int) = maxOf(expGain / karmaExpDivider, karmaLostMin)

    /**
     * Calculate karma amount that player killer must get
     */
    private suspend fun calculateKarmaGainForKillingPlayer(
        killer: PlayerCharacterInstance, killed: PlayerCharacterInstance
    ): Int {
        val pkCountMultiplier = maxOf(1.0, killer.pkCount / 2.0)
        val levelMultiplier = maxOf(1.0, (killer.level / killed.level).toDouble())

        val karmaGain = (karmaBaseAmount * pkCountMultiplier * levelMultiplier).roundToInt()
        log.debug { "Calculated '$karmaGain' karma gain of '$killer' for killing '$killed'" }

        return karmaGain
    }

    /**
     * Calculates is penalty applied for killer and killed level difference
     *
     * Drop penalty chance is
     * [levelPenaltyBaseValue] * ([minLevelDifferenceForPenalty] - ([killerLevel] - [killedLevel])), or
     * [levelPenaltyBaseValue] * ([maxLevelDifferenceForPenalty] - [minLevelDifferenceForPenalty])
     * if calculated level difference is greater than [maxLevelDifferenceForPenalty]
     *
     * @return true if penalty applied, false - not
     */
    private fun isLvlDifferenceDropPenaltyApplied(killedLevel: Int, killerLevel: Int): Boolean {
        val levelDifference = killerLevel - killedLevel

        val levelDifferenceMultiplier =
            minOf(levelDifference, maxLevelDifferenceForPenalty - minLevelDifferenceForPenalty)

        val penaltyChance = if (levelDifference > minLevelDifferenceForPenalty)
            levelPenaltyBaseValue * levelDifferenceMultiplier
        else return false

        return Random.nextDouble() < penaltyChance
    }


    private suspend fun handleLevelUp(character: PlayerCharacterInstanceImpl) {
        //Full heal on level up
        character.currentCp = character.stats.maxCp.roundToInt()
        character.currentHp = character.stats.maxHp.roundToInt()
        character.currentMp = character.stats.maxMp.roundToInt()

        sendTo(character.id) { UpdateStatusResponse.currentHpMpCpOf(character) }
        sendTo(character.id) { SystemMessageResponse.YourLevelHasIncreased }
        broadcastAround(character.position) { SocialActionResponse(character.id, SocialAction.LEVEL_UP) }

        //TODO Manage new skills
        //TODO Manage weight
        //TODO Manage grade penalty
    }

    /** Get the overhit exp bonus according to the above over-hit damage percentage */
    private fun calculateOverhitExp(expGain: Int, overhitDamage: Int, killedMaxHp: Int): Int {
        return (minOf(overhitDamage.toDouble() / killedMaxHp, 0.25) * expGain).roundToInt()
    }

}
