package org.l2kserver.game.service

import org.l2kserver.game.extensions.logger
import org.l2kserver.game.utils.PluginLoader
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

/**
 * Service to load and handle plugins
 */
@Service
class PluginService(
    private val pluginLoader: PluginLoader
) {

    private val log = logger()

    @EventListener(ApplicationStartedEvent::class)
    fun invokePlugins() {
        log.debug("Start launching plugins")
        pluginLoader.plugins.forEach {
            it.invoke()
        }

        log.info("Applied {} plugins", pluginLoader.plugins.size)
    }
}
