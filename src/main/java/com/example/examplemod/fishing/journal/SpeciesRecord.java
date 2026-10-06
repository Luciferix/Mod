package com.example.examplemod.fishing.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** A player's history with one species. */
public record SpeciesRecord(int caught, float bestKg, long firstCaughtDay) {
	public static final Codec<SpeciesRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("caught").forGetter(SpeciesRecord::caught),
			Codec.FLOAT.fieldOf("best_kg").forGetter(SpeciesRecord::bestKg),
			Codec.LONG.optionalFieldOf("first_caught_day", 0L).forGetter(SpeciesRecord::firstCaughtDay)
	).apply(instance, SpeciesRecord::new));

	public SpeciesRecord withCatch(float weightKg) {
		return new SpeciesRecord(this.caught + 1, Math.max(this.bestKg, weightKg), this.firstCaughtDay);
	}
}
