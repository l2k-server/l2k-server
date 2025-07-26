package org.l2kserver.example.kotlin.plugin

import org.l2kserver.plugin.api.L2kGameServerPlugin
import org.l2kserver.plugin.api.annotation.GameServerPlugin

@GameServerPlugin("Example plugin, written in Kotlin")
class ExampleKotlinPlugin: L2kGameServerPlugin {

    override fun invoke() {
        println("At the moment i am very little and simple plugin, so i can only say 'Hello' =( ")
        println("Hello!")
    }

}
