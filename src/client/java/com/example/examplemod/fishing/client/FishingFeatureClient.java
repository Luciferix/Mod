package com.example.examplemod.fishing.client;

import net.minecraft.client.Minecraft;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import com.example.examplemod.fishing.client.hud.FightHud;
import com.example.examplemod.fishing.client.index.FishingIndexScreen;
import com.example.examplemod.fishing.item.FishingIndexItem;

/**
 * Client entrypoint of the fishing rework, registered in fabric.mod.json. The bite indicator
 * is drawn by {@code FishingHookRendererMixin} (client mixin config).
 */
public final class FishingFeatureClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientFishing.init();
		ClientTickEvents.END_CLIENT_TICK.register(ClientFishing::tick);
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> ClientFishing.clear());
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, FightHud.ID, new FightHud());
		FishingIndexItem.setScreenOpener(() -> Minecraft.getInstance().gui.setScreen(new FishingIndexScreen()));
	}
}
