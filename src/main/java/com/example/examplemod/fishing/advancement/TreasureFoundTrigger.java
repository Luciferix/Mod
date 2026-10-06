package com.example.examplemod.fishing.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import com.example.examplemod.fishing.treasure.TreasureTier;

/** Fires when treasure is fished up. Optional condition: {@code tier}. */
public class TreasureFoundTrigger extends SimpleCriterionTrigger<TreasureFoundTrigger.Conditions> {
	@Override
	public Codec<Conditions> codec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayer player, TreasureTier tier) {
		this.trigger(player, conditions -> conditions.tier().map(tier::equals).orElse(true));
	}

	public record Conditions(Optional<Holder<LootItemCondition>> player, Optional<TreasureTier> tier)
			implements SimpleCriterionTrigger.SimpleInstance {
		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				LootItemCondition.CODEC.optionalFieldOf("player").forGetter(Conditions::player),
				TreasureTier.CODEC.optionalFieldOf("tier").forGetter(Conditions::tier)
		).apply(instance, Conditions::new));
	}
}
