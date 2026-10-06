package com.example.examplemod.bigfeature;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common (both sides) entrypoint of the big feature, registered in fabric.mod.json.
 *
 * <p>The feature never references the main mod's classes, so this package can be copied into
 * any mod with the same mod id and compile unchanged. Everything the feature registers is
 * reached from {@link #onInitialize()}.
 */
public final class BigFeature implements ModInitializer {
	/** Must match the {@code id} in fabric.mod.json. */
	public static final String MOD_ID = "examplemod";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID + "/bigfeature");

	@Override
	public void onInitialize() {
		LOGGER.info("Big feature initialized");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
