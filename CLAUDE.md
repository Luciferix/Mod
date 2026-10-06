# Project notes

This repo is a workspace for one large feature that will be merged into the owner's existing Fabric mod.
Keep it merge-friendly: [MERGING.md](MERGING.md) is the contract with the owner.

## Versions

Fabric for Minecraft 26.3 on Java 25: Loom 1.18 (plugin `net.fabricmc.fabric-loom`), Gradle 9.7.1.
Versions live in `gradle.properties` and come from the official template (https://github.com/FabricMC/fabric-example-mod, branch `26.3`).

Minecraft 26.x ships unobfuscated. Code uses Mojang's official names (`Identifier`, not `ResourceLocation`), there are no mappings, and Loom doesn't remap: use `implementation`, not `modImplementation`, and `jar`, not `remapJar`.

## Feature rules

- Mod id `examplemod` and package `com.example.examplemod` are placeholders. They get renamed to the owner's real ones before merging.
- Feature code lives only in `com.example.examplemod.bigfeature` (`src/main`) and `com.example.examplemod.bigfeature.client` (`src/client`).
  `bigfeature` is a working name, to be renamed once the feature has a real one.
- Never import `ExampleMod` or `ExampleModClient`: they stand in for the owner's classes. Use `BigFeature.MOD_ID`, `BigFeature.id(...)` and `BigFeature.LOGGER`.
- Hook into the game only from the `BigFeature` / `BigFeatureClient` entrypoints.
  Mixins go in feature-only configs: `examplemod.bigfeature.mixins.json` with package `com.example.examplemod.bigfeature.mixin`, and a `.client` variant for client mixins.
- Resources go under `assets/examplemod/` and `data/examplemod/`. When adding a file the owner's mod may also have (lang files, `sounds.json`, vanilla tag files under `data/minecraft/tags/`), add it to "Shared files" in MERGING.md.
- Mark new build dependencies with a `// bigfeature` comment (`# bigfeature` in `gradle.properties`).
- Leave `ExampleMod` and `ExampleModClient` as they are. Change `fabric.mod.json` and the Gradle files only for feature entrypoints, mixin configs and `bigfeature`-marked dependencies.

## Building in the cloud sandbox

Building needs JDK 25 (`apt-get install -y openjdk-25-jdk-headless`) and network access to `maven.fabricmc.net`, `piston-meta.mojang.com`, `piston-data.mojang.com`, `libraries.minecraft.net` and (for `runClient`) `resources.download.minecraft.net`.
If the environment's network policy blocks those, Gradle can't build in the sandbox. GitHub Actions (`.github/workflows/build.yml`) builds every push; check the run with the GitHub MCP tools.
