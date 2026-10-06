package com.example.examplemod.fishing.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

import com.example.examplemod.fishing.catching.FishingController;
import com.example.examplemod.fishing.catching.FishingHookAccess;
import com.example.examplemod.fishing.catching.HookSession;
import com.example.examplemod.fishing.gear.FishingRods;

/**
 * Hooks the reworked catch flow into vanilla fishing. Vanilla still handles casting, waiting and
 * the moment a fish bites; from the bite on, {@link FishingController} takes over.
 */
@Mixin(FishingHook.class)
public abstract class FishingHookMixin extends Projectile implements FishingHookAccess {
	@Shadow
	@Final
	private static EntityDataAccessor<Boolean> DATA_BITING;

	@Shadow
	private int nibble;

	@Shadow
	private int timeUntilLured;

	@Shadow
	private int timeUntilHooked;

	@Shadow
	@Final
	private int luck;

	@Unique
	private final HookSession examplemod$session = new HookSession();

	protected FishingHookMixin(EntityType<? extends Projectile> type, Level level) {
		super(type, level);
	}

	@Override
	public HookSession examplemod$session() {
		return this.examplemod$session;
	}

	@Override
	public int examplemod$enchantmentLuck() {
		return this.luck;
	}

	@Override
	public void examplemod$holdBite(int ticks) {
		this.nibble = Math.max(this.nibble, ticks);
		this.getEntityData().set(DATA_BITING, true);
	}

	@Override
	public void examplemod$resetToWaiting() {
		this.nibble = 0;
		this.timeUntilLured = 0;
		this.timeUntilHooked = 0;
		this.getEntityData().set(DATA_BITING, false);
	}

	/** While a fish bites or fights, vanilla's bite timers are frozen. */
	@Inject(method = "catchingFish", at = @At("HEAD"), cancellable = true)
	private void examplemod$pauseVanillaBites(BlockPos blockPos, CallbackInfo ci) {
		if (this.examplemod$session.isEngaged()) {
			ci.cancel();
		}
	}

	/** Detects the tick vanilla makes a fish bite. */
	@Inject(method = "catchingFish", at = @At("TAIL"))
	private void examplemod$onVanillaBite(BlockPos blockPos, CallbackInfo ci) {
		if (this.nibble <= 0) {
			this.examplemod$session.setVanillaBiteHandled(false);
		} else if (!this.examplemod$session.isVanillaBiteHandled()) {
			this.examplemod$session.setVanillaBiteHandled(true);
			FishingController.onVanillaBite((FishingHook) (Object) this);
		}
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void examplemod$tickSession(CallbackInfo ci) {
		if (this.level() instanceof ServerLevel && !this.isRemoved()) {
			FishingController.serverTick((FishingHook) (Object) this);
		}
	}

	/** Vanilla only keeps the bobber while the vanilla rod is held; accept every fishing rod. */
	@Inject(method = "shouldStopFishing", at = @At("HEAD"), cancellable = true)
	private void examplemod$acceptAllRods(Player owner, CallbackInfoReturnable<Boolean> cir) {
		if (owner.canInteractWithLevel() && this.distanceToSqr(owner) <= 1024.0
				&& (FishingRods.isRod(owner.getMainHandItem()) || FishingRods.isRod(owner.getOffhandItem()))) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "remove", at = @At("HEAD"))
	private void examplemod$onRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
		if (this.level() instanceof ServerLevel) {
			FishingController.onHookRemoved((FishingHook) (Object) this);
		}
	}
}
