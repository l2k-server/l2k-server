# L2K SERVER
L2K is open-source server emulator of one famous korean MMORPG. Now in Kotlin! 

## Local launch

Both servers use Testcontainers to run required dependencies, so Docker must be installed and running.

### On Linux / macOS

#### 1. Launch Login Server

```bash
./gradlew launchTestLoginServer
```

#### 2. Launch Game Server

```bash
./gradlew launchTestGameServer
```

### On Windows

#### 1. Launch Login Server

```bat
gradlew launchTestLoginServer
```

#### 2. Launch Game Server

```bat
gradlew launchTestGameServer
```
