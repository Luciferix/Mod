package com.example.examplemod.fishing.client.index;

import java.util.function.BooleanSupplier;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import com.example.examplemod.fishing.FishingFeature;
import com.example.examplemod.fishing.registry.FishingSounds;

/** One cell of the Index grid: the entry's icon (or a question mark), selectable to show its page. */
final class IndexSlotButton extends AbstractButton {
	static final int SIZE = 22;
	private static final int ICON_INSET = 3;
	private static final int MARKER_SIZE = 7;
	private static final Identifier SLOT = FishingFeature.id("fishing/index/slot");
	private static final Identifier SLOT_HIGHLIGHTED = FishingFeature.id("fishing/index/slot_highlighted");
	private static final Identifier SLOT_SELECTED = FishingFeature.id("fishing/index/slot_selected");
	private static final Identifier UNKNOWN = FishingFeature.id("fishing/index/unknown");
	private static final Identifier HERE_MARKER = FishingFeature.id("fishing/index/here_marker");

	private final IndexContent.Entry entry;
	private final BooleanSupplier selected;
	private final Runnable onSelect;

	IndexSlotButton(int x, int y, IndexContent.Entry entry, BooleanSupplier selected, Runnable onSelect) {
		super(x, y, SIZE, SIZE, entry.name());
		this.entry = entry;
		this.selected = selected;
		this.onSelect = onSelect;
		Component tooltip = entry.marked()
				? Component.translatable("fishing.examplemod.index.tooltip_here", entry.name())
				: entry.name();
		this.setTooltip(Tooltip.create(tooltip));
	}

	@Override
	public void onPress(InputWithModifiers input) {
		this.onSelect.run();
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		Identifier frame = this.selected.getAsBoolean() ? SLOT_SELECTED : this.isHoveredOrFocused() ? SLOT_HIGHLIGHTED : SLOT;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, frame, this.getX(), this.getY(), SIZE, SIZE);
		if (this.entry.showsIcon()) {
			graphics.item(this.entry.icon(), this.getX() + ICON_INSET, this.getY() + ICON_INSET);
		} else {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, UNKNOWN, this.getX() + ICON_INSET, this.getY() + ICON_INSET, 16, 16);
		}

		if (this.entry.marked()) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HERE_MARKER, this.getX() + SIZE - MARKER_SIZE - 1, this.getY() + 1, MARKER_SIZE, MARKER_SIZE);
		}
	}

	@Override
	public void playDownSound(SoundManager soundManager) {
		soundManager.play(SimpleSoundInstance.forUI(FishingSounds.INDEX_SELECT, 1.0F));
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
