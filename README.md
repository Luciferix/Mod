# Mod: feature workspace

A normal Fabric mod project where one big feature is built on its own, to be merged into the main mod later.
It builds and runs by itself, so the feature can be developed and tested without the main mod.

| | |
|---|---|
| Minecraft | 26.3 |
| Fabric | Loader 0.19.5, Fabric API 0.161.0+26.3, Loom 1.18 |
| Java | 25 |
| Mod id / package | `examplemod` / `com.example.examplemod` (placeholders, to be renamed to the main mod's) |

## Building and running

You need JDK 25.

- `./gradlew build` builds the mod jar into `build/libs/`.
- `./gradlew runClient` starts Minecraft with the mod.
- `./gradlew runServer` starts a dedicated server with the mod.

GitHub Actions also builds every push. The jar can be downloaded from the run's **Artifacts**.

For IDE setup, see the [Fabric documentation](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up).

## Layout

| Path | What it is | Copied when merging? |
|---|---|---|
| `src/main/java/.../bigfeature/` | The feature: common code | Yes |
| `src/client/java/.../bigfeature/client/` | The feature: client-only code | Yes |
| `src/main/resources/assets/examplemod/`, `data/examplemod/` | The feature's assets and data (except `icon.png`) | Yes |
| `ExampleMod.java`, `ExampleModClient.java`, `fabric.mod.json`, `icon.png`, Gradle files | Stand-ins for the main mod's own files | No |

[MERGING.md](MERGING.md) explains how the feature plugs into the main mod.
