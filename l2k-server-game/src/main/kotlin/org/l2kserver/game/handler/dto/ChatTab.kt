package org.l2kserver.game.handler.dto

/**
 * @property GENERAL Tell to all actors around
 * @property SHOUT Shout to all actors around (the same as [GENERAL] but with greater range
 * @property WHISPER Private message
 * @property PARTY Party chat
 * @property CLAN Clan chat
 * @property GM Gm chat //TODO visible to all the players when speaker is GM on GM private chat?
 * @property PETITION_PLAYER //TODO what is it?
 * @property PETITION_GM //TODO what is it?
 * @property TRADE Trade chat
 * @property ALLIANCE Alliance chat
 * @property ANNOUNCEMENT
 */
enum class ChatTab(val id: Int) {
    GENERAL(id = 0),
    SHOUT(id = 1),
    WHISPER(id = 2),
    PARTY(id = 3),
    CLAN(id = 4),
    GM(id = 5),
    PETITION_PLAYER(id = 6),
    PETITION_GM(id = 7),
    TRADE(id = 8),
    ALLIANCE(id = 9),
    ANNOUNCEMENT(id = 10),
    PARTY_ROOM_COMMANDER(id = 15),
    PARTY_ROOM(id = 16),
    HERO(id = 17);

    companion object {
        fun byId(id: Int) = requireNotNull(entries.find { it.id == id }) {
            "No ChatTab found by id='$id'"
        }
    }
}
