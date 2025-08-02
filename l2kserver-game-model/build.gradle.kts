plugins {
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

group = "org.l2kserver"
version = "0.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.17")
    testImplementation(kotlin("test"))
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