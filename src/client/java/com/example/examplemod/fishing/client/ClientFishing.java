package com.example.examplemod.fishing.client;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.example.examplemod.fishing.FishingBalance;
import com.example.examplemod.fishing.catching.FishingHookAccess;
import com.example.examplemod.fishing.catching.HookEvent;
import com.example.examplemod.fishing.catching.HookSession;
import com.example.examplemod.fishing.network.HookStatePayload;
import com.example.examplemod.fishing.registry.FishingSounds;
import com.example.examplemod.fishing.species.FishRarity;

/**
 * Client side of the catch flow: applies hook state from the server, runs the rarity reveal
 * (sounds per step) and the fight timer between syncs, and plays catch/escape effects.
 * All particles here are vanilla placeholders (see ART_HANDOFF.md).
 */
public final class ClientFishing {
	/** Bobbers with a bite or fight in progress, ticked every client tick. */
	private static final Set<FishingHook> ACTIVE = Collections.newSetFromMap(new WeakHashMap<>());

	private ClientFishing() {
	}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(HookStatePayload.TYPE, (payload, context) -> handle(context.client(), payload));
	}

	/** The clock that client-side animation timestamps on {@link HookSession} use. */
	public static long now(Level level) {
		return level.getGameTime();
	}

	private static void handle(Minecraft client, HookStatePayload payload) {
		ClientLevel level = client.level;
		if (level == null || !(level.getEntity(payload.hookId()) instanceof FishingHook hook)) {
			return;
		}

		HookSession session = FishingHookAccess.session(hook);
		session.applySync(payload.phase(), payload.rarity(), payload.hp(), payload.maxHp(), payload.ticksLeft(), payload.maxTicks(), payload.overweight(), payload.event(), now(level));
		if (session.isEngaged()) {
			ACTIVE.add(hook);
		}

		playEventEffects(level, hook, payload.event(), payload.rarity());
	}

	/** Called every client tick. */
	public static void tick(Minecraft client) {
		ClientLevel level = client.level;
		if (level == null) {
			ACTIVE.clear();
			return;
		}

		long now = now(level);
		Iterator<FishingHook> iterator = ACTIVE.iterator();
		while (iterator.hasNext()) {
			FishingHook hook = iterator.next();
			HookSession session = FishingHookAccess.session(hook);
			if (hook.isRemoved() || !session.isEngaged()) {
				iterator.remove();
				continue;
			}

			session.tickClient();
			if (session.phase() == HookSession.Phase.BITING) {
				playRevealStep(level, hook, session, now);
			}
		}
	}

	public static void clear() {
		ACTIVE.clear();
	}

	/** How many steps of the white → green → blue → purple → gold climb have played so far. */
	public static int revealStep(HookSession session, float now) {
		int target = session.rarity().ordinal();
		int step = (int) ((now - session.phaseStartedAt()) / FishingBalance.REVEAL_STEP_TICKS);
		return Math.max(0, Math.min(target, step));
	}

	private static void playRevealStep(ClientLevel level, FishingHook hook, HookSession session, long now) {
		int step = revealStep(session, now);
		if (step <= session.clientRevealStep()) {
			return;
		}

		session.setClientRevealStep(step);
		boolean finalStep = step == session.rarity().ordinal();
		SoundEvent sound = finalStep ? FishingSounds.revealSound(session.rarity()) : FishingSounds.REVEAL_TICK;
		if (sound != null) {
			level.playLocalSound(hook.getX(), hook.getY(), hook.getZ(), sound, SoundSource.NEUTRAL, 0.8F, 0.8F + step * 0.15F, false);
		}

		if (finalStep && session.rarity().isAtLeast(FishRarity.EPIC)) {
			burst(level, hook.position().add(0.0, 1.0, 0.0), session.rarity() == FishRarity.LEGENDARY ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.END_ROD, 16, 0.08);
		}
	}

	private static void playEventEffects(ClientLevel level, FishingHook hook, HookEvent event, FishRarity rarity) {
		Vec3 pos = hook.position();
		switch (event) {
			case REEL -> burst(level, pos.add(0.0, 0.2, 0.0), ParticleTypes.SPLASH, 3, 0.05);
			case REEL_CRITICAL -> burst(level, pos.add(0.0, 0.3, 0.0), ParticleTypes.CRIT, 8, 0.25);
			case PULL -> {
				hook.setDeltaMovement(hook.getDeltaMovement().add(0.0, -0.25, 0.0));
				burst(level, pos.add(0.0, 0.2, 0.0), ParticleTypes.BUBBLE, 6, 0.1);
			}
			case CAUGHT -> burst(level, pos.add(0.0, 0.5, 0.0), catchParticle(rarity), 10 + 6 * rarity.ordinal(), 0.15);
			case ESCAPED -> burst(level, pos.add(0.0, 0.3, 0.0), ParticleTypes.SMOKE, 8, 0.03);
			default -> {
			}
		}
	}

	private static ParticleOptions catchParticle(FishRarity rarity) {
		return switch (rarity) {
			case COMMON -> ParticleTypes.SPLASH;
			case UNCOMMON -> ParticleTypes.HAPPY_VILLAGER;
			case RARE -> ParticleTypes.GLOW;
			case EPIC -> ParticleTypes.END_ROD;
			case LEGENDARY -> ParticleTypes.TOTEM_OF_UNDYING;
		};
	}

	private static void burst(ClientLevel level, Vec3 pos, ParticleOptions particle, int count, double speed) {
		RandomSource random = level.getRandom();
		for (int i = 0; i < count; i++) {
			level.addParticle(particle, pos.x, pos.y, pos.z,
					(random.nextDouble() - 0.5) * 2.0 * speed, random.nextDouble() * speed * 1.5, (random.nextDouble() - 0.5) * 2.0 * speed);
		}
	}

	/** The local player's bobber, if it has a bite or fight in progress. */
	public static FishingHook localEngagedHook(Minecraft client) {
		if (client.player == null || client.player.fishing == null) {
			return null;
		}

		FishingHook hook = client.player.fishing;
		return FishingHookAccess.session(hook).isEngaged() ? hook : null;
	}
}
