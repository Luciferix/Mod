package com.example.examplemod.fishing.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.example.examplemod.fishing.FishingFeature;
import com.example.examplemod.fishing.catching.HookEvent;
import com.example.examplemod.fishing.catching.HookSession;
import com.example.examplemod.fishing.species.FishRarity;

/**
 * Server to client: the catch state of a bobber, sent to everyone who can see it whenever
 * something happens (bite, hook set, reel, pull, catch, escape). The species stays secret until
 * the fish is landed; only its rarity is shared.
 */
public record HookStatePayload(
		int hookId,
		HookSession.Phase phase,
		FishRarity rarity,
		float hp,
		float maxHp,
		int ticksLeft,
		int maxTicks,
		boolean overweight,
		HookEvent event
) implements CustomPacketPayload {
	public static final Type<HookStatePayload> TYPE = new Type<>(FishingFeature.id("hook_state"));

	public static final StreamCodec<FriendlyByteBuf, HookStatePayload> STREAM_CODEC = CustomPacketPayload.codec(HookStatePayload::write, HookStatePayload::read);

	public static HookStatePayload of(int hookId, HookSession session, HookEvent event) {
		return new HookStatePayload(hookId, session.phase(), session.rarity(), session.hp(), session.maxHp(),
				session.ticksLeft(), session.maxTicks(), session.overweight(), event);
	}

	private static HookStatePayload read(FriendlyByteBuf buf) {
		return new HookStatePayload(
				buf.readVarInt(),
				HookSession.Phase.STREAM_CODEC.decode(buf),
				FishRarity.STREAM_CODEC.decode(buf),
				buf.readFloat(),
				buf.readFloat(),
				buf.readVarInt(),
				buf.readVarInt(),
				buf.readBoolean(),
				HookEvent.STREAM_CODEC.decode(buf)
		);
	}

	private void write(FriendlyByteBuf buf) {
		buf.writeVarInt(this.hookId);
		HookSession.Phase.STREAM_CODEC.encode(buf, this.phase);
		FishRarity.STREAM_CODEC.encode(buf, this.rarity);
		buf.writeFloat(this.hp);
		buf.writeFloat(this.maxHp);
		buf.writeVarInt(this.ticksLeft);
		buf.writeVarInt(this.maxTicks);
		buf.writeBoolean(this.overweight);
		HookEvent.STREAM_CODEC.encode(buf, this.event);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
