package com.example.examplemod.fishing;

import net.minecraft.util.Mth;

import com.example.examplemod.fishing.species.FishRarity;
import com.example.examplemod.fishing.treasure.TreasureTier;

/**
 * Every tuning number of the fishing rework in one place, so the system can be rebalanced
 * without touching its logic. Times are in ticks (20 per second) unless noted otherwise.
 */
public final class FishingBalance {
	private FishingBalance() {
	}

	// ---- Rarity roll ----

	/** Relative chance of each rarity before Luck is applied. */
	public static float baseRarityWeight(FishRarity rarity) {
		return switch (rarity) {
			case COMMON -> 60.0F;
			case UNCOMMON -> 25.0F;
			case RARE -> 10.0F;
			case EPIC -> 4.0F;
			case LEGENDARY -> 1.0F;
		};
	}

	/** How much each point of Luck scales a rarity's weight (negative for Common). */
	public static float rarityLuckScaling(FishRarity rarity) {
		return switch (rarity) {
			case COMMON -> -0.06F;
			case UNCOMMON -> 0.05F;
			case RARE -> 0.12F;
			case EPIC -> 0.20F;
			case LEGENDARY -> 0.30F;
		};
	}

	/** Luck never pushes a rarity's weight below this fraction of its base weight. */
	public static final float MIN_RARITY_WEIGHT_FACTOR = 0.15F;

	public static float rarityWeight(FishRarity rarity, float luck) {
		return baseRarityWeight(rarity) * Math.max(MIN_RARITY_WEIGHT_FACTOR, 1.0F + rarityLuckScaling(rarity) * luck);
	}

	/** Species whose bait affinity is this low or lower can't be caught with that bait. */
	public static final float MIN_BAIT_AFFINITY = 0.0F;

	// ---- Size ----

	/**
	 * Normalized sizes are rolled as {@code random ^ exponent}. An exponent above 1 makes small
	 * fish common and big ones rare; Size Bonus lowers it.
	 */
	public static final float SIZE_EXPONENT_BASE = 2.0F;
	public static final float SIZE_BONUS_EXPONENT_FACTOR = 2.0F;

	/** Chance to roll a specimen from the top 10% of the size range, before Size Bonus. */
	public static final float TROPHY_ROLL_CHANCE = 0.01F;
	public static final float TROPHY_ROLL_CHANCE_PER_SIZE_BONUS = 0.02F;

	public static float sizeExponent(float sizeBonus) {
		return SIZE_EXPONENT_BASE / (1.0F + SIZE_BONUS_EXPONENT_FACTOR * Math.max(0.0F, sizeBonus));
	}

	// ---- Fight ----

	public static float baseFishHp(FishRarity rarity) {
		return switch (rarity) {
			case COMMON -> 40.0F;
			case UNCOMMON -> 80.0F;
			case RARE -> 160.0F;
			case EPIC -> 300.0F;
			case LEGENDARY -> 500.0F;
		};
	}

	/** HP multiplier for the smallest (normalized 0) and the largest (normalized 1) specimen. */
	public static final float HP_SIZE_FACTOR_MIN = 0.6F;
	public static final float HP_SIZE_FACTOR_MAX = 1.5F;

	/** Extra HP and stronger pulls when the fish is heavier than the line's strength. */
	public static final float OVERWEIGHT_HP_MULTIPLIER = 1.25F;
	public static final float OVERWEIGHT_PULL_MULTIPLIER = 1.5F;

	public static float fishHp(FishRarity rarity, float speciesDifficulty, float normalizedSize, boolean overweight) {
		float sizeFactor = Mth.lerp(normalizedSize, HP_SIZE_FACTOR_MIN, HP_SIZE_FACTOR_MAX);
		return baseFishHp(rarity) * speciesDifficulty * sizeFactor * (overweight ? OVERWEIGHT_HP_MULTIPLIER : 1.0F);
	}

