# L2K SERVER
L2K is open-source server emulator of one famous korean MMORPG. Now in Kotlin!
At the moment Interlude version is being developed

## Requirements 

* Java 21
* Postgresql
* 2106 and 7777 ports opened
* Interlude client

## Local (DEV) launch

Both servers use Testcontainers for test launch, so Docker must be installed and running.

#### On Linux / macOS

##### 1. Launch Login Server

```bash
./gradlew launchTestLoginServer
```

##### 2. Launch Game Server

```bash
./gradlew launchTestGameServer
```

#### On Windows

##### 1. Launch Login Server

```bat
gradlew launchTestLoginServer
```

##### 2. Launch Game Server

```bat
gradlew launchTestGameServer
```

Login server will start at 2106 port
To log into the game use login "admin" and password "admin" :3
