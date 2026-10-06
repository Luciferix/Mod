package com.example.examplemod.fishing.journal;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * A player's fishing progress, shown in the Fishing Index. Stored on the player (persistent,
 * kept on death) and synced to that player's client. Immutable: every update returns a copy.
 *
 * @param species    catches per species id
 * @param treasures  how often each treasure item (by item id) was found
 */
public record FishingJournal(Map<Identifier, SpeciesRecord> species, Map<Identifier, Integer> treasures) {
	public static final FishingJournal EMPTY = new FishingJournal(Map.of(), Map.of());

	public static final Codec<FishingJournal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.unboundedMap(Identifier.CODEC, SpeciesRecord.CODEC).optionalFieldOf("species", Map.of()).forGetter(FishingJournal::species),
			Codec.unboundedMap(Identifier.CODEC, Codec.INT).optionalFieldOf("treasures", Map.of()).forGetter(FishingJournal::treasures)
	).apply(instance, FishingJournal::new));

	public static final StreamCodec<ByteBuf, FishingJournal> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

	public FishingJournal {
		species = Map.copyOf(species);
		treasures = Map.copyOf(treasures);
	}

	public boolean hasCaught(Identifier speciesId) {
		return this.species.containsKey(speciesId);
	}

	public SpeciesRecord record(Identifier speciesId) {
		return this.species.get(speciesId);
	}

	public int totalCaught() {
		return this.species.values().stream().mapToInt(SpeciesRecord::caught).sum();
	}

	public int totalTreasures() {
		return this.treasures.values().stream().mapToInt(Integer::intValue).sum();
	}

	public FishingJournal withCatch(Identifier speciesId, float weightKg, long day) {
		Map<Identifier, SpeciesRecord> updated = new HashMap<>(this.species);
		updated.merge(speciesId, new SpeciesRecord(1, weightKg, day), (old, ignored) -> old.withCatch(weightKg));
		return new FishingJournal(updated, this.treasures);
	}

	public FishingJournal withTreasure(Identifier itemId) {
		Map<Identifier, Integer> updated = new HashMap<>(this.treasures);
		updated.merge(itemId, 1, Integer::sum);
		return new FishingJournal(this.species, updated);
	}
}
