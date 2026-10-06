package com.example.examplemod.fishing.registry;

import net.minecraft.world.entity.player.Player;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import com.example.examplemod.fishing.FishingFeature;
import com.example.examplemod.fishing.journal.FishingJournal;

public final class FishingAttachments {
	/** The player's Fishing Index progress. Saved with the player, kept on death, synced to that player only. */
	public static final AttachmentType<FishingJournal> JOURNAL = AttachmentRegistry.create(FishingFeature.id("fishing_journal"), builder -> builder
			.persistent(FishingJournal.CODEC)
			.copyOnDeath()
			.initializer(() -> FishingJournal.EMPTY)
			.syncWith(FishingJournal.STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

	private FishingAttachments() {
	}

	public static void init() {
	}

	public static FishingJournal journal(Player player) {
		return player.getAttachedOrElse(JOURNAL, FishingJournal.EMPTY);
	}
}
