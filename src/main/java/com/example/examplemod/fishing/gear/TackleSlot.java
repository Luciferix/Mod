package com.example.examplemod.fishing.gear;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** Where a piece of tackle attaches on a rod. Bait has its own slot and is handled separately. */
public enum TackleSlot implements StringRepresentable {
	HOOK("hook"),
	LINE("line"),
	REEL("reel");

	public static final Codec<TackleSlot> CODEC = StringRepresentable.fromEnum(TackleSlot::values);
	public static final StreamCodec<ByteBuf, TackleSlot> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(i -> values()[i], TackleSlot::ordinal);

	private final String name;

	TackleSlot(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}

	public MutableComponent displayName() {
		return Component.translatable("fishing.examplemod.slot." + this.name);
	}
}
