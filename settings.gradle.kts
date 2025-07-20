plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "l2kserver"

include("l2kserver-game")
include("l2kserver-login")
