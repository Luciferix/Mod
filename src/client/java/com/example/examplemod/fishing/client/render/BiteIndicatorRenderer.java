package com.example.examplemod.fishing.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.FishingHook;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;

import com.example.examplemod.fishing.FishingBalance;
import com.example.examplemod.fishing.FishingFeature;
import com.example.examplemod.fishing.catching.FishingHookAccess;
import com.example.examplemod.fishing.catching.HookSession;
import com.example.examplemod.fishing.client.ClientFishing;
import com.example.examplemod.fishing.species.FishRarity;

/**
 * The "!" above a bobber while a fish bites or fights. During the bite it climbs through the
 * rarity colors (white → green → blue → purple → gold) and stops on the fish's rarity; during
 * the fight it pulses faster as the fish tires, flashes red when the fish pulls and squashes on
 * each reel. An escaped fish leaves a gray "!" that sinks and fades.
 *
 * <p>Drawn by {@code FishingHookRendererMixin}: {@link #extract} runs while the bobber's render
 * state is built, {@link #submit} when it is drawn. The texture is a placeholder strip of three
 * frames: halo, glyph and rays (see ART_HANDOFF.md).
 */
public final class BiteIndicatorRenderer {
	public static final RenderStateDataKey<Indicator> KEY = RenderStateDataKey.create(() -> FishingFeature.MOD_ID + ":bite_indicator");

	private static final Identifier TEXTURE = FishingFeature.id("textures/entity/fishing/bite_indicator.png");
	private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucent(TEXTURE);

	private static final int FRAME_COUNT = 3;
	private static final int FRAME_HALO = 0;
	private static final int FRAME_GLYPH = 1;
	private static final int FRAME_RAYS = 2;

	/** Sizes in blocks; the glyph floats this high above the bobber. */
	private static final float HEIGHT_ABOVE_HOOK = 1.0F;
	private static final float GLYPH_SIZE = 0.55F;
	private static final float HALO_SIZE = 1.0F;
	private static final float RAYS_SIZE = 1.6F;
	/** Layers are nudged towards the camera so they never z-fight. */
	private static final float LAYER_DEPTH = 0.005F;

	private static final float APPEAR_TICKS = 5.0F;
	private static final float BOB_SPEED = 0.2F;
	private static final float BOB_HEIGHT = 0.05F;
	private static final float STEP_POP = 0.35F;
	private static final float STEP_POP_TICKS = 4.0F;

	private static final float FIGHT_PULSE_SPEED = 0.35F;
	private static final float FIGHT_PULSE = 0.07F;
	private static final float LOW_HP_PULSE_SPEED = 0.9F;
	private static final float LOW_HP_PULSE = 0.16F;
	private static final float REEL_SQUASH_TICKS = 3.0F;
	private static final float REEL_SQUASH = 0.12F;
	private static final float CRITICAL_POP_TICKS = 5.0F;
	private static final float CRITICAL_POP = 0.3F;
	private static final float PULL_FLASH_TICKS = 10.0F;
	private static final float PULL_SHAKE = 0.06F;
	private static final int PULL_COLOR = 0xFF4040;

	private static final float ESCAPE_FADE_TICKS = 20.0F;
	private static final float LEFT_FADE_TICKS = 10.0F;
	private static final float FADE_SINK = 0.02F;
	private static final int FADED_COLOR = 0x9A9A9A;

	private static final float RAYS_SPIN_EPIC = 1.5F;
	private static final float RAYS_SPIN_LEGENDARY = 2.5F;

	private BiteIndicatorRenderer() {
	}

	/**
	 * How the indicator looks this frame.
	 *
	 * @param color     RGB tint of all layers
	 * @param scale     size multiplier (pops and pulses)
	 * @param lift      extra height in blocks (bobbing, sinking)
	 * @param shake     sideways offset in blocks (fish pulling)
	 * @param alpha     opacity of the glyph
	 * @param haloAlpha opacity of the glow behind the glyph
	 * @param raysAlpha opacity of the spinning rays (epic and legendary only)
	 * @param raysAngle rotation of the rays in radians
	 */
	public record Indicator(int color, float scale, float lift, float shake, float alpha, float haloAlpha, float raysAlpha, float raysAngle) {
	}

	public static @Nullable Indicator extract(FishingHook hook, float partialTicks) {
		HookSession session = FishingHookAccess.session(hook);
		float now = ClientFishing.now(hook.level()) + partialTicks;
		return switch (session.phase()) {
			case BITING -> biting(session, now);
			case FIGHTING -> fighting(session, now);
			case WAITING -> fading(session, now);
		};
	}

	private static Indicator biting(HookSession session, float now) {
		float age = now - session.phaseStartedAt();
		int step = ClientFishing.revealStep(session, now);
		FishRarity shown = FishRarity.byId(step);
		boolean settled = step == session.rarity().ordinal();

		float stepAge = age - step * FishingBalance.REVEAL_STEP_TICKS;
		float pop = step > 0 ? STEP_POP * Math.max(0.0F, 1.0F - stepAge / STEP_POP_TICKS) : 0.0F;
		float scale = appear(age) * (1.0F + pop);
		float bob = Mth.sin(now * BOB_SPEED) * BOB_HEIGHT;
		float rays = settled ? raysAlpha(shown) : 0.0F;
		return new Indicator(shown.color(), scale, bob, 0.0F, 1.0F, haloAlpha(shown), rays, raysAngle(shown, now));
	}

