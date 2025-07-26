plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "l2kserver"

include("l2kserver-game")
include("l2kserver-login")

include("l2kserver-plugin-api")
include("l2kserver-plugin-api:example-java-plugin")
findProject(":l2kserver-plugin-api:example-java-plugin")?.name = "example-java-plugin"
include("l2kserver-plugin-api:example-kotlin-plugin")
findProject(":l2kserver-plugin-api:example-kotlin-plugin")?.name = "example-kotlin-plugin"
