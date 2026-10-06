package com.example.examplemod.fishing.catching;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import com.example.examplemod.fishing.species.FishRarity;
import com.example.examplemod.fishing.species.SizeClass;

/** Set on every fish landed with the reworked fishing: which species, how heavy, and who caught it. */
public record CaughtFish(Identifier species, float weightKg, float normalizedSize, FishRarity rarity, Optional<String> angler)
		implements TooltipProvider {
	public static final Codec<CaughtFish> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("species").forGetter(CaughtFish::species),
			Codec.FLOAT.fieldOf("weight_kg").forGetter(CaughtFish::weightKg),
			Codec.floatRange(0.0F, 1.0F).fieldOf("normalized_size").forGetter(CaughtFish::normalizedSize),
			FishRarity.CODEC.fieldOf("rarity").forGetter(CaughtFish::rarity),
			Codec.STRING.optionalFieldOf("angler").forGetter(CaughtFish::angler)
	).apply(instance, CaughtFish::new));

	public static final StreamCodec<ByteBuf, CaughtFish> STREAM_CODEC = StreamCodec.composite(
			Identifier.STREAM_CODEC, CaughtFish::species,
			ByteBufCodecs.FLOAT, CaughtFish::weightKg,
			ByteBufCodecs.FLOAT, CaughtFish::normalizedSize,
			FishRarity.STREAM_CODEC, CaughtFish::rarity,
			ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), CaughtFish::angler,
			CaughtFish::new
	);

	public SizeClass sizeClass() {
		return SizeClass.of(this.normalizedSize);
	}

	@Override
	public void addToTooltip(Item.TooltipContext context, Consumer<Component> tooltip, TooltipFlag flag, DataComponentGetter components) {
		tooltip.accept(Component.translatable("fishing.examplemod.fish.weight", formatWeight(this.weightKg), this.sizeClass().displayName())
				.withStyle(ChatFormatting.GRAY));
		tooltip.accept(this.rarity.displayName());
		this.angler.ifPresent(name -> tooltip.accept(Component.translatable("fishing.examplemod.fish.angler", name)
				.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
	}

	/** "0.045 kg", "4.72 kg" or "312.4 kg". */
	public static String formatWeight(float weightKg) {
		if (weightKg < 0.1F) {
			return String.format(Locale.ROOT, "%.3f kg", weightKg);
		} else if (weightKg < 100.0F) {
			return String.format(Locale.ROOT, "%.2f kg", weightKg);
		} else {
			return String.format(Locale.ROOT, "%.1f kg", weightKg);
		}
	}
}
