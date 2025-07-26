package org.l2kserver.plugin.api

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

const val JAVA_PLUGIN_NAME = ":l2kserver-plugin-api:example-java-plugin"
const val JAVA_PLUGIN_SERVICE_CLASS_NAME = "org.l2kserver.example.java.plugin.ExampleJavaPlugin"
const val JAVA_SERVICE_FILE_PATH = "classes/java/main/META-INF/" +
        "services/org.l2kserver.plugin.api.L2kGameServerPlugin"

const val KOTLIN_PLUGIN_NAME = ":l2kserver-plugin-api:example-kotlin-plugin"
const val KOTLIN_PLUGIN_SERVICE_CLASS_NAME = "org.l2kserver.example.kotlin.plugin.ExampleKotlinPlugin"
const val KOTLIN_SERVICE_FILE_PATH = "generated/ksp/main/resources/META-INF/" +
        "services/org.l2kserver.plugin.api.L2kGameServerPlugin"

class AnnotationProcessorTests {

    @BeforeEach
    @AfterEach
    fun init() {
        // Clear build directory
        GradleRunner.create()
            .withProjectDir(File("."))
            .withArguments("$JAVA_PLUGIN_NAME:clean")
            .build()

        GradleRunner.create()
            .withProjectDir(File("."))
            .withArguments("$KOTLIN_PLUGIN_NAME:clean")
            .build()
    }

    @Test
    fun shouldFillJavaPluginMetaInf() {
        val buildDir = File("example-java-plugin/build")

        // Build test plugin
        GradleRunner.create()
            .withProjectDir(File("."))
            .withArguments("$JAVA_PLUGIN_NAME:build")
            .build()

        val serviceFile = File(buildDir, JAVA_SERVICE_FILE_PATH)
        assertTrue(serviceFile.exists(), "Service file not generated")
        assertEquals(JAVA_PLUGIN_SERVICE_CLASS_NAME, serviceFile.readText())
    }

    @Test
    fun shouldFillKotlinPluginMetaInf() {
        val buildDir = File("example-kotlin-plugin/build")

        // Build test plugin
        GradleRunner.create()
            .withProjectDir(File("."))
            .withArguments("$KOTLIN_PLUGIN_NAME:build")
            .build()

        val serviceFile = File(buildDir, KOTLIN_SERVICE_FILE_PATH)
        assertTrue(serviceFile.exists(), "Service file not generated")
        assertEquals(KOTLIN_PLUGIN_SERVICE_CLASS_NAME, serviceFile.readText())

    }

}
