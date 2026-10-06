package com.example.examplemod.fishing.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

import com.example.examplemod.fishing.FishingFeature;

public final class FishingCreativeTab {
	public static final ResourceKey<CreativeModeTab> KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, FishingFeature.id("fishing"));

	private FishingCreativeTab() {
	}

	public static void init() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, KEY, FabricCreativeModeTab.builder()
				.icon(() -> new ItemStack(FishingItems.FISHING_INDEX))
				.title(Component.translatable("itemGroup.examplemod.fishing"))
				.displayItems((parameters, output) -> {
					output.accept(FishingItems.FISHING_INDEX);
					output.accept(Items.FISHING_ROD);
					FishingItems.rods().forEach(output::accept);
					FishingItems.tackle().forEach(output::accept);
					FishingItems.bait().forEach(output::accept);
					output.accept(Items.COD);
					output.accept(Items.SALMON);
					output.accept(Items.TROPICAL_FISH);
					output.accept(Items.PUFFERFISH);
					FishingItems.fish().forEach(output::accept);
					output.accept(FishingItems.COOKED_FISH);
				})
				.build());
	}
}
