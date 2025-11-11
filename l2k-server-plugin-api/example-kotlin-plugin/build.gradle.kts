plugins {
    id("jvm-toolchains")
    kotlin("jvm")
    id("com.google.devtools.ksp") version "2.2.20-2.0.4"
}

group = "org.l2kserver"
version = "0.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":l2k-server-plugin-api"))
    implementation(project(":l2k-server-game-model"))

    ksp(project(":l2k-server-plugin-api"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}