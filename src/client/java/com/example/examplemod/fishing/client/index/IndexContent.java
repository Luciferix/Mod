package com.example.examplemod.fishing.client.index;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;

import com.example.examplemod.fishing.FishingFeature;
import com.example.examplemod.fishing.journal.FishingJournal;
import com.example.examplemod.fishing.journal.SpeciesRecord;
import com.example.examplemod.fishing.registry.FishingAttachments;
import com.example.examplemod.fishing.registry.FishingComponents;
import com.example.examplemod.fishing.registry.FishingRegistries;
import com.example.examplemod.fishing.species.FishRarity;
import com.example.examplemod.fishing.species.FishSpecies;

/**
 * What the Fishing Index shows, gathered from the synced species registry and the player's
 * journal each time the book opens.
 */
final class IndexContent {
	/** Treasure the Index lists up front, so players can see what they haven't found yet. */
	static final TagKey<Item> INDEX_TREASURES = TagKey.create(Registries.ITEM, FishingFeature.id("fishing_index_treasures"));

	static final Component UNKNOWN_NAME = Component.translatable("fishing.examplemod.index.unknown");

	private static final Comparator<SpeciesEntry> SPECIES_ORDER = Comparator
			.comparing((SpeciesEntry entry) -> entry.species().rarity())
			.thenComparing(entry -> entry.species().displayName().getString())
			.thenComparing(SpeciesEntry::id);

	private final List<SpeciesEntry> species;
	private final List<TreasureEntry> treasures;
	private final FishingJournal journal;
	private final Map<FishRarity, Integer> caughtByRarity = new EnumMap<>(FishRarity.class);
	private final int speciesDiscovered;
	private final int treasureKindsFound;

	private IndexContent(List<SpeciesEntry> species, List<TreasureEntry> treasures, FishingJournal journal) {
		this.species = species;
		this.treasures = treasures;
		this.journal = journal;
		int discovered = 0;
		for (SpeciesEntry entry : species) {
			if (entry.record().isPresent()) {
				discovered++;
				this.caughtByRarity.merge(entry.species().rarity(), entry.record().get().caught(), Integer::sum);
			}
		}

		this.speciesDiscovered = discovered;
		this.treasureKindsFound = (int) treasures.stream().filter(TreasureEntry::known).count();
	}

	static IndexContent gather(ClientLevel level, Player player) {
		FishingJournal journal = FishingAttachments.journal(player);
		Holder<Biome> here = level.getBiome(player.blockPosition());

		List<SpeciesEntry> species = new ArrayList<>();
		FishingRegistries.species(level.registryAccess()).ifPresent(registry -> registry.listElements().forEach(holder -> {
			Identifier id = holder.key().identifier();
			species.add(new SpeciesEntry(id, holder.value(), Optional.ofNullable(journal.record(id)), holder.value().livesIn(here)));
		}));
		species.sort(SPECIES_ORDER);

		List<TreasureEntry> treasures = new ArrayList<>();
		Set<Identifier> listed = new HashSet<>();
		BuiltInRegistries.ITEM.get(INDEX_TREASURES).ifPresent(tag -> tag.stream().forEach(holder -> {
			Identifier id = BuiltInRegistries.ITEM.getKey(holder.value());
			if (listed.add(id)) {
				treasures.add(new TreasureEntry(holder.value(), journal.treasures().getOrDefault(id, 0)));
			}
		}));

		// Treasure found outside the listed set (other loot, data packs) still gets a page.
		journal.treasures().keySet().stream()
				.filter(id -> !listed.contains(id))
				.sorted()
				.forEach(id -> BuiltInRegistries.ITEM.get(id).ifPresent(holder ->
						treasures.add(new TreasureEntry(holder.value(), journal.treasures().get(id)))));
		return new IndexContent(List.copyOf(species), List.copyOf(treasures), journal);
	}

	List<SpeciesEntry> species() {
		return this.species;
	}

	List<TreasureEntry> treasures() {
		return this.treasures;
	}

	int speciesDiscovered() {
		return this.speciesDiscovered;
	}

	int treasureKindsFound() {
		return this.treasureKindsFound;
	}

	int fishCaught() {
		return this.journal.totalCaught();
	}

	int treasuresFound() {
		return this.journal.totalTreasures();
	}

	int caught(FishRarity rarity) {
		return this.caughtByRarity.getOrDefault(rarity, 0);
	}

	/** Discovered species, heaviest personal best first. */
	List<SpeciesEntry> heaviestCatches(int limit) {
		return this.species.stream()
				.filter(entry -> entry.record().isPresent())
				.sorted(Comparator.comparingDouble((SpeciesEntry entry) -> entry.record().get().bestKg()).reversed())
				.limit(limit)
				.toList();
	}

	/** One cell of the Index grid. */
	interface Entry {
		boolean known();

		/** Whether the cell shows its item (a silhouette when unknown) rather than a question mark. */
		boolean showsIcon();

		ItemStack icon();

		Component name();

		/** Draws the "lives around here" marker on the cell. */
		default boolean marked() {
			return false;
		}
	}

	record SpeciesEntry(Identifier id, FishSpecies species, Optional<SpeciesRecord> record, boolean livesHere) implements Entry {
		@Override
		public boolean known() {
			return this.record.isPresent();
		}

		@Override
		public boolean showsIcon() {
			return true;
		}

		/** The fish itself, or a dark silhouette of it while undiscovered. */
		@Override
		public ItemStack icon() {
			ItemStack stack = new ItemStack(this.species.item());
			if (!this.known()) {
				stack.set(FishingComponents.INDEX_SILHOUETTE, Unit.INSTANCE);
			}

			return stack;
		}

		@Override
		public Component name() {
			return this.known() ? this.species.displayName() : UNKNOWN_NAME;
		}

		@Override
		public boolean marked() {
			return this.livesHere;
		}
	}

	record TreasureEntry(Item item, int found) implements Entry {
		@Override
		public boolean known() {
			return this.found > 0;
		}

		@Override
		public boolean showsIcon() {
			return this.known();
		}

		@Override
		public ItemStack icon() {
			return new ItemStack(this.item);
		}

		@Override
		public Component name() {
			return this.known() ? this.item.getName(this.item.getDefaultInstance()) : UNKNOWN_NAME;
		}
	}
}
