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
    GENERAL(0),
    SHOUT(1),
    WHISPER(2),
    PARTY(3),
    CLAN(4),
    GM(5),
    PETITION_PLAYER(6),
    PETITION_GM(7),
    TRADE(8),
    ALLIANCE(9),
    ANNOUNCEMENT(10),
    PARTY_ROOM_COMMANDER(15),
    PARTY_ROOM(16),
    HERO(17);

    companion object {
        fun byId(id: Int) = requireNotNull(entries.find { it.id == id }) {
            "No ChatTab found by id='$id'"
        }
    }
}
