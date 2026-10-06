package com.example.examplemod.fishing.species;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** A readable label for where a catch sits in its species' size range. */
public enum SizeClass {
	TINY("tiny", 0.15F, ChatFormatting.GRAY),
	SMALL("small", 0.40F, ChatFormatting.GRAY),
	AVERAGE("average", 0.70F, ChatFormatting.WHITE),
	LARGE("large", 0.90F, ChatFormatting.AQUA),
	HUGE("huge", 0.98F, ChatFormatting.LIGHT_PURPLE),
	TROPHY("trophy", Float.POSITIVE_INFINITY, ChatFormatting.GOLD);

	private final String name;
	private final float upperBound;
	private final ChatFormatting format;

	SizeClass(String name, float upperBound, ChatFormatting format) {
		this.name = name;
		this.upperBound = upperBound;
		this.format = format;
	}

	public static SizeClass of(float normalizedSize) {
		for (SizeClass sizeClass : values()) {
			if (normalizedSize < sizeClass.upperBound) {
				return sizeClass;
			}
		}

		return TROPHY;
	}

	public MutableComponent displayName() {
		return Component.translatable("fishing.examplemod.size_class." + this.name).withStyle(this.format);
	}
}
