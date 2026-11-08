group = "org.l2kserver"
version = "0.0.0"

plugins {
    kotlin("jvm") apply false
}

tasks.register<Exec>("launchDevLoginServer") {
    dependsOn(":l2k-server-login:bootTestRun")
    group = "l2k"
    description = "Launch login server in test mode"
}

tasks.register<Exec>("launchDevGameServer") {
    dependsOn(":l2k-server-game:bootTestRun")
    group = "l2k"
    description = "Launch game server in test mode"
}