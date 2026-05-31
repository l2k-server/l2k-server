package org.l2kserver.game.model

enum class SocialAction(val id: Int) {
    GREETING(id = 2),
    VICTORY(id = 3),
    ADVANCE(id = 4),
    NO(id = 5),
    YES(id = 6),
    BOW(id = 7),
    UNAWARE(id = 8),
    WAITING(id = 9),
    LAUGH(id = 10),
    APPLAUD(id = 11),
    DANCE(id = 12),
    SORROW(id = 13),
    LEVEL_UP(id = 15); // Yeah, it is social action too

    companion object {
        fun byId(id: Int) = requireNotNull(entries.find { it.id == id }) { "Invalid social action id '$id'" }
    }
}
