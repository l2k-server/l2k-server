import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.0"
}

group = "org.l2kserver"
version = "0.0.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

extra["testcontainersVersion"] = "1.21.0"
dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}")
    }
}

dependencies {
    // Spring
    implementation("org.springframework.boot:spring-boot-starter")

    // Ktor
    implementation("io.ktor:ktor-network:3.1.2")

    //Jackson
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // L2J
    implementation("org.bitbucket.l2jserver:l2j-server-commons:2.6.6.1")

    //Hazelcast
    implementation("com.hazelcast:hazelcast:5.5.0")

    // Database
    implementation("org.jetbrains.exposed:exposed-spring-boot-starter:0.61.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.61.0")
    runtimeOnly("org.postgresql:postgresql")

    implementation("org.flywaydb:flyway-core:11.7.2")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:11.7.2")

    // Test
    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql")
}

tasks.test {
    useJUnitPlatform()
}

detekt {
    config.setFrom("detekt.yml")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjsr305=strict")
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}

// Disable creating plain jar
tasks.getByName<Jar>("jar") {
    enabled = false
}

tasks.register<Zip>("packDistribution") {
    dependsOn(":build")
    group = "l2k"
    description = "Pack distribution to zip archive"

    archiveFileName.set("${project.name}-${project.version}.zip")
    destinationDirectory.set(layout.buildDirectory)

    from(layout.buildDirectory.dir("libs")) { into("/") }
    from(layout.buildDirectory.dir("config")) { into("/config") }
}
