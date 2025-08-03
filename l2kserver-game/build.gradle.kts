import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm")
    kotlin("plugin.spring") version "2.0.21"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "org.l2kserver"
version = "0.0.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    // Spring
    implementation("org.springframework.boot:spring-boot-starter")

    // Ktor
    implementation("io.ktor:ktor-network:3.2.2")

    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.10.2")

    // Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    
    // Command parser
    implementation("com.github.ajalt.clikt:clikt-jvm:5.0.3")

    // Database
    implementation("org.jetbrains.exposed:exposed-spring-boot-starter:0.61.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.61.0")
    implementation("org.postgresql:postgresql")

    implementation("org.flywaydb:flyway-core:11.10.4")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:11.10.4")

    // Hazelcast (for sharing data with LoginServer)
    implementation("com.hazelcast:hazelcast:5.5.0")

    // L2J
    implementation("org.bitbucket.l2jserver:l2j-server-geo-driver:2.6.4.1")

    // L2K
    implementation(project(":l2kserver-game-model"))
    implementation(project(":l2kserver-plugin-api"))

    // Test
    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql")

    testImplementation("org.awaitility:awaitility:4.3.0")
    testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
}

//Geodata is too big for default JUnit heap space
tasks.test {
    useJUnitPlatform()
    minHeapSize = "512m"
    maxHeapSize = "1024m"
    testLogging.showStandardStreams = true
}

detekt {
    config.setFrom(rootDir.resolve("detekt.yml"))
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjsr305=strict")
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}

// Disable creating plain jar
tasks.getByName<Jar>("jar") { enabled = false }

tasks.register<Zip>("packDistribution") {
    dependsOn(":build")
    group = "l2k"
    description = "Pack distribution to zip archive"

    archiveFileName.set("${project.name}-${project.version}.zip")
    destinationDirectory.set(layout.buildDirectory)

    from(layout.buildDirectory.dir("libs")) { into("/") }
    from(layout.projectDirectory.dir("config")) { into("/config") }
    from(layout.projectDirectory.dir("geodata")) { into("/geodata") }
    from(layout.projectDirectory.dir("plugins")) { into("/plugins") }
}
