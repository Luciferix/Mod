package com.example.examplemod.fishing.gear;

import java.util.Optional;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import com.example.examplemod.fishing.registry.FishingComponents;

/** Base stats of a fishing rod. Its tooltip also lists the attached tackle and the combined stats. */
public record FishingRodStats(GearModifiers base) implements TooltipProvider {
	/** Used for rods without the component, e.g. from other mods. Matches the vanilla rod. */
	public static final FishingRodStats FALLBACK = new FishingRodStats(GearModifiers.builder()
			.set(GearStat.CATCH_DAMAGE, 4.0F)
			.set(GearStat.TREASURE_CHANCE, 0.005F)
			.set(GearStat.LINE_STRENGTH, 6.0F)
			.build());

	public static final Codec<FishingRodStats> CODEC = GearModifiers.CODEC.xmap(FishingRodStats::new, FishingRodStats::base);
	public static final StreamCodec<ByteBuf, FishingRodStats> STREAM_CODEC = GearModifiers.STREAM_CODEC.map(FishingRodStats::new, FishingRodStats::base);

	@Override
	public void addToTooltip(Item.TooltipContext context, Consumer<Component> tooltip, TooltipFlag flag, DataComponentGetter components) {
		RodLoadout loadout = components.get(FishingComponents.ROD_LOADOUT);
		if (loadout == null) {
			loadout = RodLoadout.EMPTY;
		}

		for (TackleSlot slot : TackleSlot.values()) {
			tooltip.accept(slotLine(slot.displayName(), loadout.get(slot)));
		}

		tooltip.accept(slotLine(Component.translatable("fishing.examplemod.slot.bait"), loadout.bait()));

		GearStats stats = GearStats.of(components);
		tooltip.accept(GearStat.CATCH_DAMAGE.totalLine(stats.catchDamage()));
		tooltip.accept(GearStat.LUCK.totalLine(stats.luck()));
		tooltip.accept(GearStat.SIZE_BONUS.totalLine(stats.sizeBonus()));
		tooltip.accept(GearStat.TREASURE_CHANCE.totalLine(stats.treasureChance()));
		tooltip.accept(GearStat.LINE_STRENGTH.totalLine(stats.lineStrength()));
		if (stats.pullResistance() > 0.0F) {
			tooltip.accept(GearStat.PULL_RESISTANCE.totalLine(stats.pullResistance()));
		}

		if (stats.fightTimeMultiplier() != 1.0F) {
			tooltip.accept(GearStat.FIGHT_TIME.totalLine(stats.fightTimeMultiplier() - 1.0F));
		}

		if (loadout.isEmpty()) {
			tooltip.accept(Component.translatable("fishing.examplemod.rod.attach_hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
		}
	}

	private static Component slotLine(MutableComponent slotName, Optional<ItemStackTemplate> attached) {
		Component value = attached
				.map(template -> {
					ItemStack stack = template.create();
					Component name = stack.getHoverName();
					return stack.getCount() > 1 ? Component.translatable("fishing.examplemod.rod.slot_count", name, stack.getCount()) : name;
				})
				.orElseGet(() -> Component.translatable("fishing.examplemod.rod.slot_empty").withStyle(ChatFormatting.DARK_GRAY));
		return Component.translatable("fishing.examplemod.rod.slot", slotName, value).withStyle(ChatFormatting.GRAY);
	}
}
