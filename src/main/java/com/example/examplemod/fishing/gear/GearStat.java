package com.example.examplemod.fishing.gear;

import java.util.Locale;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** A fishing equipment stat. Rods provide base values; tackle adds to them. */
public enum GearStat implements StringRepresentable {
	/** Flat HP removed from the fish per reel action. */
	CATCH_DAMAGE("catch_damage", Format.FLAT),
	/** Percentage bonus applied on top of the summed Catch Damage. */
	CATCH_DAMAGE_PERCENT("catch_damage_percent", Format.PERCENT),
	/** Shifts the rarity roll towards rarer fish. */
	LUCK("luck", Format.FLAT),
	/** Shifts the size roll towards larger specimens. */
	SIZE_BONUS("size_bonus", Format.PERCENT),
	/** Chance to also find treasure on a successful catch. */
	TREASURE_CHANCE("treasure_chance", Format.PERCENT_PRECISE),
	/** Heaviest fish (kg) the gear can land without the overweight penalty. */
	LINE_STRENGTH("line_strength", Format.KILOGRAMS),
	/** Reduces how much HP the fish regains when it pulls back. */
	PULL_RESISTANCE("pull_resistance", Format.PERCENT),
	/** Extra time on the fight timer. */
	FIGHT_TIME("fight_time", Format.PERCENT);

	public static final Codec<GearStat> CODEC = StringRepresentable.fromEnum(GearStat::values);
	public static final StreamCodec<ByteBuf, GearStat> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(i -> values()[i], GearStat::ordinal);

	private final String name;
	private final Format format;

	GearStat(String name, Format format) {
		this.name = name;
		this.format = format;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}

	public MutableComponent displayName() {
		return Component.translatable("fishing.examplemod.stat." + this.name);
	}

	/** "+3 Catch Damage" style line for a tackle modifier. */
	public MutableComponent modifierLine(float value) {
		String sign = value >= 0 ? "+" : "";
		ChatFormatting color = value >= 0 ? ChatFormatting.BLUE : ChatFormatting.RED;
		return Component.translatable("fishing.examplemod.stat.modifier", sign + this.format.format(value), this.displayName()).withStyle(color);
	}

	/** "Catch Damage: 7" style line for a total. */
	public MutableComponent totalLine(float value) {
		return Component.translatable("fishing.examplemod.stat.total", this.displayName(), this.format.format(value)).withStyle(ChatFormatting.DARK_GREEN);
	}

	private enum Format {
		FLAT,
		PERCENT,
		PERCENT_PRECISE,
		KILOGRAMS;

		String format(float value) {
			return switch (this) {
				case FLAT -> value == Math.rint(value) ? Integer.toString(Math.round(value)) : String.format(Locale.ROOT, "%.1f", value);
				case PERCENT -> Math.round(value * 100.0F) + "%";
				case PERCENT_PRECISE -> String.format(Locale.ROOT, "%.1f%%", value * 100.0F);
				case KILOGRAMS -> String.format(Locale.ROOT, "%.0f kg", value);
			};
		}
	}
}
