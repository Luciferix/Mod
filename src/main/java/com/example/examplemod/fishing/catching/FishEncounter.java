package com.example.examplemod.fishing.catching;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;

import com.example.examplemod.fishing.species.FishRarity;
import com.example.examplemod.fishing.species.FishSpecies;

/**
 * The fish on the other end of the line, rolled when it bites.
 *
 * @param baitTaken whether the rod's bait attracted this fish (and is used up on a catch)
 */
public record FishEncounter(
		Holder.Reference<FishSpecies> species,
		float weightKg,
		float normalizedSize,
		boolean overweight,
		float maxHp,
		int fightTicks,
		boolean baitTaken
) {
	public FishRarity rarity() {
		return this.species.value().rarity();
	}

	public Identifier speciesId() {
		return this.species.key().identifier();
	}
}
