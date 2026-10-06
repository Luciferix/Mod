package com.example.examplemod.fishing.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import com.example.examplemod.fishing.species.FishRarity;
import com.example.examplemod.fishing.species.SizeClass;

/**
 * Fires when a fish is landed. Conditions (all optional): {@code species}, {@code min_rarity},
 * {@code min_size} (a size class), {@code new_species} and {@code overweight} (heavier than the
 * line's strength).
 */
public class FishCaughtTrigger extends SimpleCriterionTrigger<FishCaughtTrigger.Conditions> {
	@Override
	public Codec<Conditions> codec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayer player, Identifier species, FishRarity rarity, SizeClass size, boolean newSpecies, boolean overweight) {
		this.trigger(player, conditions -> conditions.matches(species, rarity, size, newSpecies, overweight));
	}

	public record Conditions(
			Optional<Holder<LootItemCondition>> player,
			Optional<Identifier> species,
			Optional<FishRarity> minRarity,
			Optional<SizeClass> minSize,
			Optional<Boolean> newSpecies,
			Optional<Boolean> overweight
	) implements SimpleCriterionTrigger.SimpleInstance {
		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				LootItemCondition.CODEC.optionalFieldOf("player").forGetter(Conditions::player),
				Identifier.CODEC.optionalFieldOf("species").forGetter(Conditions::species),
				FishRarity.CODEC.optionalFieldOf("min_rarity").forGetter(Conditions::minRarity),
				SizeClass.CODEC.optionalFieldOf("min_size").forGetter(Conditions::minSize),
				Codec.BOOL.optionalFieldOf("new_species").forGetter(Conditions::newSpecies),
				Codec.BOOL.optionalFieldOf("overweight").forGetter(Conditions::overweight)
		).apply(instance, Conditions::new));

		boolean matches(Identifier species, FishRarity rarity, SizeClass size, boolean newSpecies, boolean overweight) {
			return this.species.map(species::equals).orElse(true)
					&& this.minRarity.map(rarity::isAtLeast).orElse(true)
					&& this.minSize.map(min -> size.ordinal() >= min.ordinal()).orElse(true)
					&& this.newSpecies.map(required -> required == newSpecies).orElse(true)
					&& this.overweight.map(required -> required == overweight).orElse(true);
		}
	}
}