	private static Indicator fighting(HookSession session, float now) {
		FishRarity rarity = session.rarity();
		boolean lowHp = session.hpFraction() <= FishingBalance.LOW_HP_FRACTION;
		float pulseSpeed = lowHp ? LOW_HP_PULSE_SPEED : FIGHT_PULSE_SPEED;
		float pulseSize = lowHp ? LOW_HP_PULSE : FIGHT_PULSE;
		float scale = 1.0F + pulseSize * Mth.sin(now * pulseSpeed);

		int color = rarity.color();
		float shake = 0.0F;
		float sinceEvent = now - session.lastEventAt();
		switch (session.lastEvent()) {
			case PULL -> {
				float strength = Math.max(0.0F, 1.0F - sinceEvent / PULL_FLASH_TICKS);
				color = ARGB.srgbLerp(strength, color, PULL_COLOR);
				shake = Mth.sin(now * 3.0F) * PULL_SHAKE * strength;
			}
			case REEL -> scale *= 1.0F - REEL_SQUASH * Math.max(0.0F, 1.0F - sinceEvent / REEL_SQUASH_TICKS);
			case REEL_CRITICAL -> scale *= 1.0F + CRITICAL_POP * Math.max(0.0F, 1.0F - sinceEvent / CRITICAL_POP_TICKS);
			default -> {
			}
		}

		float halo = Math.min(1.0F, haloAlpha(rarity) + (lowHp ? 0.25F : 0.0F));
		return new Indicator(color, scale, 0.0F, shake, 1.0F, halo, raysAlpha(rarity), raysAngle(rarity, now));
	}

	/** A gray "!" sinking away after the fish escaped or lost interest. */
	private static @Nullable Indicator fading(HookSession session, float now) {
		float duration = switch (session.lastEvent()) {
			case ESCAPED -> ESCAPE_FADE_TICKS;
			case LEFT -> LEFT_FADE_TICKS;
			default -> 0.0F;
		};
		float age = now - session.lastEventAt();
		if (age >= duration) {
			return null;
		}

		float alpha = 1.0F - age / duration;
		return new Indicator(FADED_COLOR, 0.8F + 0.2F * alpha, -age * FADE_SINK, 0.0F, alpha, 0.0F, 0.0F, 0.0F);
	}

	/** Grows from nothing with a small overshoot, like a speech bubble popping up. */
	private static float appear(float age) {
		float t = Mth.clamp(age / APPEAR_TICKS, 0.0F, 1.0F);
		float overshoot = 1.7F;
		float u = t - 1.0F;
		return 1.0F + (overshoot + 1.0F) * u * u * u + overshoot * u * u;
	}

	private static float haloAlpha(FishRarity rarity) {
		return switch (rarity) {
			case COMMON -> 0.15F;
			case UNCOMMON -> 0.3F;
			case RARE -> 0.45F;
			case EPIC -> 0.6F;
			case LEGENDARY -> 0.75F;
		};
	}

	private static float raysAlpha(FishRarity rarity) {
		return switch (rarity) {
			case EPIC -> 0.5F;
			case LEGENDARY -> 0.85F;
			default -> 0.0F;
		};
	}

	private static float raysAngle(FishRarity rarity, float now) {
		float degreesPerTick = rarity == FishRarity.LEGENDARY ? RAYS_SPIN_LEGENDARY : RAYS_SPIN_EPIC;
		return now * degreesPerTick * Mth.DEG_TO_RAD;
	}

	public static void submit(Indicator indicator, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.0F, HEIGHT_ABOVE_HOOK + indicator.lift(), 0.0F);
		poseStack.rotate(camera.orientation);
		poseStack.translate(indicator.shake(), 0.0F, 0.0F);
		float scale = indicator.scale();
		collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
			if (indicator.raysAlpha() > 0.0F) {
				quad(buffer, pose, FRAME_RAYS, RAYS_SIZE * scale, 0.0F, indicator.raysAngle(), ARGB.color(indicator.raysAlpha(), indicator.color()));
			}

			if (indicator.haloAlpha() > 0.0F) {
				quad(buffer, pose, FRAME_HALO, HALO_SIZE * scale, LAYER_DEPTH, 0.0F, ARGB.color(indicator.haloAlpha() * indicator.alpha(), indicator.color()));
			}

			quad(buffer, pose, FRAME_GLYPH, GLYPH_SIZE * scale, 2.0F * LAYER_DEPTH, 0.0F, ARGB.color(indicator.alpha(), indicator.color()));
		});
		poseStack.popPose();
	}

	/** A camera-facing square centered on the origin, rotated by {@code angle} in its plane. */
	private static void quad(VertexConsumer buffer, PoseStack.Pose pose, int frame, float size, float depth, float angle, int argb) {
		float half = size * 0.5F;
		float cos = Mth.cos(angle);
		float sin = Mth.sin(angle);
		float u0 = (float) frame / FRAME_COUNT;
		float u1 = (float) (frame + 1) / FRAME_COUNT;
		corner(buffer, pose, -half, -half, cos, sin, depth, u0, 1.0F, argb);
		corner(buffer, pose, half, -half, cos, sin, depth, u1, 1.0F, argb);
		corner(buffer, pose, half, half, cos, sin, depth, u1, 0.0F, argb);
		corner(buffer, pose, -half, half, cos, sin, depth, u0, 0.0F, argb);
	}

	private static void corner(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float cos, float sin, float depth, float u, float v, int argb) {
		buffer.addVertex(pose, x * cos - y * sin, x * sin + y * cos, depth)
				.setColor(argb)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(LightCoordsUtil.FULL_BRIGHT)
				.setNormal(pose, 0.0F, 1.0F, 0.0F);
	}
}
