package com.example.examplemod.fishing.registry;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import com.example.examplemod.fishing.catching.CatchRewards;
import com.example.examplemod.fishing.catching.FishEncounter;
import com.example.examplemod.fishing.journal.FishingJournal;
import com.example.examplemod.fishing.journal.SpeciesRecord;
import com.example.examplemod.fishing.species.FishSpecies;

/**
 * Test helpers for operators:
 * {@code /fishing journal reset|complete [targets]} and {@code /fishing give <species> [weight_kg]}.
 */
public final class FishingCommands {
	private FishingCommands() {
	}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> register(dispatcher));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("fishing")
				.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
				.then(Commands.literal("journal")
						.then(Commands.literal("reset")
								.executes(context -> resetJournal(context, List.of(context.getSource().getPlayerOrException())))
								.then(Commands.argument("targets", EntityArgument.players())
										.executes(context -> resetJournal(context, EntityArgument.getPlayers(context, "targets")))))
						.then(Commands.literal("complete")
								.executes(context -> completeJournal(context, List.of(context.getSource().getPlayerOrException())))
								.then(Commands.argument("targets", EntityArgument.players())
										.executes(context -> completeJournal(context, EntityArgument.getPlayers(context, "targets"))))))
				.then(Commands.literal("give")
						.then(Commands.argument("species", IdentifierArgument.id())
								.suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
										speciesRegistry(context.getSource()).keySet(), builder))
								.executes(context -> giveFish(context, Optional.empty()))
								.then(Commands.argument("weight_kg", FloatArgumentType.floatArg(0.001F))
										.executes(context -> giveFish(context, Optional.of(FloatArgumentType.getFloat(context, "weight_kg"))))))));
	}

	private static int resetJournal(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
		for (ServerPlayer player : targets) {
			player.setAttached(FishingAttachments.JOURNAL, FishingJournal.EMPTY);
		}

		context.getSource().sendSuccess(() -> Component.translatable("commands.examplemod.fishing.journal.reset", targets.size()), true);
		return targets.size();
	}

	private static int completeJournal(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
		Registry<FishSpecies> registry = speciesRegistry(context.getSource());
		long day = context.getSource().getLevel().getOverworldClockTime() / 24000L + 1L;
		for (ServerPlayer player : targets) {
			Map<Identifier, SpeciesRecord> species = new HashMap<>(FishingAttachments.journal(player).species());
			registry.listElements().forEach(holder -> species.putIfAbsent(holder.key().identifier(),
					new SpeciesRecord(1, holder.value().size().weightAt(0.5F), day)));
			player.setAttached(FishingAttachments.JOURNAL, new FishingJournal(species, FishingAttachments.journal(player).treasures()));
		}

		context.getSource().sendSuccess(() -> Component.translatable("commands.examplemod.fishing.journal.complete", targets.size()), true);
		return targets.size();
	}

	private static int giveFish(CommandContext<CommandSourceStack> context, Optional<Float> weight) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		Identifier id = IdentifierArgument.getId(context, "species");
		Optional<Holder.Reference<FishSpecies>> species = speciesRegistry(context.getSource()).get(id);
		if (species.isEmpty()) {
			context.getSource().sendFailure(Component.translatable("commands.examplemod.fishing.unknown_species", id.toString()));
			return 0;
		}

		FishSpecies value = species.get().value();
		float weightKg = weight.orElseGet(() -> value.size().weightAt(player.getRandom().nextFloat()));
		float normalized = value.size().normalize(weightKg);
		FishEncounter encounter = new FishEncounter(species.get(), weightKg, normalized, false, 0.0F, 0, false);
		ItemStack fish = CatchRewards.createFish(encounter, Optional.of(player.getPlainTextName()));
		if (!player.addItem(fish)) {
			player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), fish));
		}

		return 1;
	}

	private static Registry<FishSpecies> speciesRegistry(CommandSourceStack source) {
		return source.registryAccess().lookupOrThrow(FishingRegistries.FISH_SPECIES);
	}
}
