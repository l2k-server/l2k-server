plugins {
    id("jvm-toolchains")
    kotlin("jvm")
    id("dev.detekt")
}

group = "org.l2kserver"
version = "0.0.0"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(project(":l2k-server-game-model"))
    implementation("com.google.devtools.ksp:symbol-processing-api:2.3.5")

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
