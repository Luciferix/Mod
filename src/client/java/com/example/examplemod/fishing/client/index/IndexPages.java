package com.example.examplemod.fishing.client.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;

import com.example.examplemod.fishing.catching.CaughtFish;
import com.example.examplemod.fishing.gear.Bait;
import com.example.examplemod.fishing.journal.SpeciesRecord;
import com.example.examplemod.fishing.species.BaitPreferences;
import com.example.examplemod.fishing.species.FishRarity;
import com.example.examplemod.fishing.species.FishSpecies;
import com.example.examplemod.fishing.species.SizeClass;

/** What each page of the Fishing Index says. */
final class IndexPages {
	private static final int DESCRIPTION_LINES = 4;
	private static final int HINT_LINES = 6;
	private static final int HEAVIEST_ROWS = 7;

	private IndexPages() {
	}

	// ---- Fish ----

	static void speciesOverview(PageWriter page, IndexContent content) {
		page.heading(Component.translatable("fishing.examplemod.index.species.title"));
		int total = content.species().size();
		if (total == 0) {
			page.paragraph(Component.translatable("fishing.examplemod.index.species.none"), PageWriter.MUTED_INK, HINT_LINES);
			return;
		}

		progress(page, content.speciesDiscovered(), total);
	}

	static void speciesDetails(PageWriter page, IndexContent.SpeciesEntry entry) {
		FishSpecies species = entry.species();
		page.iconHeader(entry.icon(), entry.name(), PageWriter.rarityName(species.rarity()));
		page.separator();
		if (entry.record().isEmpty()) {
			Component hint = species.hint().orElse(Component.translatable("fishing.examplemod.index.undiscovered"));
			page.paragraph(hint.copy().withStyle(ChatFormatting.ITALIC), PageWriter.MUTED_INK, HINT_LINES);
			livesHere(page, entry);
			return;
		}

		SpeciesRecord record = entry.record().get();
		page.paragraph(species.description(), PageWriter.INK, DESCRIPTION_LINES);
		page.separator();
		page.row(label("habitat"), habitats(species));
		page.row(label("bait"), baits(species.bait()));
		page.row(label("size"), Component.translatable("fishing.examplemod.index.size_range",
				CaughtFish.formatWeight(species.size().minKg()), CaughtFish.formatWeight(species.size().maxKg())));
		SizeClass bestClass = SizeClass.of(species.size().normalize(record.bestKg()));
		page.row(label("record"), Component.translatable("fishing.examplemod.index.record_value",
				CaughtFish.formatWeight(record.bestKg()), sizeClassName(bestClass)));
		page.row(label("caught"), record.firstCaughtDay() > 0
				? Component.translatable("fishing.examplemod.index.caught_since", record.caught(), record.firstCaughtDay())
				: Component.literal(Integer.toString(record.caught())));
		livesHere(page, entry);
	}

	private static void livesHere(PageWriter page, IndexContent.SpeciesEntry entry) {
		if (entry.livesHere()) {
			page.skip(3);
			page.paragraph(Component.translatable("fishing.examplemod.index.lives_here"), PageWriter.GOOD_INK, 2);
		}
	}

	// ---- Treasure ----

	static void treasureOverview(PageWriter page, IndexContent content) {
		page.heading(Component.translatable("fishing.examplemod.index.treasure.title"));
		int total = content.treasures().size();
		if (total == 0) {
			page.paragraph(Component.translatable("fishing.examplemod.index.treasure.none"), PageWriter.MUTED_INK, HINT_LINES);
			return;
		}

		progress(page, content.treasureKindsFound(), total);
	}

	static void treasureDetails(PageWriter page, IndexContent.@Nullable TreasureEntry entry) {
		if (entry != null) {
			Component status = entry.known()
					? Component.translatable("fishing.examplemod.index.found_times", entry.found())
					: Component.translatable("fishing.examplemod.index.not_found");
			page.iconHeader(entry.known() ? entry.icon() : null, entry.name(), status);
			page.separator();
		}

		page.paragraph(Component.translatable("fishing.examplemod.index.treasure.help"), PageWriter.MUTED_INK, 10);
	}

	// ---- Records ----

