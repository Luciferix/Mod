package com.example.examplemod.fishing.catching;

import java.util.Optional;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;

import com.example.examplemod.fishing.FishingBalance;
import com.example.examplemod.fishing.gear.FishingRods;
import com.example.examplemod.fishing.gear.GearStats;
import com.example.examplemod.fishing.network.FishingNetworking;
import com.example.examplemod.fishing.registry.FishingSounds;

/**
 * Server-side flow of a catch, driven by the fishing hook and rod mixins:
 * bite (rarity revealed) → hook set by a click → fight (clicks reel, the fish pulls back,
 * a timer runs) → landed ({@link CatchRewards}) or escaped.
 */
public final class FishingController {
	private static final int SYNC_INTERVAL = 20;

	private FishingController() {
	}

	/** Vanilla just made a fish bite. Rolls which fish it is; if none fits, vanilla handles the bite. */
	public static void onVanillaBite(FishingHook hook) {
		HookSession session = FishingHookAccess.session(hook);
		if (session.isEngaged() || !(hook.level() instanceof ServerLevel level) || !(hook.getPlayerOwner() instanceof ServerPlayer player)) {
			return;
		}

		ItemStack rod = FishingRods.heldRod(player);
		if (rod.isEmpty()) {
			return;
		}

		float extraLuck = FishingHookAccess.of(hook).examplemod$enchantmentLuck() + player.getLuck();
		GearStats gear = GearStats.of(rod).withExtraLuck(extraLuck);
		Optional<FishEncounter> encounter = EncounterRoller.roll(level, hook, rod, gear);
		if (encounter.isEmpty()) {
			return;
		}

		int window = FishingBalance.biteWindowTicks(encounter.get().rarity());
		session.startBite(encounter.get(), gear, window);
		FishingHookAccess.of(hook).examplemod$holdBite(window);
		FishingNetworking.broadcast(hook, HookEvent.BITE);
		playSound(level, hook, FishingSounds.BITE, 0.8F, 1.0F);
	}

	public static void serverTick(FishingHook hook) {
		HookSession session = FishingHookAccess.session(hook);
		switch (session.phase()) {
			case BITING -> {
				if (session.tickBiteWindow()) {
					fishLeaves(hook, session);
				} else if (hook.tickCount % SYNC_INTERVAL == 0) {
					FishingNetworking.broadcast(hook, HookEvent.SYNC);
				}
			}
			case FIGHTING -> tickFight(hook, session);
			case WAITING -> {
			}
		}
	}

	/** A right-click with the rod while a fish bites or fights. */
	public static void onRodUse(ServerPlayer player, FishingHook hook) {
		HookSession session = FishingHookAccess.session(hook);
		switch (session.phase()) {
			case BITING -> setHook(hook, session);
			case FIGHTING -> reel(player, hook, session);
			case WAITING -> {
			}
		}
	}

	/** The bobber disappeared (player switched items, walked away, died...). */
	public static void onHookRemoved(FishingHook hook) {
		HookSession session = FishingHookAccess.session(hook);
		if (session.phase() == HookSession.Phase.FIGHTING && hook.getPlayerOwner() instanceof ServerPlayer player) {
			player.sendOverlayMessage(Component.translatable("fishing.examplemod.line_snapped", session.rarity().displayName()));
		}

		session.reset();
	}

	private static void setHook(FishingHook hook, HookSession session) {
		session.startFight(hook.getRandom());
		FishingNetworking.broadcast(hook, HookEvent.HOOK_SET);
		if (hook.level() instanceof ServerLevel level) {
			playSound(level, hook, FishingSounds.HOOK_SET, 1.0F, 1.0F);
			level.sendParticles(ParticleTypes.SPLASH, hook.getX(), hook.getY() + 0.3, hook.getZ(), 12, 0.2, 0.05, 0.2, 0.1);
		}
	}

	private static void reel(ServerPlayer player, FishingHook hook, HookSession session) {
		ServerLevel level = player.level();
		if (!session.tryReelAction(level.getServer().getTickCount())) {
			return;
		}

		RandomSource random = hook.getRandom();
		float damage = session.gear().catchDamage() * (1.0F + (random.nextFloat() * 2.0F - 1.0F) * FishingBalance.DAMAGE_SPREAD);
		boolean critical = random.nextFloat() < FishingBalance.CRITICAL_REEL_CHANCE;
		if (critical) {
			damage *= FishingBalance.CRITICAL_REEL_MULTIPLIER;
		}

		session.damage(damage);
		if (session.hp() <= 0.0F) {
			CatchRewards.land(player, hook, session);
			return;
		}

		HookEvent event = session.checkLowHp() ? HookEvent.LOW_HP : critical ? HookEvent.REEL_CRITICAL : HookEvent.REEL;
		FishingNetworking.broadcast(hook, event);
		float pitch = 0.8F + 0.6F * (1.0F - session.hpFraction());
		playSound(level, hook, critical ? FishingSounds.REEL_CRITICAL : FishingSounds.REEL, 0.6F, pitch);
		if (event == HookEvent.LOW_HP) {
			playSound(level, hook, FishingSounds.LOW_HP, 0.8F, 1.0F);
		}
	}

	private static void tickFight(FishingHook hook, HookSession session) {
		if (!(hook.level() instanceof ServerLevel level)) {
			return;
		}

		if (session.tickTimer()) {
			escape(hook, session);
			return;
		}

		if (session.tickPull()) {
			FishEncounter encounter = session.encounter();
			float overweight = encounter.overweight() ? FishingBalance.OVERWEIGHT_PULL_MULTIPLIER : 1.0F;
			float heal = session.maxHp() * FishingBalance.pullHealFraction(session.rarity()) * overweight * (1.0F - session.gear().pullResistance());
			session.heal(heal);
			session.scheduleNextPull(hook.getRandom());
			FishingNetworking.broadcast(hook, HookEvent.PULL);
			playSound(level, hook, FishingSounds.FISH_PULL, 0.9F, 0.9F + hook.getRandom().nextFloat() * 0.2F);
			if (encounter.overweight()) {
				playSound(level, hook, FishingSounds.LINE_STRAIN, 0.7F, 1.0F);
			}

			level.sendParticles(ParticleTypes.SPLASH, hook.getX(), hook.getY() + 0.2, hook.getZ(), 8, 0.3, 0.05, 0.3, 0.15);
		} else if (hook.tickCount % SYNC_INTERVAL == 0) {
			FishingNetworking.broadcast(hook, HookEvent.SYNC);
		}
	}

	private static void fishLeaves(FishingHook hook, HookSession session) {
		session.reset();
		FishingHookAccess.of(hook).examplemod$resetToWaiting();
		FishingNetworking.broadcast(hook, HookEvent.LEFT);
	}

	private static void escape(FishingHook hook, HookSession session) {
		Component rarity = session.rarity().displayName();
		session.reset();
		FishingHookAccess.of(hook).examplemod$resetToWaiting();
		FishingNetworking.broadcast(hook, HookEvent.ESCAPED);
		if (hook.level() instanceof ServerLevel level) {
			playSound(level, hook, FishingSounds.ESCAPE, 1.0F, 1.0F);
		}

		if (hook.getPlayerOwner() instanceof ServerPlayer player) {
			player.sendOverlayMessage(Component.translatable("fishing.examplemod.escaped", rarity));
		}
	}

	static void playSound(ServerLevel level, FishingHook hook, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, hook.getX(), hook.getY(), hook.getZ(), sound, SoundSource.NEUTRAL, volume, pitch);
	}
}
