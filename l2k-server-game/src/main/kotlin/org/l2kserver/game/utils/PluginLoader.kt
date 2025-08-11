package org.l2kserver.game.utils

import org.l2kserver.game.extensions.logger
import org.l2kserver.plugin.api.L2kGameServerPlugin
import org.springframework.stereotype.Component
import java.io.File
import java.net.URLClassLoader
import java.util.ServiceLoader

private const val PLUGIN_DIR = "plugins"

/**
 * Loads and stores plugins
 */
@Component
object PluginLoader {

    private val log = logger()

    val plugins: List<L2kGameServerPlugin>

    init {
        val pluginsDirectory = File(PLUGIN_DIR)
        if (pluginsDirectory.exists() && pluginsDirectory.isDirectory) {
            this.plugins = pluginsDirectory
                .listFiles { file -> file.isFile && file.name.endsWith(".jar") }
                ?.flatMap(this::loadFile) ?: emptyList()

            log.info("Loaded {} plugins", this.plugins.size )
        }
        else {
            log.warn("No plugin directory exists")
            this.plugins = emptyList()
        }
    }

    private fun loadFile(file: File): Iterable<L2kGameServerPlugin> = try {
        URLClassLoader(arrayOf(file.toURI().toURL()), PluginLoader::class.java.getClassLoader()).use { loader ->
            ServiceLoader.load(L2kGameServerPlugin::class.java, loader)
        }
    }
    catch (e: Throwable) {
        log.error("An error occurred while loading plugin ${file.name}", e)
        emptyList()
    }

}
