package com.example.examplemod.fishing.catching;

import net.minecraft.world.entity.projectile.FishingHook;

/** Added to {@link FishingHook} by {@code FishingHookMixin}. */
public interface FishingHookAccess {
	HookSession examplemod$session();

	/** Luck of the Sea level the hook was cast with. */
	int examplemod$enchantmentLuck();

	/** Keeps vanilla's bite state (bobber pulled under) while a fish bites or fights. */
	void examplemod$holdBite(int ticks);

	/** Clears vanilla's bite timers so the hook goes back to waiting for a new fish. */
	void examplemod$resetToWaiting();

	static HookSession session(FishingHook hook) {
		return ((FishingHookAccess) hook).examplemod$session();
	}

	static FishingHookAccess of(FishingHook hook) {
		return (FishingHookAccess) hook;
	}
}
