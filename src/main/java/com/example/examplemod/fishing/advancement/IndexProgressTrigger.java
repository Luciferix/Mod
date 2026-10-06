package com.example.examplemod.fishing.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Fires whenever the Fishing Index changes. Conditions: {@code min_discovered} (species count)
 * and/or {@code complete} (every species in the registry discovered).
 */
public class IndexProgressTrigger extends SimpleCriterionTrigger<IndexProgressTrigger.Conditions> {
	@Override
	public Codec<Conditions> codec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayer player, int discovered, int total) {
		this.trigger(player, conditions -> conditions.matches(discovered, total));
	}

	public record Conditions(
			Optional<Holder<LootItemCondition>> player,
			Optional<Integer> minDiscovered,
			Optional<Boolean> complete
	) implements SimpleCriterionTrigger.SimpleInstance {
		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				LootItemCondition.CODEC.optionalFieldOf("player").forGetter(Conditions::player),
				Codec.INT.optionalFieldOf("min_discovered").forGetter(Conditions::minDiscovered),
				Codec.BOOL.optionalFieldOf("complete").forGetter(Conditions::complete)
		).apply(instance, Conditions::new));

		boolean matches(int discovered, int total) {
			return this.minDiscovered.map(min -> discovered >= min).orElse(true)
					&& this.complete.map(required -> !required || (total > 0 && discovered >= total)).orElse(true);
		}
	}
}
