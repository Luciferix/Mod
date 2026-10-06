package com.example.examplemod.fishing.species;

import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

/**
 * Which bait types attract a species and how strongly. An affinity of 0 (or a bait type that
 * isn't listed) means the species ignores that bait; {@code unbaited} applies to a bare hook.
 */
public record BaitPreferences(Map<Identifier, Float> affinities, float unbaited) {
	public static final BaitPreferences NONE = new BaitPreferences(Map.of(), 0.0F);

	private static final Codec<Float> AFFINITY_CODEC = Codec.floatRange(0.0F, 100.0F);

	public static final Codec<BaitPreferences> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.unboundedMap(Identifier.CODEC, AFFINITY_CODEC).optionalFieldOf("affinities", Map.of()).forGetter(BaitPreferences::affinities),
			AFFINITY_CODEC.optionalFieldOf("unbaited", 0.0F).forGetter(BaitPreferences::unbaited)
	).apply(instance, BaitPreferences::new));

	public BaitPreferences {
		affinities = Map.copyOf(affinities);
	}

	/** Affinity for the given bait type, or for a bare hook when empty. */
	public float affinity(Optional<Identifier> baitType) {
		return baitType.map(type -> this.affinities.getOrDefault(type, 0.0F)).orElse(this.unbaited);
	}
}
