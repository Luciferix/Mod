package com.example.examplemod.fishing.gear;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;

/** Finds the rod a player is fishing with. All rods are {@link FishingRodItem}s, vanilla or not. */
public final class FishingRods {
	private FishingRods() {
	}

	public static boolean isRod(ItemStack stack) {
		return stack.getItem() instanceof FishingRodItem;
	}

	/** The hand holding a rod, preferring the main hand like vanilla. */
	public static InteractionHand rodHand(Player player) {
		return isRod(player.getMainHandItem()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
	}

	/** The rod in use, or an empty stack if the player holds none. */
	public static ItemStack heldRod(Player player) {
		ItemStack stack = player.getItemInHand(rodHand(player));
		return isRod(stack) ? stack : ItemStack.EMPTY;
	}
}
