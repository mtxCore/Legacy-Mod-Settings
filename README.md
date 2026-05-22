[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/H2H51ZR38L)

# Unified Legacy Settings

Unified Legacy Settings adds optional mod toggles directly to Legacy4J menu screens.

## Integrations

| Menu | Option | Source |
| --- | --- | --- |
| Graphics | Advanced Graphical Effects | [Iris](https://modrinth.com/mod/iris) |
| Advanced Graphics | Extended Online Render Distance | [Bobby](https://modrinth.com/mod/bobby) |
| Advanced Graphics | Grass Detail | [LambdaBetterGrass](https://modrinth.com/mod/lambdabettergrass) |
| Advanced Graphics | Snow Layer Blending | [LambdaBetterGrass](https://modrinth.com/mod/lambdabettergrass) |
| Advanced Graphics | Connected Textures | [Continuity](https://modrinth.com/mod/continuity) |
| Advanced Graphics | Emissive Textures | [Continuity](https://modrinth.com/mod/continuity) |
| Advanced Graphics | Third-Person Animations | [Not Enough Animations](https://modrinth.com/mod/not-enough-animations) |
| Game Options | Dynamic Lighting | [LambDynamicLights](https://modrinth.com/mod/lambdynamiclights) |
| Game Options | Minimap | [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) |
| Advanced Game Options | Zoom | [Zoomify](https://modrinth.com/mod/zoomify) |
| Advanced Game Options | Locator Compass | [Locator Lodestones](https://modrinth.com/mod/locator-lodestones) |
| Audio | Presence Footsteps | [Presence Footsteps](https://modrinth.com/mod/presence-footsteps) |
| Advanced User Interface | Chat Portraits | [Chat Heads](https://modrinth.com/mod/chat-heads) |
| Advanced User Interface | Take Screenshot on Achievement | [Advancement Screenshot](https://modrinth.com/mod/advancement-screenshot) |

## Configuration

Open Mod Menu -> Unified Legacy Settings -> Config.
You can also open it from the Unified Legacy Settings entry in Legacy4J's
Advanced User Interface screen.
Settings are written to `config/unified_legacy_settings.json`.

## Build

```bash
./gradlew build
./gradlew build -Pmc=1.21.1
./gradlew build -Pmc=1.21.8
./gradlew build -Pmc=1.21.10
./gradlew build -Pmc=26.1.2
./gradlew buildAll
```

Output jars are in `build/libs`.

Java is selected by Gradle toolchains. For `buildAll`, set one of these only if Gradle cannot find the required JDK automatically:

```bash
./gradlew buildAll -Pjava21Home=/path/to/jdk21 -Pjava25Home=/path/to/jdk25
```

## Required

- Fabric Loader
- Fabric API
- Legacy4J

Mod Menu and integrated mods remain optional.

## Adding an Integration

1. Add a compat class in `src/main/java/com/mtxcore/unifiedlegacysettings`.
2. Add the config toggle in `ModSettingsConfig`.
3. Register the entries in `ModSettingsCompat`.
4. Add the config UI toggle in `NativeConfigScreen`.
5. Add the mod id in `src/main/resources/fabric.mod.json` `suggests` when the integration target is a mod.
