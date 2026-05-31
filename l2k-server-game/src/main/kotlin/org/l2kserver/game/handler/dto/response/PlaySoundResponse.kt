package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.extensions.putUTF16String
import org.l2kserver.game.extensions.toInt

private const val PLAY_SOUND_RESPONSE_PACKET_ID: UByte = 152u

data class PlaySoundResponse(
    val sound: Sound
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(PLAY_SOUND_RESPONSE_PACKET_ID)
        putInt(sound.isMusic.toInt())
        putUTF16String(sound.fileName)
        putInt(0)
        putInt(0)
        putInt(0)
        putInt(0)
        putInt(0)
    }
}

/**
 * @property isMusic is this sound a music or not. Music files are stored at music directory
 * and disable ambient music of location
 */
@Suppress("SpellCheckingInspection")
enum class Sound(val isMusic: Boolean, val fileName: String) {

    /**
     * Played when condition check failed
     */
    ITEMSOUND_SYS_IMPOSSIBLE(false, "itemsound3.sys_impossible"),
    ITEMSOUND_SYS_SHORTAGE(false, "itemsound3.sys_shortage")
}
