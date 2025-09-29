package org.l2kserver.game.service

import org.l2kserver.game.handler.dto.request.BypassToServerRequest
import org.l2kserver.game.handler.dto.request.LinkRequest
import org.l2kserver.game.handler.dto.response.NpcChatWindowResponse
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.model.html.HtmlRegistry
import org.l2kserver.game.network.session.send
import org.springframework.stereotype.Service

/** Service to handle commands, bypassed to server by npc chat windows */
@Service
class HtmlCommandService {

    suspend fun handleLinkRequest(request: LinkRequest) {
        val link = request.link.split("#").getOrNull(0)
        require(!link.isNullOrBlank()) { "Link must not be empty!" }
        val text = runCatching { HtmlRegistry.findById(link) }
            .getOrElse { "<html><body>${it.message}</body></html>" }

        send { NpcChatWindowResponse(1, text) }
    }

    suspend fun handleBypassRequest(request: BypassToServerRequest) {
        send { SystemMessageResponse(request.bypassCommandString) }
    }
}
