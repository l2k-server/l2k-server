package org.l2kserver.game.service

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.l2kserver.game.AbstractTests
import org.l2kserver.game.extensions.pullResponse
import org.l2kserver.game.handler.dto.response.ArrivedResponse
import org.l2kserver.game.handler.dto.response.StartMovingResponse
import org.l2kserver.game.handler.dto.response.TeleportResponse
import org.l2kserver.game.model.actor.DestinationPoint
import org.l2kserver.game.model.actor.Intention
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.network.session.sessionContextOf
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class MoveServiceTests @Autowired constructor(
    private val moveService: MoveService
): AbstractTests() {

    @Test
    fun shouldTeleportPlayer(): Unit = runBlocking {
        val otherCharacter = createTestCharacter(name = "MrWitness")
        val otherContext = sessionContextOf(otherCharacter.id)!!

        val character = createTestCharacter()
        val context = sessionContextOf(character.id)!!

        character.targetId = otherCharacter.id
        // Character is doing smth
        character.intentionQueue.enqueue(
            Intention.Move(DestinationPoint(Position(-71533, 258203, -3109)))
        )

        //wait a bit to let server move our character
        delay(100.milliseconds)

        //Teleport!
        val teleportPosition = Position(-114462, -249619, -2984) //GM consultation
        moveService.teleport(character, teleportPosition)

        //Wait a bit before teleportation completes
        delay(100.milliseconds)

        //Check responses of teleported character
        val startMovingResponse = assertIs<StartMovingResponse>(context.pullResponse())
        assertEquals(character, startMovingResponse.actor)

        // Character moving was canceled, server must send ArrivedResponse
        val arrivedResponse = assertIs<ArrivedResponse>(context.pullResponse())
        assertEquals(character.id, arrivedResponse.actorId)

        val teleportResponse = assertIs<TeleportResponse>(context.pullResponse())
        assertEquals(character.id, teleportResponse.actorId)
        assertEquals(teleportPosition, teleportResponse.position)
        assertEquals(teleportPosition, character.position)

        //Check responses of second character - at the starting position
        val startMovingResponseForOther = assertIs<StartMovingResponse>(otherContext.pullResponse())
        assertEquals(character, startMovingResponseForOther.actor)

        // Character moving was canceled, server must send ArrivedResponse
        val arrivedResponseForOther = assertIs<ArrivedResponse>(otherContext.pullResponse())
        assertEquals(character.id, arrivedResponseForOther.actorId)

        //Check character's state
        assertTrue(character.intentionQueue.isEmpty())
    }

}
