package com.example.examplemod.fishing.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.level.Level;

import com.example.examplemod.fishing.catching.FishingController;
import com.example.examplemod.fishing.catching.FishingHookAccess;

/** While a fish bites or fights, right-clicks set the hook and reel instead of pulling the bobber in. */
@Mixin(FishingRodItem.class)
public abstract class FishingRodItemMixin {
	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void examplemod$reelWhileEngaged(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		FishingHook hook = player.fishing;
		if (hook != null && FishingHookAccess.session(hook).isEngaged()) {
			if (player instanceof ServerPlayer serverPlayer) {
				FishingController.onRodUse(serverPlayer, hook);
			}

			cir.setReturnValue(InteractionResult.SUCCESS);
		}
	}
}
