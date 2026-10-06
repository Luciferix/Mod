# Merging the feature into the main mod

The feature is built so it can be dropped into the main mod as-is:

- All feature code is in `bigfeature` packages, and it never uses the main mod's classes.
- It starts through its own entrypoints in `fabric.mod.json` (`BigFeature` and `BigFeatureClient`), so none of the main mod's Java files need editing.
- Its assets and data use the same mod id as the main mod, so item IDs, textures and translations line up.

## Before merging: match the names

This project uses the placeholders `examplemod` (mod id) and `com.example.examplemod` (package).
They need to become the main mod's real ones. Give Claude your mod id and package and it will rename them in this repo; after that, merging is plain copying.

Also check that `gradle.properties` here and in the main mod have the same `minecraft_version`, `loader_version` and `fabric_api_version`.
If they differ, update one side first.

## Steps

1. **Copy the code.**
   - `src/main/java/<your package>/bigfeature/` goes to the same path in the main mod.
   - `src/client/java/<your package>/bigfeature/` goes to the same path in the main mod.
     If the main mod has no `src/client` folder, put it under `src/main/java` instead.
2. **Copy the resources:** everything in `src/main/resources/assets/<modid>/` and `src/main/resources/data/<modid>/`, except `icon.png`.
   If a file already exists in the main mod (for example `lang/en_us.json`), don't overwrite it. Copy the entries from this project's file into yours instead.
   The files where that can happen are listed under [Shared files](#shared-files).
3. **Register the entrypoints** in the main mod's `fabric.mod.json`, after your own classes:
   ```json
   "entrypoints": {
     "main": ["<your main class>", "<your package>.bigfeature.BigFeature"],
     "client": ["<your client class>", "<your package>.bigfeature.client.BigFeatureClient"]
   }
   ```
   If the main mod has no `client` entrypoint, add a `client` list with only `BigFeatureClient` in it.
4. **Mixins (only if the feature has any):** copy the `<modid>.bigfeature*.mixins.json` files into the main mod's resources and add them to the `mixins` list in its `fabric.mod.json`.
5. **Dependencies (only if the feature added any):** copy the lines marked `bigfeature` in this project's `build.gradle` and `gradle.properties` into the main mod's.
6. **Build and test:** `./gradlew build`, then `./gradlew runClient`.

Don't copy `ExampleMod.java`, `ExampleModClient.java`, `fabric.mod.json` or the Gradle files. They stand in for your mod's own.

## Shared files

Files the main mod may already have, which must be merged by hand in step 2. This list is kept up to date as the feature grows.

- None yet.
