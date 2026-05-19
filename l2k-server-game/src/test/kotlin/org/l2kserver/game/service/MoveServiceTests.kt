package org.l2kserver.game.service

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.l2kserver.game.AbstractTests
import org.l2kserver.game.extensions.pullResponse
import org.l2kserver.game.handler.dto.response.ArrivedResponse
import org.l2kserver.game.handler.dto.response.CharacterInfoResponse
import org.l2kserver.game.handler.dto.response.DeleteObjectResponse
import org.l2kserver.game.handler.dto.response.SetTargetResponse
import org.l2kserver.game.handler.dto.response.StartMovingResponse
import org.l2kserver.game.handler.dto.response.TeleportResponse
import org.l2kserver.game.model.actor.DestinationPoint
import org.l2kserver.game.model.actor.Intention
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.network.session.sessionContextOf
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

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
        character.knownGameWorldObjects.add((otherCharacter))
        otherCharacter.knownGameWorldObjects.add(character)
        // Character is doing smth
        character.intentionQueue.enqueue(
            Intention.Move(DestinationPoint(Position(-71533, 258203, -3109)))
        )

        val teleportPosition = Position(-114462, -249619, -2984) //GM consultation

        val thirdCharacter = createTestCharacter(name = "MrGM")
        thirdCharacter.position = teleportPosition
        val thirdContext = sessionContextOf(thirdCharacter.id)!!

        moveService.teleport(character, teleportPosition)

        //Wait a bit before teleportation completes
        delay(100)

        //Check responses of teleported character
        val startMovingResponse = assertIs<StartMovingResponse>(context.pullResponse())
        assertEquals(character, startMovingResponse.actor)

        // Character moving was canceled, server must send ArrivedResponse
        val arrivedResponse = assertIs<ArrivedResponse>(context.pullResponse())
        assertEquals(character.id, arrivedResponse.actorId)

        val deleteObjectResponse = assertIs<DeleteObjectResponse>(context.pullResponse())
        assertEquals(otherCharacter.id, deleteObjectResponse.gameObjectId)

        val setTargetResponse = assertIs<SetTargetResponse>(context.pullResponse())
        assertEquals(0, setTargetResponse.targetId)
        assertEquals(0, setTargetResponse.levelDifference)

        val teleportResponse = assertIs<TeleportResponse>(context.pullResponse())
        assertEquals(character.id, teleportResponse.actorId)
        assertEquals(teleportPosition, teleportResponse.position)
        assertEquals(teleportPosition, character.position)

        val thirdCharacterInfo = assertIs<CharacterInfoResponse>(context.pullResponse())
        assertEquals(thirdCharacter, thirdCharacterInfo.character)

        //Check responses of second character - at the starting position
        val startMovingResponseForOther = assertIs<StartMovingResponse>(otherContext.pullResponse())
        assertEquals(character, startMovingResponseForOther.actor)

        // Character moving was canceled, server must send ArrivedResponse
        val arrivedResponseForOther = assertIs<ArrivedResponse>(otherContext.pullResponse())
        assertEquals(character.id, arrivedResponseForOther.actorId)

        val deleteTeleportedForOther = assertIs<DeleteObjectResponse>(otherContext.pullResponse())
        assertEquals(character.id, deleteTeleportedForOther.gameObjectId)

        //Check responses of third character - at the target position
        val characterResponseForThird = assertIs<CharacterInfoResponse>(thirdContext.pullResponse())
        assertEquals(character, characterResponseForThird.character)

        //Check character's state
        assertTrue(character.knownGameWorldObjects.contains(thirdCharacter))
        assertFalse(character.knownGameWorldObjects.contains(otherCharacter))
        assertTrue(character.intentionQueue.isEmpty())
    }

}
