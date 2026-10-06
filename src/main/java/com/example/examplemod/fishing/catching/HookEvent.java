package com.example.examplemod.fishing.catching;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** What just happened to a bobber; sent with every state sync so clients can play feedback. */
public enum HookEvent {
	SYNC,
	BITE,
	HOOK_SET,
	REEL,
	REEL_CRITICAL,
	PULL,
	LOW_HP,
	CAUGHT,
	ESCAPED,
	LEFT;

	private static final HookEvent[] VALUES = values();

	public static final StreamCodec<ByteBuf, HookEvent> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(i -> VALUES[i], HookEvent::ordinal);
}
