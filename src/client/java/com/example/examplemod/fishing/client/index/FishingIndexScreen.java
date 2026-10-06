package com.example.examplemod.fishing.client.index;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;

import com.example.examplemod.fishing.FishingFeature;
import com.example.examplemod.fishing.registry.FishingSounds;

/**
 * The Fishing Index: a two-page book with bookmark tabs for fish, treasure and personal
 * records. The left page holds a grid of entries (undiscovered fish show as silhouettes), the
 * right page the selected entry. The open tab, page and selection are remembered until the
 * game closes.
 */
public final class FishingIndexScreen extends Screen {
	private static final Identifier BOOK_TEXTURE = FishingFeature.id("textures/gui/fishing_index/book.png");
	private static final int BOOK_WIDTH = 300;
	private static final int BOOK_HEIGHT = 190;

	private static final int PAGE_WIDTH = 122;
	private static final int LEFT_PAGE_X = 18;
	private static final int RIGHT_PAGE_X = 160;
	private static final int PAGE_TOP = 14;

	private static final int GRID_COLUMNS = 5;
	private static final int GRID_ROWS = 4;
	private static final int SLOTS_PER_PAGE = GRID_COLUMNS * GRID_ROWS;
	private static final int SLOT_PITCH = IndexSlotButton.SIZE + 2;
	private static final int GRID_TOP = 50;

	private static final int PAGE_BUTTON_Y = 160;
	private static final int PAGE_BUTTON_WIDTH = 23;
	private static final int TAB_PITCH = IndexTabButton.WIDTH + 2;
	private static final int TAB_OVERLAP = 5;

	private static IndexTab openTab = IndexTab.FISH;
	private static final int[] OPEN_PAGE = new int[IndexTab.values().length];
	private static final int[] SELECTION = new int[IndexTab.values().length];

	private IndexContent content;
	private int left;
	private int top;

	public FishingIndexScreen() {
		super(Component.translatable("item.examplemod.fishing_index"));
	}

	@Override
	protected void init() {
		if (this.minecraft.level == null || this.minecraft.player == null) {
			return;
		}

		this.content = IndexContent.gather(this.minecraft.level, this.minecraft.player);
		this.left = (this.width - BOOK_WIDTH) / 2;
		this.top = Math.max(IndexTabButton.HEIGHT, (this.height - BOOK_HEIGHT + IndexTabButton.HEIGHT - TAB_OVERLAP) / 2);

		IndexTab[] tabs = IndexTab.values();
		for (int i = 0; i < tabs.length; i++) {
			IndexTab tab = tabs[i];
			int x = this.left + RIGHT_PAGE_X + PAGE_WIDTH - (tabs.length - i) * TAB_PITCH;
			this.addRenderableWidget(new IndexTabButton(x, this.top - IndexTabButton.HEIGHT + TAB_OVERLAP, tab, () -> openTab == tab, () -> this.selectTab(tab)));
		}

		List<? extends IndexContent.Entry> entries = this.entries();
		int pages = this.pageCount();
		int page = Mth.clamp(OPEN_PAGE[openTab.ordinal()], 0, pages - 1);
		OPEN_PAGE[openTab.ordinal()] = page;
		SELECTION[openTab.ordinal()] = Mth.clamp(SELECTION[openTab.ordinal()], 0, Math.max(0, entries.size() - 1));

		int gridLeft = this.left + LEFT_PAGE_X + (PAGE_WIDTH - (GRID_COLUMNS * SLOT_PITCH - 2)) / 2;
		for (int slot = 0; slot < SLOTS_PER_PAGE; slot++) {
			int index = page * SLOTS_PER_PAGE + slot;
			if (index >= entries.size()) {
				break;
			}

			int x = gridLeft + (slot % GRID_COLUMNS) * SLOT_PITCH;
			int y = this.top + GRID_TOP + (slot / GRID_COLUMNS) * SLOT_PITCH;
			this.addRenderableWidget(new IndexSlotButton(x, y, entries.get(index),
					() -> SELECTION[openTab.ordinal()] == index, () -> SELECTION[openTab.ordinal()] = index));
		}

		if (pages > 1) {
			int buttonY = this.top + PAGE_BUTTON_Y;
			PageButton back = this.addRenderableWidget(new PageButton(this.left + LEFT_PAGE_X, buttonY, false, button -> this.turnPage(-1), false));
			PageButton forward = this.addRenderableWidget(new PageButton(this.left + LEFT_PAGE_X + PAGE_WIDTH - PAGE_BUTTON_WIDTH, buttonY, true, button -> this.turnPage(1), false));
			back.visible = page > 0;
			forward.visible = page < pages - 1;
		}
	}

	private List<? extends IndexContent.Entry> entries() {
		return switch (openTab) {
			case FISH -> this.content.species();
			case TREASURE -> this.content.treasures();
			case RECORDS -> List.of();
		};
	}

	private int pageCount() {
		return Math.max(1, Mth.positiveCeilDiv(this.entries().size(), SLOTS_PER_PAGE));
	}

	private void selectTab(IndexTab tab) {
		openTab = tab;
		this.rebuildWidgets();
	}

	private void turnPage(int direction) {
		int page = Mth.clamp(OPEN_PAGE[openTab.ordinal()] + direction, 0, this.pageCount() - 1);
		if (page != OPEN_PAGE[openTab.ordinal()]) {
			OPEN_PAGE[openTab.ordinal()] = page;
			this.playSound(FishingSounds.INDEX_PAGE);
			this.rebuildWidgets();
		}
	}

	private void playSound(SoundEvent sound) {
		this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F));
	}

	@Override
	public void added() {
		super.added();
		this.playSound(FishingSounds.INDEX_OPEN);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_TEXTURE, this.left, this.top, 0.0F, 0.0F, BOOK_WIDTH, BOOK_HEIGHT, BOOK_WIDTH, BOOK_HEIGHT);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		if (this.content == null) {
			return;
		}

		PageWriter leftPage = new PageWriter(graphics, this.font, this.left + LEFT_PAGE_X, this.top + PAGE_TOP, PAGE_WIDTH);
		PageWriter rightPage = new PageWriter(graphics, this.font, this.left + RIGHT_PAGE_X, this.top + PAGE_TOP, PAGE_WIDTH);
		int selection = SELECTION[openTab.ordinal()];
		switch (openTab) {
			case FISH -> {
				IndexPages.speciesOverview(leftPage, this.content);
				if (!this.content.species().isEmpty()) {
					IndexPages.speciesDetails(rightPage, this.content.species().get(selection));
				}
			}
			case TREASURE -> {
				IndexPages.treasureOverview(leftPage, this.content);
				IndexPages.treasureDetails(rightPage, this.content.treasures().isEmpty() ? null : this.content.treasures().get(selection));
			}
			case RECORDS -> {
				IndexPages.records(leftPage, this.content);
				IndexPages.heaviestCatches(rightPage, this.content);
			}
		}

		int pages = this.pageCount();
		if (pages > 1) {
			Component indicator = Component.translatable("book.pageIndicator", OPEN_PAGE[openTab.ordinal()] + 1, pages);
			int x = this.left + LEFT_PAGE_X + (PAGE_WIDTH - this.font.width(indicator)) / 2;
			graphics.text(this.font, indicator, x, this.top + PAGE_BUTTON_Y + 3, PageWriter.MUTED_INK, false);
		}
	}

	@Override
	public boolean isInGameUi() {
		return true;
	}
}
