package com.example.examplemod.fishing.advancement;

import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

import com.example.examplemod.fishing.FishingFeature;

public final class FishingCriteria {
	public static final FishCaughtTrigger FISH_CAUGHT = register("fish_caught", new FishCaughtTrigger());
	public static final IndexProgressTrigger INDEX_PROGRESS = register("index_progress", new IndexProgressTrigger());
	public static final TreasureFoundTrigger TREASURE_FOUND = register("treasure_found", new TreasureFoundTrigger());

	private FishingCriteria() {
	}

	public static void init() {
	}

	private static <T extends CriterionTrigger<?>> T register(String name, T trigger) {
		return Registry.register(BuiltInRegistries.TRIGGER_TYPES, FishingFeature.id(name), trigger);
	}
}
