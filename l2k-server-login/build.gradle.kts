plugins {
    id("jvm-toolchains")
    kotlin("jvm")
    kotlin("plugin.spring")
    id("dev.detekt")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "org.l2kserver"
version = "0.0.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    // Spring
    implementation("org.springframework.boot:spring-boot-starter")

    // Ktor
    implementation("io.ktor:ktor-network-jvm:3.4.3")

    //Jackson
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // L2J
    implementation("org.bitbucket.l2jserver:l2j-server-commons:2.6.6.1")

    //Hazelcast
    implementation("com.hazelcast:hazelcast:5.7.0")

    // Database
    implementation("org.jetbrains.exposed:exposed-spring-boot4-starter:1.2.0")
    implementation("org.jetbrains.exposed:exposed-java-time:1.2.0")
    runtimeOnly("org.postgresql:postgresql")

    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:12.6.0")

    // Test
    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql:1.21.4")
}

tasks.test {
    useJUnitPlatform()
}

detekt {
    config.setFrom(rootDir.resolve("detekt.yml"))
}

kotlin {
    jvmToolchain(21)
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
