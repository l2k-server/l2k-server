pluginManagement {
    plugins {
        kotlin("jvm") version "2.3.21"
        kotlin("plugin.spring") version "2.3.21"
        id("dev.detekt") version "2.0.0-alpha.3"
        id("org.springframework.boot") version "4.0.6"
        id("io.spring.dependency-management") version "1.1.7"
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("ktor", "3.4.3")
            version("exposed", "1.3.0")
            version("hazelcast", "5.7.0")

            library(
                "ktor-network-jvm",
                "io.ktor",
                "ktor-network-jvm"
            ).versionRef("ktor")

            library(
                "exposed-spring-boot4-starter",
                "org.jetbrains.exposed",
                "exposed-spring-boot4-starter"
            ).versionRef("exposed")
            library(
                "exposed-java-time",
                "org.jetbrains.exposed",
                "exposed-java-time"
            ).versionRef("exposed")

            library(
                "hazelcast",
                "com.hazelcast",
                "hazelcast"
            ).versionRef("hazelcast")
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "l2k-server"

include("l2k-server-game")
include("l2k-server-login")
include("l2k-server-game-model")
include("l2k-server-plugin-api")

include("l2k-server-plugin-api:example-java-plugin")
findProject(":l2k-server-plugin-api:example-java-plugin")?.name = "example-java-plugin"
include("l2k-server-plugin-api:example-kotlin-plugin")
findProject(":l2k-server-plugin-api:example-kotlin-plugin")?.name = "example-kotlin-plugin"
