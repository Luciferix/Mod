package com.example.examplemod.fishing.catching;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;

import com.example.examplemod.fishing.FishingBalance;
import com.example.examplemod.fishing.advancement.FishingCriteria;
import com.example.examplemod.fishing.gear.Bait;
import com.example.examplemod.fishing.gear.FishingRods;
import com.example.examplemod.fishing.gear.RodLoadout;
import com.example.examplemod.fishing.journal.FishingJournal;
import com.example.examplemod.fishing.journal.SpeciesRecord;
import com.example.examplemod.fishing.network.FishingNetworking;
import com.example.examplemod.fishing.registry.FishingAttachments;
import com.example.examplemod.fishing.registry.FishingComponents;
import com.example.examplemod.fishing.registry.FishingRegistries;
import com.example.examplemod.fishing.registry.FishingSounds;
import com.example.examplemod.fishing.species.FishRarity;
import com.example.examplemod.fishing.species.FishSpecies;
import com.example.examplemod.fishing.species.SizeClass;
import com.example.examplemod.fishing.treasure.TreasureRoller;

/** Everything that happens when a fish is landed: the fish itself, treasure, records and feedback. */
public final class CatchRewards {
	private CatchRewards() {
	}

	public static void land(ServerPlayer player, FishingHook hook, HookSession session) {
		ServerLevel level = player.level();
		FishEncounter encounter = session.encounter();
		FishSpecies species = encounter.species().value();
		Identifier speciesId = encounter.speciesId();
		InteractionHand hand = FishingRods.rodHand(player);
		ItemStack rod = FishingRods.heldRod(player);

		ItemStack fish = createFish(encounter, Optional.of(player.getPlainTextName()));
		List<ItemStack> rewards = new ArrayList<>();
		rewards.add(fish);
		Optional<TreasureRoller.TreasureResult> treasure = TreasureRoller.roll(level, hook, rod, session.gear());
		treasure.ifPresent(result -> rewards.addAll(result.items()));

		CriteriaTriggers.FISHING_ROD_HOOKED.trigger(player, rod, hook, rewards);
		for (ItemStack reward : rewards) {
			throwTowards(level, hook, player, reward);
		}

		player.awardStat(Stats.FISH_CAUGHT, 1);
		level.addFreshEntity(new ExperienceOrb(level, player.getX(), player.getY() + 0.5, player.getZ() + 0.5, experience(encounter, hook)));

		FishingJournal before = FishingAttachments.journal(player);
		SpeciesRecord previous = before.record(speciesId);
		long day = level.getOverworldClockTime() / 24000L + 1L;
		FishingJournal after = before.withCatch(speciesId, encounter.weightKg(), day);
		if (treasure.isPresent()) {
			for (ItemStack item : treasure.get().items()) {
				after = after.withTreasure(BuiltInRegistries.ITEM.getKey(item.getItem()));
			}
		}

		player.setAttached(FishingAttachments.JOURNAL, after);

		SizeClass sizeClass = SizeClass.of(encounter.normalizedSize());
		boolean newSpecies = previous == null;
		boolean newRecord = previous != null && encounter.weightKg() > previous.bestKg();
		FishingCriteria.FISH_CAUGHT.trigger(player, speciesId, species.rarity(), sizeClass, newSpecies, encounter.overweight());
		int totalSpecies = FishingRegistries.species(level.registryAccess()).map(registry -> registry.size()).orElse(0);
		FishingCriteria.INDEX_PROGRESS.trigger(player, after.species().size(), totalSpecies);
		treasure.ifPresent(result -> FishingCriteria.TREASURE_FOUND.trigger(player, result.tier()));

		if (encounter.baitTaken()) {
			consumeBait(player, rod);
		}

		int rodDamage = FishingBalance.ROD_DAMAGE_PER_CATCH + (encounter.overweight() ? FishingBalance.ROD_DAMAGE_OVERWEIGHT : 0);
		rod.hurtAndBreak(rodDamage, player, hand);

		FishingNetworking.broadcast(hook, HookEvent.CAUGHT);
		feedback(player, level, hook, encounter, fish, sizeClass, newSpecies, newRecord, previous, treasure, after.species().size(), totalSpecies);

		session.reset();
		hook.discard();
	}

