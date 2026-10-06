package com.example.examplemod.fishing.registry;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;

import net.fabricmc.fabric.api.item.v1.ItemComponentTooltipProviderRegistry;

import com.example.examplemod.fishing.FishingFeature;
import com.example.examplemod.fishing.catching.CaughtFish;
import com.example.examplemod.fishing.gear.Bait;
import com.example.examplemod.fishing.gear.FishingRodStats;
import com.example.examplemod.fishing.gear.RodLoadout;
import com.example.examplemod.fishing.gear.Tackle;

public final class FishingComponents {
	/** Base stats of a fishing rod. The vanilla rod gets this through default components. */
	public static final DataComponentType<FishingRodStats> ROD_STATS = register("rod_stats",
			DataComponentType.<FishingRodStats>builder().persistent(FishingRodStats.CODEC).networkSynchronized(FishingRodStats.STREAM_CODEC));

	/** Hook, line, reel and bait attached to a rod. */
	public static final DataComponentType<RodLoadout> ROD_LOADOUT = register("rod_loadout",
			DataComponentType.<RodLoadout>builder().persistent(RodLoadout.CODEC).networkSynchronized(RodLoadout.STREAM_CODEC));

	/** Makes an item attachable tackle. */
	public static final DataComponentType<Tackle> TACKLE = register("tackle",
			DataComponentType.<Tackle>builder().persistent(Tackle.CODEC).networkSynchronized(Tackle.STREAM_CODEC));

	/** Makes an item usable as bait. */
	public static final DataComponentType<Bait> BAIT = register("bait",
			DataComponentType.<Bait>builder().persistent(Bait.CODEC).networkSynchronized(Bait.STREAM_CODEC));

	/** Species, weight and angler of a caught fish. */
	public static final DataComponentType<CaughtFish> CAUGHT_FISH = register("caught_fish",
			DataComponentType.<CaughtFish>builder().persistent(CaughtFish.CODEC).networkSynchronized(CaughtFish.STREAM_CODEC));

	/**
	 * Client-only display marker: the Fishing Index sets it on icon stacks of undiscovered fish,
	 * and the fish item models render a black silhouette when it is present.
	 */
	public static final DataComponentType<Unit> INDEX_SILHOUETTE = register("index_silhouette",
			DataComponentType.<Unit>builder().persistent(MapCodec.unit(Unit.INSTANCE).codec()).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

	private FishingComponents() {
	}

	private static <T> DataComponentType<T> register(String name, DataComponentType.Builder<T> builder) {
		return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, FishingFeature.id(name), builder.build());
	}

	public static void init() {
		ItemComponentTooltipProviderRegistry.addLast(CAUGHT_FISH);
		ItemComponentTooltipProviderRegistry.addLast(TACKLE);
		ItemComponentTooltipProviderRegistry.addLast(BAIT);
		ItemComponentTooltipProviderRegistry.addLast(ROD_STATS);
	}
}