	/** Fight duration in seconds is {@code BASE + PER_HP * hp}, clamped, then scaled by reels. */
	public static final float FIGHT_SECONDS_BASE = 5.0F;
	public static final float FIGHT_SECONDS_PER_HP = 0.012F;
	public static final float FIGHT_SECONDS_MIN = 6.0F;
	public static final float FIGHT_SECONDS_MAX = 20.0F;

	public static int fightTicks(float maxHp, float fightTimeMultiplier) {
		float seconds = Mth.clamp(FIGHT_SECONDS_BASE + FIGHT_SECONDS_PER_HP * maxHp, FIGHT_SECONDS_MIN, FIGHT_SECONDS_MAX);
		return Math.round(seconds * fightTimeMultiplier * 20.0F);
	}

	/** Each reel action deals catch damage times a random factor in [1 - spread, 1 + spread]. */
	public static final float DAMAGE_SPREAD = 0.10F;
	public static final float CRITICAL_REEL_CHANCE = 0.08F;
	public static final float CRITICAL_REEL_MULTIPLIER = 1.75F;

	/** Reel actions are rate-limited per player to stop auto-clickers. */
	public static final int MAX_REEL_ACTIONS_PER_SECOND = 14;

	/** The fish pulls back every {@code PULL_INTERVAL_MIN..MAX} ticks, shortened for rarer fish. */
	public static final int PULL_INTERVAL_MIN = 40;
	public static final int PULL_INTERVAL_MAX = 70;
	public static final float PULL_INTERVAL_REDUCTION_PER_RARITY = 0.08F;

	/** Fraction of max HP the fish regains on each pull, before line Pull Resistance. */
	public static float pullHealFraction(FishRarity rarity) {
		return switch (rarity) {
			case COMMON -> 0.06F;
			case UNCOMMON -> 0.065F;
			case RARE -> 0.075F;
			case EPIC -> 0.085F;
			case LEGENDARY -> 0.10F;
		};
	}

	/** Below this fraction of max HP the fish counts as nearly landed (faster pulse, cue sound). */
	public static final float LOW_HP_FRACTION = 0.25F;

	// ---- Bite ----

	/** Ticks the player has to set the hook after a bite, longer for rarer fish whose reveal takes longer. */
	public static int biteWindowTicks(FishRarity rarity) {
		return 50 + 8 * rarity.ordinal();
	}

	/** Ticks per step of the rarity reveal (white, green, blue, purple, gold). */
	public static final int REVEAL_STEP_TICKS = 6;

	// ---- Rewards ----

	public static int experienceMin(FishRarity rarity) {
		return switch (rarity) {
			case COMMON -> 1;
			case UNCOMMON -> 3;
			case RARE -> 6;
			case EPIC -> 10;
			case LEGENDARY -> 20;
		};
	}

	public static int experienceMax(FishRarity rarity) {
		return switch (rarity) {
			case COMMON -> 3;
			case UNCOMMON -> 6;
			case RARE -> 10;
			case EPIC -> 16;
			case LEGENDARY -> 35;
		};
	}

	/** Bonus experience multiplier for the largest specimens (normalized size 1). */
	public static final float EXPERIENCE_SIZE_BONUS = 0.5F;

	/** Extra hunger restored by a raw fish at the top of its size range. */
	public static final int FOOD_SIZE_BONUS_MAX = 3;

	/** Rod durability used per catch, plus extra when the fish was heavier than the line. */
	public static final int ROD_DAMAGE_PER_CATCH = 1;
	public static final int ROD_DAMAGE_OVERWEIGHT = 1;

	// ---- Treasure ----

	/** Treasure chance gained per point of Luck, and the cap on the total chance. */
	public static final float TREASURE_CHANCE_PER_LUCK = 0.0025F;
	public static final float TREASURE_CHANCE_CAP = 0.25F;

	public static float treasureTierWeight(TreasureTier tier, float luck) {
		return switch (tier) {
			case COMMON -> Math.max(10.0F, 70.0F - 2.5F * luck);
			case RARE -> 25.0F + 2.0F * luck;
			case LEGENDARY -> 5.0F + 0.75F * luck;
		};
	}
}
