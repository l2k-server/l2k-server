package org.l2kserver.plugin.api.annotation

/**
 * @param pluginId Plugin identifier
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class GameServerPlugin(
    val pluginId: String
)
