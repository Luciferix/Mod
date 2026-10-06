package com.example.examplemod.fishing.client.index;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import com.example.examplemod.fishing.FishingFeature;
import com.example.examplemod.fishing.species.FishRarity;

/**
 * Lays out one page of the Fishing Index top to bottom: headings, wrapped paragraphs,
 * "label: value" rows, item rows and progress bars, in ink colors that read on the paper.
 */
final class PageWriter {
	static final int INK = 0xFF3B2A1A;
	static final int MUTED_INK = 0xFF7A6248;
	static final int GOOD_INK = 0xFF2E6B2E;
	private static final int RULE_COLOR = 0x553B2A1A;

	private static final Identifier ICON_FRAME = FishingFeature.id("fishing/index/icon_frame");
	private static final Identifier UNKNOWN = FishingFeature.id("fishing/index/unknown");
	private static final Identifier PROGRESS_BACKGROUND = FishingFeature.id("fishing/index/progress_background");
	private static final Identifier PROGRESS_FILL = FishingFeature.id("fishing/index/progress_fill");
	private static final int ICON_FRAME_SIZE = 36;
	private static final int PROGRESS_WIDTH = 100;
	private static final int PROGRESS_HEIGHT = 5;
	private static final int ITEM_ROW_HEIGHT = 18;
	private static final int LABEL_GAP = 4;

	private final GuiGraphicsExtractor graphics;
	private final Font font;
	private final int x;
	private final int width;
	private int y;

	PageWriter(GuiGraphicsExtractor graphics, Font font, int x, int y, int width) {
		this.graphics = graphics;
		this.font = font;
		this.x = x;
		this.y = y;
		this.width = width;
	}

	/** Paper-friendly (darker) version of a rarity's color. */
	static int rarityInk(FishRarity rarity) {
		return switch (rarity) {
			case COMMON -> 0xFF5E5E5E;
			case UNCOMMON -> 0xFF2E7D32;
			case RARE -> 0xFF1E5AA8;
			case EPIC -> 0xFF7B2FA8;
			case LEGENDARY -> 0xFFB07A00;
		};
	}

	static Component rarityName(FishRarity rarity) {
		return Component.translatable("fishing.examplemod.rarity." + rarity.getSerializedName()).withColor(rarityInk(rarity) & 0xFFFFFF);
	}

	int y() {
		return this.y;
	}

	void skip(int pixels) {
		this.y += pixels;
	}

	void heading(Component text) {
		this.centered(text.copy().withStyle(ChatFormatting.BOLD), INK);
		this.y += 3;
	}

	void centered(Component text, int color) {
		this.graphics.text(this.font, text, this.x + (this.width - this.font.width(text)) / 2, this.y, color, false);
		this.y += this.font.lineHeight;
	}

	/** Word-wrapped text, cut off after {@code maxLines}. */
	void paragraph(Component text, int color, int maxLines) {
		List<FormattedCharSequence> lines = this.font.split(text, this.width);
		for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
			this.graphics.text(this.font, lines.get(i), this.x, this.y, color, false);
			this.y += this.font.lineHeight;
		}
	}

	/** "Label: value", with the value wrapped beside the label. */
	void row(Component label, Component value) {
		Component labelText = Component.translatable("fishing.examplemod.index.label", label);
		int labelWidth = this.font.width(labelText);
		this.graphics.text(this.font, labelText, this.x, this.y, MUTED_INK, false);
		List<FormattedCharSequence> lines = this.font.split(value, Math.max(20, this.width - labelWidth - LABEL_GAP));
		for (FormattedCharSequence line : lines) {
			this.graphics.text(this.font, line, this.x + labelWidth + LABEL_GAP, this.y, INK, false);
			this.y += this.font.lineHeight;
		}

		if (lines.isEmpty()) {
			this.y += this.font.lineHeight;
		}
	}

	void separator() {
		this.y += 2;
		this.graphics.fill(this.x, this.y, this.x + this.width, this.y + 1, RULE_COLOR);
		this.y += 4;
	}

	/** A centered bar showing how much of a collection is complete. */
	void progress(float fraction) {
		int left = this.x + (this.width - PROGRESS_WIDTH) / 2;
		this.graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESS_BACKGROUND, left, this.y, PROGRESS_WIDTH, PROGRESS_HEIGHT);
		int filled = Math.round((PROGRESS_WIDTH - 2) * Mth.clamp(fraction, 0.0F, 1.0F));
		if (filled > 0) {
			this.graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESS_FILL, PROGRESS_WIDTH - 2, PROGRESS_HEIGHT - 2, 0, 0,
					left + 1, this.y + 1, filled, PROGRESS_HEIGHT - 2);
		}

		this.y += PROGRESS_HEIGHT + 4;
	}

	/** A framed double-size icon (a question mark when null) with a title and a subtitle beside it. */
	void iconHeader(@Nullable ItemStack icon, Component title, Component subtitle) {
		this.graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ICON_FRAME, this.x, this.y, ICON_FRAME_SIZE, ICON_FRAME_SIZE);
		if (icon != null) {
			this.graphics.pose().pushMatrix();
			this.graphics.pose().translate(this.x + 2, this.y + 2);
			this.graphics.pose().scale(2.0F, 2.0F);
			this.graphics.item(icon, 0, 0);
			this.graphics.pose().popMatrix();
		} else {
			this.graphics.blitSprite(RenderPipelines.GUI_TEXTURED, UNKNOWN, this.x + 2, this.y + 2, ICON_FRAME_SIZE - 4, ICON_FRAME_SIZE - 4);
		}

		int textX = this.x + ICON_FRAME_SIZE + 4;
		int textWidth = this.width - ICON_FRAME_SIZE - 4;
		int textY = this.y + 3;
		List<FormattedCharSequence> titleLines = this.font.split(title.copy().withStyle(ChatFormatting.BOLD), textWidth);
		for (int i = 0; i < Math.min(2, titleLines.size()); i++) {
			this.graphics.text(this.font, titleLines.get(i), textX, textY, INK, false);
			textY += this.font.lineHeight;
		}

		this.graphics.text(this.font, subtitle, textX, textY + 1, MUTED_INK, false);
		this.y += ICON_FRAME_SIZE + 2;
	}

	/** A small icon, a name and a right-aligned value on one line. */
	void itemRow(ItemStack icon, Component name, Component value) {
		this.graphics.item(icon, this.x, this.y);
		int valueWidth = this.font.width(value);
		int nameWidth = this.width - 20 - valueWidth - LABEL_GAP;
		String clipped = this.font.plainSubstrByWidth(name.getString(), nameWidth);
		int textY = this.y + (16 - this.font.lineHeight) / 2 + 1;
		this.graphics.text(this.font, clipped, this.x + 20, textY, INK, false);
		this.graphics.text(this.font, value, this.x + this.width - valueWidth, textY, MUTED_INK, false);
		this.y += ITEM_ROW_HEIGHT;
	}
}
