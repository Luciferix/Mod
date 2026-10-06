package com.example.examplemod.fishing.network;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.example.examplemod.fishing.catching.FishingHookAccess;
import com.example.examplemod.fishing.catching.HookEvent;

public final class FishingNetworking {
	private FishingNetworking() {
	}

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(HookStatePayload.TYPE, HookStatePayload.STREAM_CODEC);
	}

	/** Sends the bobber's current state to its owner and everyone tracking it. */
	public static void broadcast(FishingHook hook, HookEvent event) {
		HookStatePayload payload = HookStatePayload.of(hook.getId(), FishingHookAccess.session(hook), event);
		Set<ServerPlayer> recipients = new HashSet<>(PlayerLookup.tracking(hook));
		if (hook.getPlayerOwner() instanceof ServerPlayer owner) {
			recipients.add(owner);
		}

		for (ServerPlayer player : recipients) {
			if (ServerPlayNetworking.canSend(player, HookStatePayload.TYPE)) {
				ServerPlayNetworking.send(player, payload);
			}
		}
	}
}
