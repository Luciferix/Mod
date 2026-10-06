package com.example.examplemod.fishing.catching;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import com.example.examplemod.fishing.FishingBalance;
import com.example.examplemod.fishing.gear.Bait;
import com.example.examplemod.fishing.gear.GearStats;
import com.example.examplemod.fishing.gear.RodLoadout;
import com.example.examplemod.fishing.registry.FishingComponents;
import com.example.examplemod.fishing.registry.FishingRegistries;
import com.example.examplemod.fishing.species.FishRarity;
import com.example.examplemod.fishing.species.FishSpecies;

/**
 * Decides which fish bites: species that live in the bobber's biome, accept the rod's bait and
 * pass their conditions are candidates. Luck shifts the rarity roll, then a species of that
 * rarity is picked by weight, and finally its size is rolled.
 */
public final class EncounterRoller {
	private EncounterRoller() {
	}

	public static Optional<FishEncounter> roll(ServerLevel level, FishingHook hook, ItemStack rod, GearStats gear) {
		Optional<Registry<FishSpecies>> registry = FishingRegistries.species(level.registryAccess());
		if (registry.isEmpty()) {
			return Optional.empty();
		}

		Holder<Biome> biome = level.getBiome(hook.blockPosition());
		LootContext context = lootContext(level, hook, rod, gear.luck());
		Optional<Bait> bait = attachedBait(rod);

		List<Candidate> candidates = candidates(registry.get(), biome, context, bait);
		boolean baitTaken = bait.isPresent() && !candidates.isEmpty();
		if (candidates.isEmpty() && bait.isPresent()) {
			// Nothing here wants this bait: fish bite the bare hook instead and the bait stays on.
			candidates = candidates(registry.get(), biome, context, Optional.empty());
		}

		if (candidates.isEmpty()) {
			candidates = strays(registry.get());
		}

		RandomSource random = hook.getRandom();
		Optional<Candidate> chosen = pick(candidates, gear.luck(), random);
		if (chosen.isEmpty()) {
			return Optional.empty();
		}

		FishSpecies species = chosen.get().species().value();
		float normalizedSize = rollNormalizedSize(gear.sizeBonus(), random);
		float weightKg = species.size().weightAt(normalizedSize);
		boolean overweight = weightKg > gear.lineStrength();
		float maxHp = FishingBalance.fishHp(species.rarity(), species.difficulty(), normalizedSize, overweight);
		int fightTicks = FishingBalance.fightTicks(maxHp, gear.fightTimeMultiplier());
		return Optional.of(new FishEncounter(chosen.get().species(), weightKg, normalizedSize, overweight, maxHp, fightTicks, baitTaken));
	}

	/** Rolls a normalized size in [0, 1]; small fish are common unless Size Bonus pushes up. */
	public static float rollNormalizedSize(float sizeBonus, RandomSource random) {
		float trophyChance = FishingBalance.TROPHY_ROLL_CHANCE + FishingBalance.TROPHY_ROLL_CHANCE_PER_SIZE_BONUS * sizeBonus;
		if (random.nextFloat() < trophyChance) {
			return 0.9F + 0.1F * random.nextFloat();
		}

		return Mth.clamp((float) Math.pow(random.nextFloat(), FishingBalance.sizeExponent(sizeBonus)), 0.0F, 1.0F);
	}

	/** Rolls a rarity among those present in the candidates (weighted by Luck), then a species of it. */
	public static Optional<Candidate> pick(List<Candidate> candidates, float luck, RandomSource random) {
		Map<FishRarity, List<Candidate>> byRarity = new EnumMap<>(FishRarity.class);
		for (Candidate candidate : candidates) {
			byRarity.computeIfAbsent(candidate.species().value().rarity(), ignored -> new ArrayList<>()).add(candidate);
		}

		if (byRarity.isEmpty()) {
			return Optional.empty();
		}

		float totalRarityWeight = 0.0F;
		for (FishRarity rarity : byRarity.keySet()) {
			totalRarityWeight += FishingBalance.rarityWeight(rarity, luck);
		}

		float roll = random.nextFloat() * totalRarityWeight;
		FishRarity rolled = null;
		for (FishRarity rarity : byRarity.keySet()) {
			rolled = rarity;
			roll -= FishingBalance.rarityWeight(rarity, luck);
			if (roll < 0.0F) {
				break;
			}
		}

		List<Candidate> pool = byRarity.get(rolled);
		float totalWeight = 0.0F;
		for (Candidate candidate : pool) {
			totalWeight += candidate.weight();
		}

		float pickRoll = random.nextFloat() * totalWeight;
		for (Candidate candidate : pool) {
			pickRoll -= candidate.weight();
			if (pickRoll < 0.0F) {
				return Optional.of(candidate);
			}
		}

		return Optional.of(pool.getLast());
	}

	public static List<Candidate> candidates(Registry<FishSpecies> registry, Holder<Biome> biome, LootContext context, Optional<Bait> bait) {
		Optional<Identifier> baitType = bait.map(Bait::type);
		float potency = bait.map(Bait::potency).orElse(1.0F);
		List<Candidate> candidates = new ArrayList<>();
		registry.listElements().forEach(holder -> {
			FishSpecies species = holder.value();
			float affinity = species.bait().affinity(baitType);
			if (affinity > FishingBalance.MIN_BAIT_AFFINITY && species.livesIn(biome) && species.conditionsPass(context)) {
				candidates.add(new Candidate(holder, species.weight() * affinity * potency));
			}
		});
		return candidates;
	}

	/** Fallback for waters no species calls home: any Common fish that bites a bare hook. */
	private static List<Candidate> strays(Registry<FishSpecies> registry) {
		List<Candidate> candidates = new ArrayList<>();
		registry.listElements().forEach(holder -> {
			FishSpecies species = holder.value();
			if (species.rarity() == FishRarity.COMMON && species.bait().unbaited() > 0.0F && species.conditions().isEmpty()) {
				candidates.add(new Candidate(holder, species.weight() * species.bait().unbaited()));
			}
		});
		return candidates;
	}

	public static Optional<Bait> attachedBait(ItemStack rod) {
		RodLoadout loadout = rod.get(FishingComponents.ROD_LOADOUT);
		if (loadout == null) {
			return Optional.empty();
		}

		return loadout.bait().map(template -> template.get(FishingComponents.BAIT));
	}

	private static LootContext lootContext(ServerLevel level, FishingHook hook, ItemStack rod, float luck) {
		LootParams params = new LootParams.Builder(level)
				.withParameter(LootContextParams.ORIGIN, hook.position())
				.withParameter(LootContextParams.TOOL, rod)
				.withParameter(LootContextParams.THIS_ENTITY, hook)
				.withLuck(luck)
				.create(LootContextParamSets.FISHING);
		return new LootContext.Builder(params).create(Optional.empty());
	}

	public record Candidate(Holder.Reference<FishSpecies> species, float weight) {
	}
}
