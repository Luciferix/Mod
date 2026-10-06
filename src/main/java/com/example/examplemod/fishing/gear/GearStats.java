package com.example.examplemod.fishing.gear;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.util.Mth;

import com.example.examplemod.fishing.registry.FishingComponents;

/**
 * The combined stats of a rod: its base stats plus everything attached to it.
 * Enchantments (Luck of the Sea) are added separately when a fish is rolled.
 */
public record GearStats(
		float catchDamage,
		float luck,
		float sizeBonus,
		float treasureChance,
		float lineStrength,
		float pullResistance,
		float fightTimeMultiplier
) {
	private static final float MAX_PULL_RESISTANCE = 0.9F;

	public static GearStats of(DataComponentGetter rod) {
		Map<GearStat, Float> totals = new EnumMap<>(GearStat.class);
		FishingRodStats rodStats = rod.get(FishingComponents.ROD_STATS);
		add(totals, rodStats != null ? rodStats.base() : FishingRodStats.FALLBACK.base());

		RodLoadout loadout = rod.get(FishingComponents.ROD_LOADOUT);
		if (loadout != null) {
			for (TackleSlot slot : TackleSlot.values()) {
				loadout.get(slot).ifPresent(template -> {
					Tackle tackle = template.get(FishingComponents.TACKLE);
					if (tackle != null) {
						add(totals, tackle.modifiers());
					}
				});
			}
		}

		float flatDamage = totals.getOrDefault(GearStat.CATCH_DAMAGE, 0.0F);
		float damagePercent = totals.getOrDefault(GearStat.CATCH_DAMAGE_PERCENT, 0.0F);
		return new GearStats(
				Math.max(1.0F, flatDamage * (1.0F + damagePercent)),
				totals.getOrDefault(GearStat.LUCK, 0.0F),
				Math.max(0.0F, totals.getOrDefault(GearStat.SIZE_BONUS, 0.0F)),
				Math.max(0.0F, totals.getOrDefault(GearStat.TREASURE_CHANCE, 0.0F)),
				Math.max(1.0F, totals.getOrDefault(GearStat.LINE_STRENGTH, 0.0F)),
				Mth.clamp(totals.getOrDefault(GearStat.PULL_RESISTANCE, 0.0F), 0.0F, MAX_PULL_RESISTANCE),
				Math.max(0.25F, 1.0F + totals.getOrDefault(GearStat.FIGHT_TIME, 0.0F))
		);
	}

	private static void add(Map<GearStat, Float> totals, GearModifiers modifiers) {
		modifiers.values().forEach((stat, value) -> totals.merge(stat, value, Float::sum));
	}

	public GearStats withExtraLuck(float extraLuck) {
		return new GearStats(this.catchDamage, this.luck + extraLuck, this.sizeBonus, this.treasureChance, this.lineStrength, this.pullResistance, this.fightTimeMultiplier);
	}
}