	static void records(PageWriter page, IndexContent content) {
		page.heading(Component.translatable("fishing.examplemod.index.records.title"));
		page.row(label("fish_caught"), Component.literal(Integer.toString(content.fishCaught())));
		page.row(label("species"), Component.translatable("fishing.examplemod.index.fraction", content.speciesDiscovered(), content.species().size()));
		page.row(label("treasure"), Component.translatable("fishing.examplemod.index.treasure_value", content.treasuresFound(), content.treasureKindsFound()));
		page.separator();
		page.centered(Component.translatable("fishing.examplemod.index.records.by_rarity"), PageWriter.MUTED_INK);
		page.skip(2);
		for (FishRarity rarity : FishRarity.values()) {
			page.row(PageWriter.rarityName(rarity), Component.literal(Integer.toString(content.caught(rarity))));
		}
	}

	static void heaviestCatches(PageWriter page, IndexContent content) {
		page.heading(Component.translatable("fishing.examplemod.index.records.heaviest"));
		List<IndexContent.SpeciesEntry> heaviest = content.heaviestCatches(HEAVIEST_ROWS);
		if (heaviest.isEmpty()) {
			page.paragraph(Component.translatable("fishing.examplemod.index.records.empty"), PageWriter.MUTED_INK, HINT_LINES);
			return;
		}

		for (IndexContent.SpeciesEntry entry : heaviest) {
			page.itemRow(entry.icon(), entry.name(), Component.literal(CaughtFish.formatWeight(entry.record().orElseThrow().bestKg())));
		}
	}

	// ---- Helpers ----

	private static void progress(PageWriter page, int found, int total) {
		int percent = Math.round(100.0F * found / total);
		page.centered(Component.translatable("fishing.examplemod.index.progress", found, total, percent), PageWriter.MUTED_INK);
		page.skip(2);
		page.progress((float) found / total);
	}

	private static Component label(String name) {
		return Component.translatable("fishing.examplemod.index.label." + name);
	}

	private static Component sizeClassName(SizeClass sizeClass) {
		return Component.translatable("fishing.examplemod.size_class." + sizeClass.getSerializedName());
	}

	private static Component habitats(FishSpecies species) {
		List<Component> names = new ArrayList<>();
		for (HolderSet<Biome> habitat : species.habitats()) {
			names.add(habitatName(habitat));
		}

		return join(names);
	}

	/**
	 * A habitat tag reads as its translated name ({@code tag.worldgen.biome.<namespace>.<path>}),
	 * a plain biome list as the biomes' names.
	 */
	private static Component habitatName(HolderSet<Biome> habitat) {
		return habitat.unwrapKey().<Component>map(tag -> {
			Identifier id = tag.location();
			String key = "tag.worldgen.biome." + id.getNamespace() + "." + id.getPath().replace('/', '.');
			return Component.translatableWithFallback(key, prettify(id.getPath()));
		}).orElseGet(() -> join(habitat.stream().map(IndexPages::biomeName).toList()));
	}

	private static Component biomeName(Holder<Biome> biome) {
		return biome.unwrapKey()
				.<Component>map(key -> Component.translatable("biome." + key.identifier().getNamespace() + "." + key.identifier().getPath()))
				.orElse(Component.literal("?"));
	}

	/** Bait types the species bites, strongest attraction first. */
	private static Component baits(BaitPreferences preferences) {
		List<Component> names = new ArrayList<>();
		preferences.affinities().entrySet().stream()
				.filter(entry -> entry.getValue() > 0.0F)
				.sorted(Map.Entry.<Identifier, Float>comparingByValue().reversed())
				.forEach(entry -> names.add(Bait.typeName(entry.getKey())));
		if (preferences.unbaited() > 0.0F) {
			names.add(Component.translatable("fishing.examplemod.index.bare_hook"));
		}

		return names.isEmpty() ? IndexContent.UNKNOWN_NAME : join(names);
	}

	private static Component join(List<Component> parts) {
		MutableComponent joined = Component.empty();
		for (int i = 0; i < parts.size(); i++) {
			if (i > 0) {
				joined.append(", ");
			}

			joined.append(parts.get(i));
		}

		return joined;
	}

	/** "fishing_habitat/warm_oceans" → "Warm Oceans", for tags without a translation. */
	private static String prettify(String path) {
		String name = path.substring(path.lastIndexOf('/') + 1);
		StringBuilder pretty = new StringBuilder();
		for (String word : name.split("_")) {
			if (!word.isEmpty()) {
				if (!pretty.isEmpty()) {
					pretty.append(' ');
				}

				pretty.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
			}
		}

		return pretty.toString();
	}
}
