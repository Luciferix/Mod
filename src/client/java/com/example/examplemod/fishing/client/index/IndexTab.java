package com.example.examplemod.fishing.client.index;

import java.util.function.Supplier;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The bookmark tabs along the top of the Fishing Index. */
enum IndexTab {
	FISH("fish", () -> Items.TROPICAL_FISH),
	TREASURE("treasure", () -> Items.NAUTILUS_SHELL),
	RECORDS("records", () -> Items.WRITABLE_BOOK);

	private final String name;
	private final Supplier<Item> icon;

	IndexTab(String name, Supplier<Item> icon) {
		this.name = name;
		this.icon = icon;
	}

	Component title() {
		return Component.translatable("fishing.examplemod.index.tab." + this.name);
	}

	ItemStack icon() {
		return new ItemStack(this.icon.get());
	}
}
