package org.l2kserver.game.service

import org.jetbrains.exposed.v1.jdbc.select
import org.l2kserver.game.domain.PlayerCharacterTable
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.BitSet

private const val FIRST_ID = 268435456

@Service
class IdGenerationService {
    // 0 - available, 1 - acquired
    private val freeIds = BitSet()

    @EventListener(ApplicationStartedEvent::class)
    @Transactional
    fun init() {
        PlayerCharacterTable.select(PlayerCharacterTable.id).forEach {
            freeIds.set(it[PlayerCharacterTable.id].value - FIRST_ID)
        }
    }

    /** Returns next available object ID */
    @Synchronized
    fun next(): Int {
        val id = freeIds.nextClearBit(0)

        check(id + FIRST_ID >= 0) { "Ran out of valid Id's." }
        freeIds.set(id)

        return id + FIRST_ID
    }

    /** Makes [id] available to acquire */
    @Synchronized
    fun release(id: Int) = freeIds.clear(id - FIRST_ID)
}
