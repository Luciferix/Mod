package com.example.examplemod.bigfeature.client;

import com.example.examplemod.bigfeature.BigFeature;

import net.fabricmc.api.ClientModInitializer;

/**
 * Client-only entrypoint of the big feature (rendering, screens, key binds, ...),
 * registered in fabric.mod.json.
 */
public final class BigFeatureClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BigFeature.LOGGER.info("Big feature client initialized");
	}
}
