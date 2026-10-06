package com.example.examplemod.fishing.gear;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** An immutable set of stat values, used for both rod base stats and tackle bonuses. */
public record GearModifiers(Map<GearStat, Float> values) {
	public static final GearModifiers NONE = new GearModifiers(Map.of());

	public static final Codec<GearModifiers> CODEC = Codec.unboundedMap(GearStat.CODEC, Codec.FLOAT)
			.xmap(GearModifiers::new, GearModifiers::values);

	public static final StreamCodec<ByteBuf, GearModifiers> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

	public GearModifiers {
		values = Map.copyOf(values);
	}

	public static Builder builder() {
		return new Builder();
	}

	public float get(GearStat stat) {
		return this.values.getOrDefault(stat, 0.0F);
	}

	/** Adds one tooltip line per non-zero modifier, in stat order. */
	public void addModifierLines(Consumer<Component> tooltip) {
		for (GearStat stat : GearStat.values()) {
			float value = this.get(stat);
			if (value != 0.0F) {
				tooltip.accept(stat.modifierLine(value));
			}
		}
	}

	public static final class Builder {
		private final Map<GearStat, Float> values = new EnumMap<>(GearStat.class);

		private Builder() {
		}

		public Builder set(GearStat stat, float value) {
			this.values.put(stat, value);
			return this;
		}

		public GearModifiers build() {
			return new GearModifiers(this.values);
		}
	}
}
