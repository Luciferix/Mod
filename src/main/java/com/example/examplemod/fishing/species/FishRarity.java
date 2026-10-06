package com.example.examplemod.fishing.species;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * How rare a species is. Drives the bite indicator color, the base difficulty of the fight
 * and how often the species is rolled (see {@link com.example.examplemod.fishing.FishingBalance}).
 */
public enum FishRarity implements StringRepresentable {
	COMMON("common", 0xFFFFFF),
	UNCOMMON("uncommon", 0x55FF55),
	RARE("rare", 0x55AAFF),
	EPIC("epic", 0xC060FF),
	LEGENDARY("legendary", 0xFFD84A);

	private static final FishRarity[] VALUES = values();

	public static final Codec<FishRarity> CODEC = StringRepresentable.fromEnum(FishRarity::values);
	public static final StreamCodec<ByteBuf, FishRarity> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(FishRarity::byId, FishRarity::ordinal);

	private final String name;
	private final int color;

	FishRarity(String name, int color) {
		this.name = name;
		this.color = color;
	}

	public static FishRarity byId(int id) {
		return VALUES[Math.clamp(id, 0, VALUES.length - 1)];
	}

	/** RGB color used for the bite indicator and for rarity text. */
	public int color() {
		return this.color;
	}

	public boolean isAtLeast(FishRarity other) {
		return this.ordinal() >= other.ordinal();
	}

	public MutableComponent displayName() {
		return Component.translatable("fishing.examplemod.rarity." + this.name).withColor(this.color);
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}
