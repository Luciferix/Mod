package com.example.examplemod.fishing.catching;

import java.util.Arrays;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import com.example.examplemod.fishing.FishingBalance;
import com.example.examplemod.fishing.gear.GearStats;
import com.example.examplemod.fishing.species.FishRarity;

/**
 * The catch state of one bobber, attached to every {@link net.minecraft.world.entity.projectile.FishingHook}
 * by a mixin. The server owns the state; clients hold a mirror updated from
 * {@link com.example.examplemod.fishing.network.HookStatePayload} plus a few animation timestamps.
 */
public final class HookSession {
	// ---- Synced state ----
	private Phase phase = Phase.WAITING;
	private FishRarity rarity = FishRarity.COMMON;
	private float hp;
	private float maxHp;
	private int ticksLeft;
	private int maxTicks;
	private boolean overweight;

	// ---- Server only ----
	private FishEncounter encounter;
	private GearStats gear;
	private int biteWindowLeft;
	private int ticksUntilPull;
	private boolean lowHpAnnounced;
	private boolean vanillaBiteHandled;
	private final long[] recentReelTicks = new long[FishingBalance.MAX_REEL_ACTIONS_PER_SECOND];
	private int recentReelIndex;

	// ---- Client only (animation) ----
	private long phaseStartedAt;
	private HookEvent lastEvent = HookEvent.SYNC;
	private long lastEventAt;
	private int clientRevealStep;

	public Phase phase() {
		return this.phase;
	}

	/** True while a fish is biting or being fought; rod clicks then reel instead of retrieving. */
	public boolean isEngaged() {
		return this.phase != Phase.WAITING;
	}

	public FishRarity rarity() {
		return this.rarity;
	}

	public float hp() {
		return this.hp;
	}

	public float maxHp() {
		return this.maxHp;
	}

	public float hpFraction() {
		return this.maxHp > 0.0F ? Mth.clamp(this.hp / this.maxHp, 0.0F, 1.0F) : 0.0F;
	}

	public int ticksLeft() {
		return this.ticksLeft;
	}

	public int maxTicks() {
		return this.maxTicks;
	}

	/** Whether the fish is heavier than the line's strength (a harder fight). */
	public boolean overweight() {
		return this.overweight;
	}

	public FishEncounter encounter() {
		return this.encounter;
	}

	public GearStats gear() {
		return this.gear;
	}

	// ---- Server transitions ----

	public void startBite(FishEncounter encounter, GearStats gear, int windowTicks) {
		this.phase = Phase.BITING;
		this.encounter = encounter;
		this.gear = gear;
		this.rarity = encounter.rarity();
		this.biteWindowLeft = windowTicks;
		this.hp = encounter.maxHp();
		this.maxHp = encounter.maxHp();
		this.ticksLeft = encounter.fightTicks();
		this.maxTicks = encounter.fightTicks();
		this.overweight = encounter.overweight();
	}

	/** Counts down the window to set the hook; returns true once the fish has lost interest. */
	public boolean tickBiteWindow() {
		return --this.biteWindowLeft <= 0;
	}

	public void startFight(RandomSource random) {
		this.phase = Phase.FIGHTING;
		this.lowHpAnnounced = false;
		this.scheduleNextPull(random);
	}

	/** Counts down the fight timer; returns true when it has run out. */
	public boolean tickTimer() {
		return --this.ticksLeft <= 0;
	}

	/** Counts down to the next pull-back; returns true when the fish pulls this tick. */
	public boolean tickPull() {
		return --this.ticksUntilPull <= 0;
	}

	public void scheduleNextPull(RandomSource random) {
		float reduction = 1.0F - FishingBalance.PULL_INTERVAL_REDUCTION_PER_RARITY * this.rarity.ordinal();
		int interval = Mth.nextInt(random, FishingBalance.PULL_INTERVAL_MIN, FishingBalance.PULL_INTERVAL_MAX);
		this.ticksUntilPull = Math.max(10, Math.round(interval * reduction));
	}

	public void damage(float amount) {
		this.hp = Math.max(0.0F, this.hp - amount);
	}

	public void heal(float amount) {
		this.hp = Math.min(this.maxHp, this.hp + amount);
	}

	/** Returns true the first time the fish drops below the low-HP threshold. */
	public boolean checkLowHp() {
		if (!this.lowHpAnnounced && this.hp <= this.maxHp * FishingBalance.LOW_HP_FRACTION) {
			this.lowHpAnnounced = true;
			return true;
		}

		return false;
	}

	/**
	 * Rate-limits reel actions to {@link FishingBalance#MAX_REEL_ACTIONS_PER_SECOND} per 20 ticks
	 * and one per tick.
	 */
	public boolean tryReelAction(long tick) {
		int newest = Math.floorMod(this.recentReelIndex - 1, this.recentReelTicks.length);
		long oldest = this.recentReelTicks[this.recentReelIndex];
		if (this.recentReelTicks[newest] == tick || (oldest != 0L && tick - oldest < 20L)) {
			return false;
		}

		this.recentReelTicks[this.recentReelIndex] = tick;
		this.recentReelIndex = (this.recentReelIndex + 1) % this.recentReelTicks.length;
		return true;
	}

	public boolean isVanillaBiteHandled() {
		return this.vanillaBiteHandled;
	}

	public void setVanillaBiteHandled(boolean handled) {
		this.vanillaBiteHandled = handled;
	}

	public void reset() {
		this.phase = Phase.WAITING;
		this.encounter = null;
		this.gear = null;
		this.hp = 0.0F;
		this.maxHp = 0.0F;
		this.ticksLeft = 0;
		this.maxTicks = 0;
		this.overweight = false;
		this.lowHpAnnounced = false;
		Arrays.fill(this.recentReelTicks, 0L);
	}

	// ---- Client mirror ----

	public void applySync(Phase phase, FishRarity rarity, float hp, float maxHp, int ticksLeft, int maxTicks, boolean overweight, HookEvent event, long clientTick) {
		if (phase != this.phase) {
			this.phaseStartedAt = clientTick;
			this.clientRevealStep = 0;
		}

		this.phase = phase;
		this.rarity = rarity;
		this.hp = hp;
		this.maxHp = maxHp;
		this.ticksLeft = ticksLeft;
		this.maxTicks = maxTicks;
		this.overweight = overweight;
		this.lastEvent = event;
		this.lastEventAt = clientTick;
	}

	/** Client-side countdown between syncs so the timer bar moves smoothly. */
	public void tickClient() {
		if (this.phase == Phase.FIGHTING && this.ticksLeft > 0) {
			this.ticksLeft--;
		}
	}

	public long phaseStartedAt() {
		return this.phaseStartedAt;
	}

	public HookEvent lastEvent() {
		return this.lastEvent;
	}

	public long lastEventAt() {
		return this.lastEventAt;
	}

	/** Last rarity-reveal step the client played a sound for. */
	public int clientRevealStep() {
		return this.clientRevealStep;
	}

	public void setClientRevealStep(int step) {
		this.clientRevealStep = step;
	}

	public enum Phase {
		WAITING,
		BITING,
		FIGHTING;

		private static final Phase[] VALUES = values();

		public static final StreamCodec<ByteBuf, Phase> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(i -> VALUES[i], Phase::ordinal);
	}
}
