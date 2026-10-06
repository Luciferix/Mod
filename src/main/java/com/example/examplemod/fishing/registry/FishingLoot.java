package com.example.examplemod.fishing.registry;

import java.util.List;
import java.util.Optional;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

/** Natural bait sources: worms from soil, crickets from grass. */
public final class FishingLoot {
	private static final List<Block> WORM_BLOCKS = List.of(Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT,
			Blocks.ROOTED_DIRT, Blocks.PODZOL, Blocks.MUD, Blocks.MYCELIUM);
	private static final List<Block> CRICKET_BLOCKS = List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN);
	private static final float WORM_CHANCE = 0.05F;
	private static final float CRICKET_CHANCE = 0.06F;

	private FishingLoot() {
	}

	public static void init() {
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			if (!source.isBuiltin()) {
				return;
			}

			if (dropsFrom(WORM_BLOCKS, key)) {
				tableBuilder.withPool(chancePool(FishingItems.WORM, WORM_CHANCE));
			} else if (dropsFrom(CRICKET_BLOCKS, key)) {
				tableBuilder.withPool(chancePool(FishingItems.CRICKET, CRICKET_CHANCE));
			}
		});
	}

	private static boolean dropsFrom(List<Block> blocks, ResourceKey<LootTable> key) {
		for (Block block : blocks) {
			Optional<ResourceKey<LootTable>> table = block.getLootTable();
			if (table.isPresent() && table.get().equals(key)) {
				return true;
			}
		}

		return false;
	}

	private static LootPool.Builder chancePool(Item item, float chance) {
		return LootPool.lootPool()
				.add(LootItem.lootTableItem(item))
				.when(LootItemRandomChanceCondition.randomChance(chance))
				.when(ExplosionCondition.survivesExplosion());
	}
}
