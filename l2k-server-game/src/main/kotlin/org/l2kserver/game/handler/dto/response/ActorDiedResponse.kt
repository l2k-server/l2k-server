package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.domain.AccessLevel
import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.extensions.toInt
import org.l2kserver.game.model.actor.Npc
import org.l2kserver.game.model.actor.PlayerCharacter

private const val ACTOR_DIED_RESPONSE_PACKET_ID: UByte = 6u

/**
 * Notifies client about actor's death
 *
 * @property actorId Dead actor id
 * @property toVillage display "To village" respawn button
 * @property toClanHideout display "To hideout" respawn button
 * @property toClanCastle display "To castle" respawn button
 * @property toClanSiegeHeadquarters display "To headquarters" respawn button
 * @property isSweepable Can this actor be swept (blue glow)
 * @property toDeathPlace display "Fixed" respawn button
 */
data class ActorDiedResponse(
    val actorId: Int,
    val toVillage: Boolean,
    val toClanHideout: Boolean,
    val toClanCastle: Boolean,
    val toClanSiegeHeadquarters: Boolean,
    val toDeathPlace: Boolean,
    val isSweepable: Boolean
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(ACTOR_DIED_RESPONSE_PACKET_ID)
        putInt(actorId)
        putInt(toVillage.toInt())
        putInt(toClanHideout.toInt())
        putInt(toClanCastle.toInt())
        putInt(toClanSiegeHeadquarters.toInt())
        putInt(isSweepable.toInt())
        putInt(toDeathPlace.toInt())
    }

}

/**
 * Notifies client about NPC's death
 */
@Suppress("FunctionName")
fun NpcDiedResponse(npc: Npc) = ActorDiedResponse(
    actorId = npc.id,
    toVillage = false,
    toClanHideout = false,
    toClanCastle = false,
    toClanSiegeHeadquarters = false,
    toDeathPlace = false,
    isSweepable = false //TODO Spoil/Sweep
)

/**
 * Notifies client about player's death
 */
@Suppress("FunctionName")
fun PlayerDiedResponse(character: PlayerCharacter) = ActorDiedResponse(
    actorId = character.id,
    toVillage = true,
    toClanHideout = false, //TODO ClanHideout
    toClanCastle = false, // TODO ClanCastle
    toClanSiegeHeadquarters = false, //TODO Siege Headquarters
    toDeathPlace = character.accessLevel == AccessLevel.GAME_MASTER, //TODO Festival (??) or some events
    isSweepable = false
)
