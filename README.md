# Nebula UI (Fabric, Minecraft 1.21.11)

Dark, glowing, animated dropdown menu. **Press Right Shift (in-game, title screen or pause menu)** (while in a world, no other screen open) to open it.
Change the key in Options > Controls > Nebula UI.

## Requirements
- Java 21 (JDK)
- Gradle 8.14+ (or IntelliJ IDEA, which handles it)
- Fabric Loader 0.18.1+ and Fabric API for 1.21.11 installed in your launcher/mods folder

## Build the .jar
    gradle wrapper        # once, if there's no gradlew yet
    ./gradlew build       # Windows: gradlew.bat build

Your jar: `build/libs/nebula-ui-fabric-1.0.0+mc1.21.11.jar` (not the -sources jar). Drop it into `.minecraft/mods`.
