package com.example.examplemod.fishing.client.hud;

import java.util.Locale;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.FishingHook;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;

import com.example.examplemod.fishing.FishingBalance;
import com.example.examplemod.fishing.FishingFeature;
import com.example.examplemod.fishing.catching.FishingHookAccess;
import com.example.examplemod.fishing.catching.HookEvent;
import com.example.examplemod.fishing.catching.HookSession;
import com.example.examplemod.fishing.client.ClientFishing;
import com.example.examplemod.fishing.species.FishRarity;

/**
 * Catch HUD under the crosshair, for the local player's bobber only.
 *
 * <p>While a fish bites: what is biting (once the reveal settles), the key to set the hook and
 * a bar for the time left to do it. While fighting: the fish's HP bar in its rarity color, with
 * a fading "ghost" of recent damage, a red flash and shake when the fish pulls back and a
 * flicker at low HP; below it the fight timer and a warning when the fish is heavier than the
 * line. All sprites are placeholders under {@code textures/gui/sprites/fishing/hud/}.
 */
public final class FightHud implements HudElement {
	public static final Identifier ID = FishingFeature.id("fishing_fight");

	private static final Identifier BAR_BACKGROUND = FishingFeature.id("fishing/hud/hp_bar_background");
	private static final Identifier BAR_FILL = FishingFeature.id("fishing/hud/hp_bar_fill");
	private static final Identifier TIMER_BACKGROUND = FishingFeature.id("fishing/hud/timer_bar_background");
	private static final Identifier TIMER_FILL = FishingFeature.id("fishing/hud/timer_bar_fill");

	/** Sprite sizes; the fills sit one pixel inside their backgrounds. */
	private static final int BAR_WIDTH = 122;
	private static final int BAR_HEIGHT = 7;
	private static final int TIMER_HEIGHT = 5;
	private static final int FILL_WIDTH = BAR_WIDTH - 2;
	private static final int BAR_FILL_HEIGHT = BAR_HEIGHT - 2;
	private static final int TIMER_FILL_HEIGHT = TIMER_HEIGHT - 2;

	private static final int TOP_BELOW_CROSSHAIR = 14;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int MUTED_TEXT_COLOR = 0xFFB8B8B8;
	private static final int WARNING_COLOR = 0xFFFFA040;
	private static final int GHOST_COLOR = 0xFFFFF4D0;
	private static final int PULL_COLOR = 0xFF4040;
	private static final int TIMER_SAFE_COLOR = 0xE8E8E8;
	private static final int TIMER_WARN_COLOR = 0xFFD24A;
	private static final int TIMER_DANGER_COLOR = 0xFF5050;

	/** Ghost damage drains at this fraction of the bar per second. */
	private static final float GHOST_DRAIN_PER_SECOND = 0.6F;
	private static final float PULL_SHAKE_TICKS = 8.0F;
	private static final float PULL_SHAKE_PIXELS = 2.0F;
	private static final float HIT_FLASH_TICKS = 3.0F;

	private int ghostHookId = -1;
	private float ghostFraction = 1.0F;
	private long lastFrameNanos;

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		FishingHook hook = ClientFishing.localEngagedHook(client);
		long nanos = System.nanoTime();
		float frameSeconds = this.lastFrameNanos == 0L ? 0.0F : Math.min(0.25F, (nanos - this.lastFrameNanos) / 1.0E9F);
		this.lastFrameNanos = nanos;
		if (hook == null) {
			this.ghostHookId = -1;
			return;
		}

