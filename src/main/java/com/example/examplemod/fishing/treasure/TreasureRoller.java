package com.example.examplemod.fishing.treasure;

import java.util.List;
import java.util.Optional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import com.example.examplemod.fishing.FishingBalance;
import com.example.examplemod.fishing.gear.GearStats;

/**
 * The separate treasure roll after a successful catch. The rod's Treasure stat (plus Luck) is the
 * chance; Luck also shifts which tier's loot table is used.
 */
public final class TreasureRoller {
	private TreasureRoller() {
	}

	public static float chance(GearStats gear) {
		return Math.min(FishingBalance.TREASURE_CHANCE_CAP, gear.treasureChance() + Math.max(0.0F, gear.luck()) * FishingBalance.TREASURE_CHANCE_PER_LUCK);
	}

	public static Optional<TreasureResult> roll(ServerLevel level, FishingHook hook, ItemStack rod, GearStats gear) {
		RandomSource random = hook.getRandom();
		if (random.nextFloat() >= chance(gear)) {
			return Optional.empty();
		}

		TreasureTier tier = rollTier(gear.luck(), random);
		LootTable table = level.getServer().reloadableRegistries().getLootTable(tier.lootTable());
		LootParams params = new LootParams.Builder(level)
				.withParameter(LootContextParams.ORIGIN, hook.position())
				.withParameter(LootContextParams.TOOL, rod)
				.withParameter(LootContextParams.THIS_ENTITY, hook)
				.withLuck(gear.luck())
				.create(LootContextParamSets.FISHING);
		List<ItemStack> items = table.getRandomItems(params);
		return items.isEmpty() ? Optional.empty() : Optional.of(new TreasureResult(tier, items));
	}

	public static TreasureTier rollTier(float luck, RandomSource random) {
		float total = 0.0F;
		for (TreasureTier tier : TreasureTier.values()) {
			total += FishingBalance.treasureTierWeight(tier, luck);
		}

		float roll = random.nextFloat() * total;
		for (TreasureTier tier : TreasureTier.values()) {
			roll -= FishingBalance.treasureTierWeight(tier, luck);
			if (roll < 0.0F) {
				return tier;
			}
		}

		return TreasureTier.COMMON;
	}

	public record TreasureResult(TreasureTier tier, List<ItemStack> items) {
	}
}
