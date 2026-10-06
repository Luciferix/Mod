package com.example.examplemod.fishing.gear;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;

/** The tackle and bait attached to a fishing rod. Each slot holds one item; bait keeps its count. */
public record RodLoadout(
		Optional<ItemStackTemplate> hook,
		Optional<ItemStackTemplate> line,
		Optional<ItemStackTemplate> reel,
		Optional<ItemStackTemplate> bait
) {
	public static final RodLoadout EMPTY = new RodLoadout(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

	public static final Codec<RodLoadout> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ItemStackTemplate.CODEC.optionalFieldOf("hook").forGetter(RodLoadout::hook),
			ItemStackTemplate.CODEC.optionalFieldOf("line").forGetter(RodLoadout::line),
			ItemStackTemplate.CODEC.optionalFieldOf("reel").forGetter(RodLoadout::reel),
			ItemStackTemplate.CODEC.optionalFieldOf("bait").forGetter(RodLoadout::bait)
	).apply(instance, RodLoadout::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, RodLoadout> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC), RodLoadout::hook,
			ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC), RodLoadout::line,
			ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC), RodLoadout::reel,
			ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC), RodLoadout::bait,
			RodLoadout::new
	);

	public Optional<ItemStackTemplate> get(TackleSlot slot) {
		return switch (slot) {
			case HOOK -> this.hook;
			case LINE -> this.line;
			case REEL -> this.reel;
		};
	}

	public RodLoadout with(TackleSlot slot, Optional<ItemStackTemplate> tackle) {
		return switch (slot) {
			case HOOK -> new RodLoadout(tackle, this.line, this.reel, this.bait);
			case LINE -> new RodLoadout(this.hook, tackle, this.reel, this.bait);
			case REEL -> new RodLoadout(this.hook, this.line, tackle, this.bait);
		};
	}

	public RodLoadout withBait(Optional<ItemStackTemplate> bait) {
		return new RodLoadout(this.hook, this.line, this.reel, bait);
	}

	public boolean isEmpty() {
		return this.hook.isEmpty() && this.line.isEmpty() && this.reel.isEmpty() && this.bait.isEmpty();
	}
}
