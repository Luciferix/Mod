package com.example.examplemod.fishing.client;

import com.example.examplemod.fishing.FishingFeature;

import net.fabricmc.api.ClientModInitializer;

/** Client-only entrypoint of the fishing rework, registered in fabric.mod.json. */
public final class FishingFeatureClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		FishingFeature.LOGGER.info("Fishing rework client initialized");
	}
}
