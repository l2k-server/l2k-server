package org.l2kserver.game.model

enum class SocialAction(val id: Int) {
    GREETING(2),
    VICTORY(3),
    ADVANCE(4),
    YES(5),
    NO(6),
    BOW(7),
    UNAWARE(8),
    WAITING(9),
    LAUGH(10),
    APPLAUD(11),
    DANCE(12),
    SORROW(13),
    LEVEL_UP(15); // Yeah, it is social action too

    companion object {
        fun byId(id: Int) = requireNotNull(entries.find { it.id == id }) { "Invalid social action id '$id'" }
    }
}