		HookSession session = FishingHookAccess.session(hook);
		float now = ClientFishing.now(hook.level()) + deltaTracker.getGameTimeDeltaPartialTick(false);
		int centerX = graphics.guiWidth() / 2;
		int top = graphics.guiHeight() / 2 + TOP_BELOW_CROSSHAIR;
		Font font = client.font;
		switch (session.phase()) {
			case BITING -> this.extractBite(graphics, font, client, session, centerX, top, now);
			case FIGHTING -> this.extractFight(graphics, font, hook, session, centerX, top, now, frameSeconds);
			case WAITING -> {
			}
		}
	}

	private void extractBite(GuiGraphicsExtractor graphics, Font font, Minecraft client, HookSession session, int centerX, int top, float now) {
		int step = ClientFishing.revealStep(session, now);
		FishRarity shown = FishRarity.byId(step);
		boolean settled = step == session.rarity().ordinal();
		Component headline = settled
				? Component.translatable("fishing.examplemod.hud.rarity_bite", session.rarity().displayName())
				: Component.translatable("fishing.examplemod.hud.bite");
		float blink = 0.75F + 0.25F * Mth.sin(now * 0.6F);
		graphics.centeredText(font, headline, centerX, top, ARGB.color(blink, shown.color()));

		Component prompt = Component.translatable("fishing.examplemod.hud.set_hook", client.options.keyUse.getTranslatedKeyMessage());
		graphics.centeredText(font, prompt, centerX, top + font.lineHeight + 2, MUTED_TEXT_COLOR);

		float window = FishingBalance.biteWindowTicks(session.rarity());
		float left = Mth.clamp(1.0F - (now - session.phaseStartedAt()) / window, 0.0F, 1.0F);
		int barY = top + 2 * (font.lineHeight + 2);
		this.extractTimer(graphics, centerX - BAR_WIDTH / 2, barY, left, now);
	}

	private void extractFight(GuiGraphicsExtractor graphics, Font font, FishingHook hook, HookSession session, int centerX, int top,
			float now, float frameSeconds) {
		FishRarity rarity = session.rarity();
		float hp = session.hpFraction();
		this.updateGhost(hook.getId(), hp, frameSeconds);

		float sinceEvent = now - session.lastEventAt();
		HookEvent event = session.lastEvent();
		float pullStrength = event == HookEvent.PULL ? Math.max(0.0F, 1.0F - sinceEvent / PULL_SHAKE_TICKS) : 0.0F;
		int shake = Math.round(Mth.sin(now * 2.5F) * PULL_SHAKE_PIXELS * pullStrength);
		int left = centerX - BAR_WIDTH / 2 + shake;

		graphics.centeredText(font, rarity.displayName(), centerX + shake, top, TEXT_COLOR);
		int barY = top + font.lineHeight + 2;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_BACKGROUND, left, barY, BAR_WIDTH, BAR_HEIGHT);

		int fillX = left + 1;
		int fillY = barY + 1;
		int hpWidth = Math.round(FILL_WIDTH * hp);
		int ghostWidth = Math.round(FILL_WIDTH * this.ghostFraction);
		if (ghostWidth > hpWidth) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_FILL, FILL_WIDTH, BAR_FILL_HEIGHT, hpWidth, 0,
					fillX + hpWidth, fillY, ghostWidth - hpWidth, BAR_FILL_HEIGHT, GHOST_COLOR);
		}

		if (hpWidth > 0) {
			int fillColor = ARGB.srgbLerp(pullStrength, rarity.color(), PULL_COLOR);
			if (hp <= FishingBalance.LOW_HP_FRACTION) {
				fillColor = ARGB.srgbLerp(0.35F * (0.5F + 0.5F * Mth.sin(now * 1.2F)), fillColor, 0xFFFFFF);
			}

			if (isReel(event) && sinceEvent < HIT_FLASH_TICKS) {
				fillColor = ARGB.srgbLerp(0.5F * (1.0F - sinceEvent / HIT_FLASH_TICKS), fillColor, 0xFFFFFF);
			}

			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_FILL, FILL_WIDTH, BAR_FILL_HEIGHT, 0, 0,
					fillX, fillY, hpWidth, BAR_FILL_HEIGHT, ARGB.opaque(fillColor));
		}

		int timerY = barY + BAR_HEIGHT + 1;
		float timeLeft = session.maxTicks() > 0 ? Mth.clamp((float) session.ticksLeft() / session.maxTicks(), 0.0F, 1.0F) : 0.0F;
		this.extractTimer(graphics, left, timerY, timeLeft, now);

		int textY = timerY + TIMER_HEIGHT + 2;
		String hpText = String.format(Locale.ROOT, "%d / %d", Mth.ceil(session.hp()), Mth.ceil(session.maxHp()));
		String timeText = String.format(Locale.ROOT, "%.1fs", Math.max(0, session.ticksLeft()) / 20.0F);
		graphics.text(font, hpText, left, textY, MUTED_TEXT_COLOR, true);
		graphics.text(font, timeText, left + BAR_WIDTH - font.width(timeText), textY, MUTED_TEXT_COLOR, true);

		if (session.overweight()) {
			graphics.centeredText(font, Component.translatable("fishing.examplemod.hud.line_strained"), centerX, textY + font.lineHeight + 2, WARNING_COLOR);
		}
	}

	private void extractTimer(GuiGraphicsExtractor graphics, int left, int y, float fraction, float now) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TIMER_BACKGROUND, left, y, BAR_WIDTH, TIMER_HEIGHT);
		int width = Math.round(FILL_WIDTH * fraction);
		if (width <= 0) {
			return;
		}

		int color = fraction > 0.5F ? TIMER_SAFE_COLOR : fraction > 0.25F ? TIMER_WARN_COLOR : TIMER_DANGER_COLOR;
		float alpha = fraction > 0.25F ? 1.0F : 0.7F + 0.3F * Mth.sin(now * 1.5F);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TIMER_FILL, FILL_WIDTH, TIMER_FILL_HEIGHT, 0, 0,
				left + 1, y + 1, width, TIMER_FILL_HEIGHT, ARGB.color(alpha, color));
	}

	private static boolean isReel(HookEvent event) {
		return event == HookEvent.REEL || event == HookEvent.REEL_CRITICAL || event == HookEvent.LOW_HP;
	}

	/** The ghost bar trails the real HP after damage and snaps up when the fish heals. */
	private void updateGhost(int hookId, float hp, float frameSeconds) {
		if (hookId != this.ghostHookId || hp > this.ghostFraction) {
			this.ghostHookId = hookId;
			this.ghostFraction = hp;
			return;
		}

		this.ghostFraction = Math.max(hp, this.ghostFraction - GHOST_DRAIN_PER_SECOND * frameSeconds);
	}
}
