package com.example.examplemod.fishing.species;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * A catchable fish species, loaded from {@code data/<namespace>/examplemod/fish_species/}.
 *
 * <p>A species can bite when the bobber is in one of its {@code habitats} (biome sets), the bait on
 * the rod attracts it, and all of its loot {@code conditions} pass (weather, time, depth, structures,
 * ...). Conditions are evaluated on the server only and are not sent to clients.
 */
public record FishSpecies(
		Holder<Item> item,
		FishRarity rarity,
		int weight,
		SizeRange size,
		float difficulty,
		List<HolderSet<Biome>> habitats,
		BaitPreferences bait,
		List<LootItemCondition> conditions,
		Optional<Component> hint,
		Component description
) {
	private static final Codec<List<HolderSet<Biome>>> HABITATS_CODEC = RegistryCodecs.homogeneousList(Registries.BIOME).listOf();

	public static final Codec<FishSpecies> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Item.CODEC.fieldOf("item").forGetter(FishSpecies::item),
			FishRarity.CODEC.fieldOf("rarity").forGetter(FishSpecies::rarity),
			ExtraCodecs.POSITIVE_INT.optionalFieldOf("weight", 10).forGetter(FishSpecies::weight),
			SizeRange.CODEC.fieldOf("size").forGetter(FishSpecies::size),
			Codec.floatRange(0.1F, 10.0F).optionalFieldOf("difficulty", 1.0F).forGetter(FishSpecies::difficulty),
			HABITATS_CODEC.fieldOf("habitats").forGetter(FishSpecies::habitats),
			BaitPreferences.CODEC.optionalFieldOf("bait", BaitPreferences.NONE).forGetter(FishSpecies::bait),
			LootItemCondition.DIRECT_CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(FishSpecies::conditions),
			ComponentSerialization.CODEC.optionalFieldOf("hint").forGetter(FishSpecies::hint),
			ComponentSerialization.CODEC.fieldOf("description").forGetter(FishSpecies::description)
	).apply(instance, FishSpecies::new));

	/** What clients receive: everything except the loot conditions. */
	public static final Codec<FishSpecies> NETWORK_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Item.CODEC.fieldOf("item").forGetter(FishSpecies::item),
			FishRarity.CODEC.fieldOf("rarity").forGetter(FishSpecies::rarity),
			ExtraCodecs.POSITIVE_INT.optionalFieldOf("weight", 10).forGetter(FishSpecies::weight),
			SizeRange.CODEC.fieldOf("size").forGetter(FishSpecies::size),
			Codec.floatRange(0.1F, 10.0F).optionalFieldOf("difficulty", 1.0F).forGetter(FishSpecies::difficulty),
			HABITATS_CODEC.fieldOf("habitats").forGetter(FishSpecies::habitats),
			BaitPreferences.CODEC.optionalFieldOf("bait", BaitPreferences.NONE).forGetter(FishSpecies::bait),
			ComponentSerialization.CODEC.optionalFieldOf("hint").forGetter(FishSpecies::hint),
			ComponentSerialization.CODEC.fieldOf("description").forGetter(FishSpecies::description)
	).apply(instance, (item, rarity, weight, size, difficulty, habitats, bait, hint, description) ->
			new FishSpecies(item, rarity, weight, size, difficulty, habitats, bait, List.of(), hint, description)));

	public boolean livesIn(Holder<Biome> biome) {
		for (HolderSet<Biome> habitat : this.habitats) {
			if (habitat.contains(biome)) {
				return true;
			}
		}

		return false;
	}

	public boolean conditionsPass(LootContext context) {
		for (LootItemCondition condition : this.conditions) {
			if (!condition.test(context)) {
				return false;
			}
		}

		return true;
	}

	public Component displayName() {
		return this.item.value().getName(this.item.value().getDefaultInstance());
	}
}
