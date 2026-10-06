package com.example.examplemod.fishing.gear;

import java.util.Optional;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

import net.fabricmc.fabric.api.item.v1.ItemClickBehaviorCallback;
import net.fabricmc.fabric.api.util.EventResult;

import com.example.examplemod.fishing.registry.FishingComponents;
import com.example.examplemod.fishing.registry.FishingSounds;

/**
 * Bundle-style tackle handling in any inventory: right-click a hook, line, reel or bait onto a
 * fishing rod to attach it (swapping out what was there), and right-click a rod with an empty
 * cursor to take attachments off again (bait first, then hook, line and reel).
 */
public final class TackleInteractions {
	private static final int MAX_BAIT = 64;

	private TackleInteractions() {
	}

	public static void init() {
		ItemClickBehaviorCallback.EVENT.register(TackleInteractions::onClick);
	}

	private static EventResult onClick(ItemStack hovered, Slot slot, ItemStack carried, SlotAccess carriedAccess, ClickAction action, Player player) {
		if (action != ClickAction.SECONDARY || !FishingRods.isRod(hovered) || hovered.getCount() != 1) {
			return EventResult.PASS;
		}

		boolean handled = carried.isEmpty() ? detach(hovered, carriedAccess, player) : attach(hovered, carried, carriedAccess, player);
		if (handled) {
			slot.setChanged();
			return EventResult.DENY;
		}

		return EventResult.PASS;
	}

	private static boolean attach(ItemStack rod, ItemStack carried, SlotAccess carriedAccess, Player player) {
		RodLoadout loadout = loadout(rod);
		Tackle tackle = carried.get(FishingComponents.TACKLE);
		if (tackle != null) {
			Optional<ItemStackTemplate> previous = loadout.get(tackle.slot());
			if (previous.isPresent() && carried.getCount() > 1) {
				// Swapping needs a free cursor for the old piece.
				return false;
			}

			rod.set(FishingComponents.ROD_LOADOUT, loadout.with(tackle.slot(), Optional.of(ItemStackTemplate.fromNonEmptyStack(carried, 1))));
			carried.shrink(1);
			previous.ifPresent(old -> carriedAccess.set(old.create()));
			playSound(player, FishingSounds.TACKLE_ATTACH);
			return true;
		}

		if (carried.get(FishingComponents.BAIT) != null) {
			Optional<ItemStack> loaded = loadout.bait().map(ItemStackTemplate::create);
			if (loaded.isPresent() && ItemStack.isSameItemSameComponents(loaded.get(), carried)) {
				int moved = Math.min(carried.getCount(), MAX_BAIT - loaded.get().getCount());
				if (moved <= 0) {
					return false;
				}

				rod.set(FishingComponents.ROD_LOADOUT, loadout.withBait(Optional.of(ItemStackTemplate.fromNonEmptyStack(loaded.get(), loaded.get().getCount() + moved))));
				carried.shrink(moved);
			} else {
				rod.set(FishingComponents.ROD_LOADOUT, loadout.withBait(Optional.of(ItemStackTemplate.fromNonEmptyStack(carried))));
				carriedAccess.set(loaded.orElse(ItemStack.EMPTY));
			}

			playSound(player, FishingSounds.TACKLE_ATTACH);
			return true;
		}

		return false;
	}

	private static boolean detach(ItemStack rod, SlotAccess carriedAccess, Player player) {
		RodLoadout loadout = loadout(rod);
		if (loadout.bait().isPresent()) {
			rod.set(FishingComponents.ROD_LOADOUT, loadout.withBait(Optional.empty()));
			carriedAccess.set(loadout.bait().get().create());
			playSound(player, FishingSounds.TACKLE_DETACH);
			return true;
		}

		for (TackleSlot tackleSlot : TackleSlot.values()) {
			Optional<ItemStackTemplate> attached = loadout.get(tackleSlot);
			if (attached.isPresent()) {
				rod.set(FishingComponents.ROD_LOADOUT, loadout.with(tackleSlot, Optional.empty()));
				carriedAccess.set(attached.get().create());
				playSound(player, FishingSounds.TACKLE_DETACH);
				return true;
			}
		}

		return false;
	}

	private static RodLoadout loadout(ItemStack rod) {
		RodLoadout loadout = rod.get(FishingComponents.ROD_LOADOUT);
		return loadout != null ? loadout : RodLoadout.EMPTY;
	}

	private static void playSound(Player player, SoundEvent sound) {
		player.playSound(sound, 0.8F, 0.9F + player.getRandom().nextFloat() * 0.2F);
	}
}
