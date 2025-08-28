package org.l2kserver.game.service

import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.handler.dto.response.FullCharacterResponse
import org.l2kserver.game.handler.dto.response.PvPStatusResponse
import org.l2kserver.game.handler.dto.response.SocialActionResponse
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.model.SocialAction
import org.l2kserver.game.model.actor.Actor
import org.l2kserver.game.model.actor.Npc
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.actor.character.PvpState
import org.l2kserver.game.network.session.sendTo
import org.l2kserver.game.repository.GameObjectRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.random.Random

/** Service for rewards calculation and management */
@Service
class RewardService(
    private val itemService: ItemService,
    override val gameObjectRepository: GameObjectRepository,

    @Value("\${pvp.karmaBaseAmount}") private val karmaBaseAmount: Int,
    @Value("\${pvp.karmaMaxAmount}") private val karmaMaxAmount: Int,
    @Value("\${pvp.karmaExpDivider}") private val karmaExpDivider: Int,
    @Value("\${pvp.karmaLostMin}") private val karmaLostMin: Int,
    @Value("\${reward.minLevelDifferenceForPenalty}") private val minLevelDifferenceForPenalty: Int,
    @Value("\${reward.maxLevelDifferenceForPenalty}") private val maxLevelDifferenceForPenalty: Int,
    @Value("\${reward.levelPenaltyBaseValue}") private val levelPenaltyBaseValue: Double
) : AbstractService() {

    override val log = logger()

    /**
     * Manages rewards for killing NPC.
     * Calculates exp, sp, item drops, distributes the reward among the players
     */
    suspend fun manageRewardForKillingNpc(killer: PlayerCharacter, killed: Npc, overhitDamage: Int) {
        manageItemRewards(killed)
        manageExpAndSpGain(killer, killed, overhitDamage)
    }

    /**
     * Manages rewards for killing PlayerCharacter.
     * Calculates pvp and pk scores, karma gain, item drops
     */
    suspend fun manageRewardForKillingPlayer(killed: PlayerCharacter, killer: PlayerCharacter) {

        if (killed.pvpState != PvpState.NOT_IN_PVP || killed.karma > 0) {
            killer.pvpCount++
            log.debug("Updated PVP score of '{}': '{}", killer, killer.pvpCount)
        } else {
            //Apply karma points for killing player, not greater than karmaMaxAmount
            val newKarma = minOf(killer.karma + calculateKarmaGainForKillingPlayer(killer, killed), karmaMaxAmount)
            killer.karma = newKarma
            killer.pkCount++

            log.debug("Updated PK state of '{}': PK score = '{}', Karma = '{}", killer, killer.pkCount, killer.karma)
        }

        if (killed.karma != 0) {
            killed.karma = 0

            log.debug("{} was killed, decreased his karma to '{}'", killed, killed.karma)

            sendTo(killed.id, FullCharacterResponse(killed))
            broadcastPacket(PvPStatusResponse(killed), killed.position)
        }

        sendTo(killer.id, FullCharacterResponse(killer))
        broadcastPacket(PvPStatusResponse(killer), killer.position)
    }

    /**
     * Calculates item drops
     */
    private suspend fun manageItemRewards(killed: Npc) {
        val mostValuableDamager = killed.opponentsDamage.maxBy { (_, damage) -> damage }.key
        if (mostValuableDamager is Npc) return

        for (itemGroup in killed.reward.itemGroups) {
            if (isLvlDifferenceDropPenaltyApplied(killed.level, mostValuableDamager.level)) continue
            if (Random.nextDouble() < itemGroup.chance) itemService.dropRewardItem(itemGroup.items.random(), killed)
        }
    }

    /**
     * Calculates exp and sp gain for all the attackers by level difference and damage dealt, and applies it to killer
     */
    private suspend fun manageExpAndSpGain(killer: PlayerCharacter, killed: Npc, overhitDamage: Int) {
        val allTheDamageReceived = killed.opponentsDamage.values.reduce { acc, i -> acc + i }

        for ((attacker: Actor, damage: Int) in killed.opponentsDamage) {
            //TODO Manage damage dealt by pets and summons
            //TODO Share reward between party members
            //TODO Manage sp share between parties and solo players, who hit this monster

            // Monsters do not get exp/sp for monster hunt
            if (attacker !is PlayerCharacter) continue
            if (!attacker.position.isCloseTo(killed.position, VISION_RANGE)) continue

            val killerLevel = attacker.level

            //TODO Manage exp gain of pets
            var expShare = ((killed.reward.exp.toDouble() * damage) / allTheDamageReceived)
            var spShare = ((killed.reward.sp.toDouble() * damage) / allTheDamageReceived)

            if (killerLevel - killed.level > minLevelDifferenceForPenalty) {
                val levelDifferenceModifier = (5.0 / 6.0).pow(killerLevel - killed.level - minLevelDifferenceForPenalty)

                expShare = maxOf(0.0, expShare * levelDifferenceModifier)
                spShare = maxOf(0.0, spShare * levelDifferenceModifier)
            }

            val overhitExp = if (attacker == killer && overhitDamage > 0)
                calculateOverhitExp(expShare.roundToInt(), overhitDamage, killed.stats.maxHp)
            else 0

            newSuspendedTransaction {
                attacker.exp += (expShare.roundToLong() + overhitExp)
                attacker.sp += spShare.roundToInt()

                if (attacker.karma > 0)
                    attacker.karma = maxOf(attacker.karma - calculateKarmaLossForExp(expShare.roundToInt()), 0)

                sendTo(attacker.id,
                    SystemMessageResponse.YouHaveEarnedExpAndSp(expShare.roundToInt(), spShare.roundToInt())
                )

                if (overhitExp > 0) sendTo(attacker.id,
                    SystemMessageResponse.YouHaveAcquiredExpForOverHit(overhitExp))

                sendTo(attacker.id, FullCharacterResponse(attacker))
                if (attacker.level > killerLevel) handleLevelUp(attacker)
            }
        }
    }

    /**
     * Calculates how much karma points must be subtracted when player killer kills monster
     */
    private fun calculateKarmaLossForExp(expGain: Int) = maxOf(expGain / karmaExpDivider, karmaLostMin)

    /**
     * Calculate karma amount that player killer must get
     */
    private suspend fun calculateKarmaGainForKillingPlayer(killer: PlayerCharacter, killed: PlayerCharacter): Int {
        val pkCountMultiplier = maxOf(1.0, killer.pkCount / 2.0)
        val levelMultiplier = maxOf(1.0, (killer.level / killed.level).toDouble())

        val karmaGain = (karmaBaseAmount * pkCountMultiplier * levelMultiplier).roundToInt()
        log.debug("Calculated '{}' karma gain of '{}' for killing '{}'", karmaGain, killer, killed)

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


    private suspend fun handleLevelUp(character: PlayerCharacter) {
        //Full heal on level up
        character.currentCp = character.stats.maxCp
        character.currentHp = character.stats.maxHp
        character.currentMp = character.stats.maxMp

        sendTo(character.id, UpdateStatusResponse.hpMpCpOf(character))
        sendTo(character.id, SystemMessageResponse.YourLevelHasIncreased)
        broadcastPacket(SocialActionResponse(character.id, SocialAction.LEVEL_UP), character.position)

        //TODO Manage weight
        //TODO Manage grade penalty
    }

    /** Get the overhit exp bonus according to the above over-hit damage percentage */
    fun calculateOverhitExp(expGain: Int, overhitDamage: Int, killedMaxHp: Int): Int {
        return (minOf(overhitDamage.toDouble() / killedMaxHp, 0.25) * expGain).roundToInt()
    }

}
