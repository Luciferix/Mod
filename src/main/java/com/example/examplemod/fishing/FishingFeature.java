package com.example.examplemod.fishing;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.examplemod.fishing.advancement.FishingCriteria;
import com.example.examplemod.fishing.gear.TackleInteractions;
import com.example.examplemod.fishing.network.FishingNetworking;
import com.example.examplemod.fishing.registry.FishingAttachments;
import com.example.examplemod.fishing.registry.FishingCommands;
import com.example.examplemod.fishing.registry.FishingComponents;
import com.example.examplemod.fishing.registry.FishingCreativeTab;
import com.example.examplemod.fishing.registry.FishingItems;
import com.example.examplemod.fishing.registry.FishingLoot;
import com.example.examplemod.fishing.registry.FishingRegistries;
import com.example.examplemod.fishing.registry.FishingSounds;

/**
 * Common entrypoint of the fishing rework, registered in fabric.mod.json.
 *
 * <p>The feature never references the main mod's classes, so this package can be copied into
 * any mod with the same mod id and compile unchanged. Everything it registers is reached from
 * {@link #onInitialize()}.
 */
public final class FishingFeature implements ModInitializer {
	/** Must match the {@code id} in fabric.mod.json. */
	public static final String MOD_ID = "examplemod";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID + "/fishing");

	@Override
	public void onInitialize() {
		FishingComponents.init();
		FishingRegistries.init();
		FishingSounds.init();
		FishingItems.init();
		FishingCreativeTab.init();
		FishingAttachments.init();
		FishingCriteria.init();
		FishingNetworking.init();
		FishingLoot.init();
		TackleInteractions.init();
		FishingCommands.init();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
