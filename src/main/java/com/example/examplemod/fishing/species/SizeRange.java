package com.example.examplemod.fishing.species;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/** The weight range of a species, in kilograms. */
public record SizeRange(float minKg, float maxKg) {
	private static final Codec<SizeRange> UNCHECKED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.floatRange(0.001F, 10_000F).fieldOf("min_kg").forGetter(SizeRange::minKg),
			Codec.floatRange(0.001F, 10_000F).fieldOf("max_kg").forGetter(SizeRange::maxKg)
	).apply(instance, SizeRange::new));

	public static final Codec<SizeRange> CODEC = UNCHECKED_CODEC.validate(range -> range.minKg < range.maxKg
			? DataResult.success(range)
			: DataResult.error(() -> "min_kg must be smaller than max_kg: " + range));

	public static final StreamCodec<ByteBuf, SizeRange> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, SizeRange::minKg,
			ByteBufCodecs.FLOAT, SizeRange::maxKg,
			SizeRange::new
	);

	/** Weight at a normalized position in the range (0 = smallest, 1 = largest). */
	public float weightAt(float normalized) {
		return Mth.lerp(Mth.clamp(normalized, 0.0F, 1.0F), this.minKg, this.maxKg);
	}

	/** Normalized position of a weight in the range (0 = smallest, 1 = largest). */
	public float normalize(float weightKg) {
		return Mth.clamp((weightKg - this.minKg) / (this.maxKg - this.minKg), 0.0F, 1.0F);
	}
}
