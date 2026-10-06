package com.example.examplemod.fishing.registry;

import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import net.fabricmc.fabric.api.event.registry.DynamicRegistries;

import com.example.examplemod.fishing.FishingFeature;
import com.example.examplemod.fishing.species.FishSpecies;

public final class FishingRegistries {
	/** Data-driven fish species, synced to clients for the Fishing Index. */
	public static final ResourceKey<Registry<FishSpecies>> FISH_SPECIES = ResourceKey.createRegistryKey(FishingFeature.id("fish_species"));

	private FishingRegistries() {
	}

	public static void init() {
		DynamicRegistries.registerSynced(FISH_SPECIES, FishSpecies.CODEC, FishSpecies.NETWORK_CODEC);
	}

	public static Optional<Registry<FishSpecies>> species(RegistryAccess registries) {
		return registries.lookup(FISH_SPECIES);
	}

	public static Optional<Holder.Reference<FishSpecies>> species(RegistryAccess registries, Identifier id) {
		return registries.get(ResourceKey.create(FISH_SPECIES, id));
	}
}
