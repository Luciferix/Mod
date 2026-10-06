package com.example.examplemod.fishing.gear;

import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

/**
 * Marks an item as bait. The bait type decides which species are attracted (see
 * {@link com.example.examplemod.fishing.species.BaitPreferences}); potency scales how strongly.
 * Natural bait is used up on each catch, artificial lures are reusable.
 */
public record Bait(Identifier type, float potency, boolean reusable) implements TooltipProvider {
	public static final Codec<Bait> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("type").forGetter(Bait::type),
			Codec.floatRange(0.0F, 10.0F).optionalFieldOf("potency", 1.0F).forGetter(Bait::potency),
			Codec.BOOL.optionalFieldOf("reusable", false).forGetter(Bait::reusable)
	).apply(instance, Bait::new));

	public static final StreamCodec<ByteBuf, Bait> STREAM_CODEC = StreamCodec.composite(
			Identifier.STREAM_CODEC, Bait::type,
			ByteBufCodecs.FLOAT, Bait::potency,
			ByteBufCodecs.BOOL, Bait::reusable,
			Bait::new
	);

	public static MutableComponent typeName(Identifier type) {
		return Component.translatable("bait_type." + type.getNamespace() + "." + type.getPath().replace('/', '.'));
	}

	@Override
	public void addToTooltip(Item.TooltipContext context, Consumer<Component> tooltip, TooltipFlag flag, DataComponentGetter components) {
		tooltip.accept(Component.translatable("fishing.examplemod.bait.type", typeName(this.type)).withStyle(ChatFormatting.GRAY));
		if (this.reusable) {
			tooltip.accept(Component.translatable("fishing.examplemod.bait.reusable", Math.round(this.potency * 100.0F)).withStyle(ChatFormatting.BLUE));
		}

		tooltip.accept(Component.translatable("fishing.examplemod.bait.attach_hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
	}
}
