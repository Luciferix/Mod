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
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import com.example.examplemod.fishing.FishingFeature;
import com.example.examplemod.fishing.registry.FishingSounds;

/** A bookmark ribbon above the book; the selected one sticks up a little further. */
final class IndexTabButton extends AbstractButton {
	static final int WIDTH = 26;
	static final int HEIGHT = 24;
	private static final int SELECTED_RAISE = 3;
	private static final Identifier TAB = FishingFeature.id("fishing/index/tab");
	private static final Identifier TAB_HIGHLIGHTED = FishingFeature.id("fishing/index/tab_highlighted");
	private static final Identifier TAB_SELECTED = FishingFeature.id("fishing/index/tab_selected");

	private final ItemStack icon;
	private final BooleanSupplier selected;
	private final Runnable onPress;

	IndexTabButton(int x, int y, IndexTab tab, BooleanSupplier selected, Runnable onPress) {
		super(x, y, WIDTH, HEIGHT, tab.title());
		this.icon = tab.icon();
		this.selected = selected;
		this.onPress = onPress;
		this.setTooltip(Tooltip.create(tab.title()));
	}

	@Override
	public void onPress(InputWithModifiers input) {
		if (!this.selected.getAsBoolean()) {
			this.onPress.run();
		}
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		boolean selected = this.selected.getAsBoolean();
		int y = selected ? this.getY() - SELECTED_RAISE : this.getY();
		Identifier sprite = selected ? TAB_SELECTED : this.isHoveredOrFocused() ? TAB_HIGHLIGHTED : TAB;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), y, WIDTH, HEIGHT);
		graphics.item(this.icon, this.getX() + (WIDTH - 16) / 2, y + 3);
	}

	@Override
	public void playDownSound(SoundManager soundManager) {
		soundManager.play(SimpleSoundInstance.forUI(FishingSounds.INDEX_PAGE, 1.0F));
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
