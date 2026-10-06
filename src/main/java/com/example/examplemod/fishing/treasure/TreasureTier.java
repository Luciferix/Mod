package com.example.examplemod.fishing.treasure;

import com.mojang.serialization.Codec;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.storage.loot.LootTable;

import com.example.examplemod.fishing.FishingFeature;

/** Treasure quality; each tier rolls its own loot table. */
public enum TreasureTier implements StringRepresentable {
	COMMON("common"),
	RARE("rare"),
	LEGENDARY("legendary");

	public static final Codec<TreasureTier> CODEC = StringRepresentable.fromEnum(TreasureTier::values);

	private final String name;
	private final ResourceKey<LootTable> lootTable;

	TreasureTier(String name) {
		this.name = name;
		this.lootTable = ResourceKey.create(Registries.LOOT_TABLE, FishingFeature.id("gameplay/fishing_treasure/" + name));
	}

	public ResourceKey<LootTable> lootTable() {
		return this.lootTable;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}
