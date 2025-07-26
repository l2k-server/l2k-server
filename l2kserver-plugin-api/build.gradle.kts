plugins {
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

group = "org.l2kserver"
version = "0.0.0"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.0-1.0.13")

    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
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
