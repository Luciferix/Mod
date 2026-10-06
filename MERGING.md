# Merging the feature into the main mod

The feature here is the **fishing rework**. It is built so it can be dropped into the main mod as-is:

- All feature code is in `fishing` packages, and it never uses the main mod's classes.
- It starts through its own entrypoints in `fabric.mod.json` (`FishingFeature` and `FishingFeatureClient`), so none of the main mod's Java files need editing.
- Its assets and data use the same mod id as the main mod, so item IDs, textures and translations line up.

## Before merging: match the names

This project uses the placeholders `examplemod` (mod id) and `com.example.examplemod` (package).
They need to become the main mod's real ones. Give Claude your mod id and package and it will rename them in this repo; after that, merging is plain copying.

The rename has to cover the following, because the mod id appears in many places:

- File contents: Java, JSON, translation keys such as `fishing.examplemod.*`, and the mixin config names.
- Folder names: the species folder `data/examplemod/examplemod/fish_species/` contains the mod id twice (data namespace and registry name).

Also check that `gradle.properties` here and in the main mod have the same `minecraft_version`, `loader_version` and `fabric_api_version`.
If they differ, update one side first.

## Steps

1. **Copy the code.**
   - `src/main/java/<your package>/fishing/` goes to the same path in the main mod.
   - `src/client/java/<your package>/fishing/` goes to the same path in the main mod.
     If the main mod has no `src/client` folder, put it under `src/main/java` instead.
2. **Copy the resources:** everything in `src/main/resources/assets/` and `src/main/resources/data/`, except `assets/<modid>/icon.png`.
   If a file already exists in the main mod (for example `lang/en_us.json`), don't overwrite it. Copy the entries from this project's file into yours instead.
   The files where that can happen are listed under [Shared files](#shared-files).
3. **Register the entrypoints** in the main mod's `fabric.mod.json`, after your own classes:
   ```json
   "entrypoints": {
     "main": ["<your main class>", "<your package>.fishing.FishingFeature"],
     "client": ["<your client class>", "<your package>.fishing.client.FishingFeatureClient"]
   }
   ```
   If the main mod has no `client` entrypoint, add a `client` list with only `FishingFeatureClient` in it.
4. **Mixins:** copy `src/main/resources/<modid>.fishing.mixins.json` and `src/client/resources/<modid>.fishing.client.mixins.json` into the main mod's resources and add both to the `mixins` list in its `fabric.mod.json`:
   ```json
   "mixins": [
     "<modid>.fishing.mixins.json",
     { "config": "<modid>.fishing.client.mixins.json", "environment": "client" }
   ]
   ```
5. **Dependencies:** the feature adds none beyond Fabric API.
6. **Game tests (optional):** to keep the server tests, copy `src/gametest/` and the `fabricApi { configureTests { ... } }` block marked `fishing` in `build.gradle`.
   They run with `./gradlew runGameTest`, and as part of `./gradlew build`.
7. **Build and test:** `./gradlew build`, then `./gradlew runClient`. ART_HANDOFF.md has a checklist of what to look at in game.

Don't copy `ExampleMod.java`, `ExampleModClient.java`, `fabric.mod.json` or the Gradle files. They stand in for your mod's own.

## Shared files

Files the main mod may already have, which must be merged by hand in step 2. This list is kept up to date as the feature grows.

**Mod files.** Merge the entries into your copy:
- `assets/<modid>/lang/en_us.json`: all of the feature's text.
- `assets/<modid>/sounds.json`: the `fishing.*` sound events.

**Vanilla overrides.** If your mod already overrides these, combine them by hand:
- `assets/minecraft/items/cod.json`, `salmon.json`, `tropical_fish.json`, `pufferfish.json`: these wrap the vanilla fish models so the Fishing Index can draw them as silhouettes.

**Tag additions.** All use `"replace": false`, so merging means adding the listed values to your file:
- `data/minecraft/tags/item/enchantable/fishing.json` and `enchantable/durability.json`: the new rods.
- `data/minecraft/tags/item/fishes.json`: the new raw fish and cooked fish.
- `data/minecraft/tags/villager_trade/fisherman/level_1.json` to `level_5.json`: bait and tackle trades.
- `data/c/tags/item/tools/fishing_rod.json`, `data/c/tags/item/foods/raw_fish.json`, `data/c/tags/item/foods/cooked_fish.json`: Fabric convention tags, for compatibility with other mods.