	/** A fish stack carrying its species, weight and angler; bigger fish restore more hunger. */
	public static ItemStack createFish(FishEncounter encounter, Optional<String> angler) {
		FishSpecies species = encounter.species().value();
		ItemStack fish = new ItemStack(species.item());
		fish.set(FishingComponents.CAUGHT_FISH, new CaughtFish(encounter.speciesId(), encounter.weightKg(),
				encounter.normalizedSize(), species.rarity(), angler));
		FoodProperties food = fish.get(DataComponents.FOOD);
		int bonus = Math.round(encounter.normalizedSize() * FishingBalance.FOOD_SIZE_BONUS_MAX);
		if (food != null && bonus > 0) {
			fish.set(DataComponents.FOOD, new FoodProperties(food.nutrition() + bonus, food.saturation(), food.canAlwaysEat()));
		}

		return fish;
	}

	private static int experience(FishEncounter encounter, FishingHook hook) {
		FishRarity rarity = encounter.rarity();
		int base = Mth.nextInt(hook.getRandom(), FishingBalance.experienceMin(rarity), FishingBalance.experienceMax(rarity));
		return Math.round(base * (1.0F + FishingBalance.EXPERIENCE_SIZE_BONUS * encounter.normalizedSize()));
	}

	private static void throwTowards(ServerLevel level, FishingHook hook, ServerPlayer player, ItemStack stack) {
		ItemEntity entity = new ItemEntity(level, hook.getX(), hook.getY(), hook.getZ(), stack);
		double dx = player.getX() - hook.getX();
		double dy = player.getY() - hook.getY();
		double dz = player.getZ() - hook.getZ();
		entity.setDeltaMovement(dx * 0.1, dy * 0.1 + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08, dz * 0.1);
		level.addFreshEntity(entity);
	}

	private static void consumeBait(ServerPlayer player, ItemStack rod) {
		RodLoadout loadout = rod.get(FishingComponents.ROD_LOADOUT);
		if (loadout == null || loadout.bait().isEmpty()) {
			return;
		}

		ItemStackTemplate bait = loadout.bait().get();
		Bait baitInfo = bait.get(FishingComponents.BAIT);
		if (baitInfo == null || baitInfo.reusable()) {
			return;
		}

		int remaining = bait.count() - 1;
		rod.set(FishingComponents.ROD_LOADOUT, loadout.withBait(remaining > 0 ? Optional.of(bait.withCount(remaining)) : Optional.empty()));
		if (remaining <= 0) {
			player.sendSystemMessage(Component.translatable("fishing.examplemod.bait.ran_out", bait.create().getHoverName()));
		}
	}

	private static void feedback(ServerPlayer player, ServerLevel level, FishingHook hook, FishEncounter encounter, ItemStack fish,
			SizeClass sizeClass, boolean newSpecies, boolean newRecord, SpeciesRecord previous,
			Optional<TreasureRoller.TreasureResult> treasure, int discovered, int totalSpecies) {
		FishRarity rarity = encounter.rarity();
		Component name = Component.empty().append(fish.getHoverName()).withColor(rarity.color());
		Component weight = Component.literal(CaughtFish.formatWeight(encounter.weightKg()));

		player.sendOverlayMessage(Component.translatable("fishing.examplemod.catch.summary", name, weight, sizeClass.displayName()));
		FishingController.playSound(level, hook, rarity.isAtLeast(FishRarity.RARE) ? FishingSounds.CATCH_RARE : FishingSounds.CATCH, 1.0F, 1.0F);
		level.sendParticles(ParticleTypes.SPLASH, hook.getX(), hook.getY() + 0.3, hook.getZ(), 24, 0.4, 0.1, 0.4, 0.2);

		if (newSpecies) {
			player.sendSystemMessage(Component.translatable("fishing.examplemod.catch.new_species", name, discovered, totalSpecies));
			FishingController.playSound(level, hook, FishingSounds.NEW_SPECIES, 1.0F, 1.0F);
		} else if (newRecord) {
			player.sendSystemMessage(Component.translatable("fishing.examplemod.catch.new_record", name, weight,
					Component.literal(CaughtFish.formatWeight(previous.bestKg()))));
			FishingController.playSound(level, hook, FishingSounds.NEW_RECORD, 1.0F, 1.0F);
		}

		if (sizeClass == SizeClass.TROPHY) {
			player.sendSystemMessage(Component.translatable("fishing.examplemod.catch.trophy", name));
		}

		treasure.ifPresent(result -> {
			for (ItemStack item : result.items()) {
				player.sendSystemMessage(Component.translatable("fishing.examplemod.treasure.found", item.getHoverName()));
			}

			FishingController.playSound(level, hook, FishingSounds.TREASURE, 1.0F, 1.0F);
		});

		if (rarity == FishRarity.LEGENDARY) {
			Component announcement = Component.translatable("fishing.examplemod.catch.legendary_broadcast", player.getName(), name, weight);
			for (ServerPlayer other : PlayerLookup.level(level)) {
				other.sendSystemMessage(announcement);
			}
		}
	}
}
