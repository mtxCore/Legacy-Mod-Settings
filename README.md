# Legacy Mod Settings

Legacy Mod Settings adds optional mod toggles directly to Legacy4J menu screens.
Each integration is soft-dependent: if a target mod is missing, that entry is skipped.

## Integrations

| Menu | Option | Mod |
| --- | --- | --- |
| Graphics | Advanced Graphical Effects | [Iris](https://modrinth.com/mod/iris) |
| Graphics | Extended Render Distance | [Bobby](https://modrinth.com/mod/bobby) |
| Advanced Graphics | Grass Detail | [LambdaBetterGrass](https://modrinth.com/mod/lambdabettergrass) |
| Advanced Graphics | Snow Layer Blending | [LambdaBetterGrass](https://modrinth.com/mod/lambdabettergrass) |
| Advanced Graphics | Connected Textures | [Continuity](https://modrinth.com/mod/continuity) |
| Advanced Graphics | Emissive Textures | [Continuity](https://modrinth.com/mod/continuity) |
| Advanced Graphics | Third-Person Animations | [Not Enough Animations](https://modrinth.com/mod/not-enough-animations) |
| Advanced Graphics | Entity Culling | [Sodium](https://modrinth.com/mod/sodium) |
| Advanced Graphics | Particle Culling | [Sodium](https://modrinth.com/mod/sodium) |
| Game Options | Dynamic Lighting | [LambDynamicLights](https://modrinth.com/mod/lambdynamiclights) |
| Game Options | Zoom | [Zoomify](https://modrinth.com/mod/zoomify) |
| Game Options | Locator Compass | [Locator Lodestones](https://modrinth.com/mod/locator-lodestones) |
| Audio | Presence Footsteps | [Presence Footsteps](https://modrinth.com/mod/presence-footsteps) |
| User Interface | Chat Portraits | [Chat Heads](https://modrinth.com/mod/chat-heads) |
| User Interface | Minimap | [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) |

## Configuration

Open Mod Menu -> Legacy Mod Settings -> Config.
Settings are written to `config/legacy_mod_settings.json`.

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

## Required

- Fabric Loader
- Fabric API
- Legacy4J
- Cloth Config

Integrated mods remain optional.

## Adding an Integration

1. Add a compat class in `src/main/java/com/mtxcore/legacymodsettings`.
2. Add the config toggle in `ModSettingsConfig`.
3. Register the entries in `ModSettingsCompat`.
4. Add the config UI toggle in `ClothConfigScreenFactory`.
5. Add the mod id in `src/main/resources/fabric.mod.json` `suggests`.
