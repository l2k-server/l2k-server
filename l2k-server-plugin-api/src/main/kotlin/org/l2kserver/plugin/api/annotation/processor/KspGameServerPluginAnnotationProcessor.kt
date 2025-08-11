package org.l2kserver.plugin.api.annotation.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import org.l2kserver.plugin.api.L2kGameServerPlugin
import org.l2kserver.plugin.api.annotation.GameServerPlugin
import kotlin.io.use
import kotlin.sequences.filterIsInstance
import kotlin.sequences.forEach
import kotlin.text.toByteArray

class KspGameServerPluginAnnotationProcessor(
    private val codeGenerator: CodeGenerator
): SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(GameServerPlugin::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()

        symbols.forEach { klass ->
            val className = klass.qualifiedName?.asString() ?: "Unknown"

            codeGenerator.createNewFile(
                dependencies = Dependencies(false),
                packageName = "",
                fileName = "META-INF/services/${L2kGameServerPlugin::class.qualifiedName}",
                extensionName = ""
            ).use { out -> out.write(className.toByteArray()) }
        }

        return emptyList()
    }

}

class KspGameServerPluginAnnotationProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return KspGameServerPluginAnnotationProcessor(environment.codeGenerator)
    }

}
