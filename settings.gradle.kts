pluginManagement {
    plugins {
        kotlin("jvm") version "2.0.21"
        kotlin("plugin.spring") version "2.0.21"
        id("io.gitlab.arturbosch.detekt") version "1.23.8"
        id("org.springframework.boot") version "3.5.4"
        id("io.spring.dependency-management") version "1.1.7"
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
