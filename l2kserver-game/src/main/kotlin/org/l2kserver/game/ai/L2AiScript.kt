package org.l2kserver.game.ai

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.model.actor.Npc
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromClassloader
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

private const val AI_SCRIPT_EXTENSION_NAME = "l2ai.kts"

@KotlinScript(fileExtension = AI_SCRIPT_EXTENSION_NAME)
abstract class L2AiScript {

    companion object {
        private val aiScriptsDirectory = File("./data/ai")
        private val aiMap = ConcurrentHashMap<String, L2AiScript>()
        private val log = logger()

        init {
            scanDirectory(aiScriptsDirectory)
        }

        private fun scanDirectory(directory: File) {
            log.debug("Scanning {}...", directory.path)

            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) (scanDirectory(file))
                else if (file.name.endsWith(".l2ai.kts")) {
                    val aiName = file.name.substring(0, file.name.length - AI_SCRIPT_EXTENSION_NAME.length - 1)
                    aiMap[aiName] = evaluateAiScript(file)
                    log.info("Loaded ai: '{}'", aiName)
                }
            }
        }

        private fun evaluateAiScript(scriptFile: File): L2AiScript {
            val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<L2AiScript> {
                jvm {
                    dependenciesFromClassloader(classLoader = L2AiScript::class.java.classLoader, wholeClasspath = true)
                }
            }

            return BasicJvmScriptingHost()
                .eval(scriptFile.toScriptSource(), compilationConfiguration, null)
                .valueOrThrow()
                .returnValue
                .scriptInstance as L2AiScript
        }

        fun get(aiName: String): L2AiScript = requireNotNull(aiMap[aiName]) { "No AI found by name='$aiName'" }
    }

    var onIdleAction: (AiIntents.(it: Npc) -> Unit)? = null

    @Suppress("unused")
    fun onIdle(block: AiIntents.(it: Npc) -> Unit) {
        onIdleAction = block
    }

}
