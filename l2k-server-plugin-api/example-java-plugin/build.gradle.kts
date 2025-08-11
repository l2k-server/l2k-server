plugins {
    id("java")
}

group = "org.l2kserver"
version = "0.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":l2k-server-plugin-api"))
    implementation(project(":l2k-server-game-model"))

    annotationProcessor(project(":l2k-server-plugin-api"))
}

tasks.test {
    useJUnitPlatform()
}