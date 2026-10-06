package com.example.examplemod.fishing.treasure;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

import com.example.examplemod.fishing.FishingFeature;

/** Treasure quality; each tier rolls its own loot table. */
public enum TreasureTier {
	COMMON("common"),
	RARE("rare"),
	LEGENDARY("legendary");

	private final String name;
	private final ResourceKey<LootTable> lootTable;

	TreasureTier(String name) {
		this.name = name;
		this.lootTable = ResourceKey.create(Registries.LOOT_TABLE, FishingFeature.id("gameplay/fishing_treasure/" + name));
	}

	public ResourceKey<LootTable> lootTable() {
		return this.lootTable;
	}

	public String getName() {
		return this.name;
	}
}
