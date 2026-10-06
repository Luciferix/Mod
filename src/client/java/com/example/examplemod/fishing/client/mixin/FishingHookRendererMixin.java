package com.example.examplemod.fishing.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.projectile.FishingHook;

import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;

import com.example.examplemod.fishing.client.render.BiteIndicatorRenderer;

/** Draws the bite indicator above every bobber with a bite or fight in progress. */
@Mixin(FishingHookRenderer.class)
public abstract class FishingHookRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/projectile/FishingHook;Lnet/minecraft/client/renderer/entity/state/FishingHookRenderState;F)V", at = @At("TAIL"))
	private void examplemod$extractBiteIndicator(FishingHook hook, FishingHookRenderState state, float partialTicks, CallbackInfo ci) {
		((FabricRenderState) state).setData(BiteIndicatorRenderer.KEY, BiteIndicatorRenderer.extract(hook, partialTicks));
	}

	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/FishingHookRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("TAIL"))
	private void examplemod$submitBiteIndicator(FishingHookRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		BiteIndicatorRenderer.Indicator indicator = ((FabricRenderState) state).getData(BiteIndicatorRenderer.KEY);
		if (indicator != null) {
			BiteIndicatorRenderer.submit(indicator, poseStack, collector, camera);
		}
	}
}
