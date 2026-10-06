# Mod: feature workspace

A normal Fabric mod project where one big feature is built on its own, to be merged into the main mod later.
It builds and runs by itself, so the feature can be developed and tested without the main mod.

The feature being built is the **fishing rework**:
- Fish species that depend on biome, bait, time and weather, each catch with its own weight.
- Rarity reveal on the bite, then a reel fight against the fish's HP and a timer.
- Rods with stats, plus hook, line, reel and bait attachments.
- A separate treasure roll after each catch.
- The Fishing Index book with the player's collection and records.

| | |
|---|---|
| Minecraft | 26.3 |
| Fabric | Loader 0.19.5, Fabric API 0.161.0+26.3, Loom 1.18 |
| Java | 25 |
| Mod id / package | `examplemod` / `com.example.examplemod` (placeholders, to be renamed to the main mod's) |

## Building and running

You need JDK 25.

- `./gradlew build` builds the mod jar into `build/libs/` and runs the server game tests.
- `./gradlew runClient` starts Minecraft with the mod.
- `./gradlew runServer` starts a dedicated server with the mod.
- `./gradlew runGameTest` runs only the server game tests.

GitHub Actions also builds every push. The jar can be downloaded from the run's **Artifacts**.

For IDE setup, see the [Fabric documentation](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up).

In game, `/fishing give <species> [weight_kg]` and `/fishing journal reset|complete` (operators only) help with testing.

## Layout

| Path | What it is | Copied when merging? |
|---|---|---|
| `src/main/java/.../fishing/` | The feature: common code (species, catch flow, gear, journal, networking, mixins) | Yes |
| `src/client/java/.../fishing/client/` | The feature: client-only code (bite indicator, fight HUD, Fishing Index screen) | Yes |
| `src/gametest/` | Server game tests for the feature | Optional |
| `src/main/resources/assets/`, `data/` | The feature's assets and data (except `icon.png`) | Yes |
| `ExampleMod.java`, `ExampleModClient.java`, `fabric.mod.json`, `icon.png`, Gradle files | Stand-ins for the main mod's own files | No |

- [MERGING.md](MERGING.md) explains how the feature plugs into the main mod.
- [ART_HANDOFF.md](ART_HANDOFF.md) lists every placeholder asset and what still needs an in-game check.
- All balance numbers are in `fishing/FishingBalance.java`. Fish species are data files in `data/examplemod/examplemod/fish_species/`.
