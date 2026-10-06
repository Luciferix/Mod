package com.example.examplemod.fishing.gear;

import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

/** Marks an item as rod tackle (hook, line or reel) and holds the bonuses it gives. */
public record Tackle(TackleSlot slot, GearModifiers modifiers) implements TooltipProvider {
	public static final Codec<Tackle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			TackleSlot.CODEC.fieldOf("slot").forGetter(Tackle::slot),
			GearModifiers.CODEC.fieldOf("modifiers").forGetter(Tackle::modifiers)
	).apply(instance, Tackle::new));

	public static final StreamCodec<ByteBuf, Tackle> STREAM_CODEC = StreamCodec.composite(
			TackleSlot.STREAM_CODEC, Tackle::slot,
			GearModifiers.STREAM_CODEC, Tackle::modifiers,
			Tackle::new
	);

	@Override
	public void addToTooltip(Item.TooltipContext context, Consumer<Component> tooltip, TooltipFlag flag, DataComponentGetter components) {
		tooltip.accept(Component.translatable("fishing.examplemod.tackle.slot", this.slot.displayName()).withStyle(ChatFormatting.GRAY));
		this.modifiers.addModifierLines(tooltip);
		tooltip.accept(Component.translatable("fishing.examplemod.tackle.attach_hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
	}
}
