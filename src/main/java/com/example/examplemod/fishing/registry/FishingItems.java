package com.example.examplemod.fishing.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;

import com.example.examplemod.fishing.FishingFeature;
import com.example.examplemod.fishing.gear.Bait;
import com.example.examplemod.fishing.gear.FishingRodStats;
import com.example.examplemod.fishing.gear.GearModifiers;
import com.example.examplemod.fishing.gear.GearStat;
import com.example.examplemod.fishing.gear.Tackle;
import com.example.examplemod.fishing.gear.TackleSlot;
import com.example.examplemod.fishing.item.FishingIndexItem;

public final class FishingItems {
	private static final List<Item> FISH = new ArrayList<>();
	private static final List<Item> RODS = new ArrayList<>();
	private static final List<Item> TACKLE = new ArrayList<>();
	private static final List<Item> BAIT = new ArrayList<>();

	// ---- Fish (vanilla cod, salmon, tropical fish and pufferfish are species too) ----
	public static final Item MINNOW = fish("minnow");
	public static final Item BLUEGILL = fish("bluegill");
	public static final Item PERCH = fish("perch");
	public static final Item RAINBOW_TROUT = fish("rainbow_trout");
	public static final Item CARP = fish("carp");
	public static final Item PIKE = fish("pike");
	public static final Item STURGEON = fish("sturgeon");
	public static final Item ANCIENT_KOI = fish("ancient_koi");
	public static final Item RIVER_MONARCH = fish("river_monarch");
	public static final Item MUDSKIPPER = fish("mudskipper");
	public static final Item CATFISH = fish("catfish");
	public static final Item BOWFIN = fish("bowfin");
	public static final Item BOG_LURKER = fish("bog_lurker");
	public static final Item PEACOCK_BASS = fish("peacock_bass");
	public static final Item PIRANHA = fish("piranha");
	public static final Item ARAPAIMA = fish("arapaima");
	public static final Item MACKEREL = fish("mackerel");
	public static final Item SEA_BASS = fish("sea_bass");
	public static final Item HALIBUT = fish("halibut");
	public static final Item BLUEFIN_TUNA = fish("bluefin_tuna");
	public static final Item CLOWNFISH = fish("clownfish");
	public static final Item MAHI_MAHI = fish("mahi_mahi");
	public static final Item SWORDFISH = fish("swordfish");
	public static final Item STORMCALLER_MARLIN = fish("stormcaller_marlin");
	public static final Item ARCTIC_CHAR = fish("arctic_char");
	public static final Item ICEFISH = fish("icefish");
	public static final Item FROSTJAW = fish("frostjaw");
	public static final Item GLACIAL_BEHEMOTH = fish("glacial_behemoth");
	public static final Item ANGLERFISH = fish("anglerfish");
	public static final Item COELACANTH = fish("coelacanth");
	public static final Item ABYSSAL_LEVIATHAN = fish("abyssal_leviathan");
	public static final Item BLIND_CAVEFISH = fish("blind_cavefish");
	public static final Item LUMEN_EEL = fish("lumen_eel");
	public static final Item SPOREFIN = fish("sporefin");

	public static final Item COOKED_FISH = register("cooked_fish", Item::new, new Item.Properties()
			.food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.8F).build()));

	// ---- Rods (the vanilla fishing rod is the first tier) ----
	public static final Item COPPER_FISHING_ROD = rod("copper_fishing_rod", 96, 8, Items.COPPER_INGOT, GearModifiers.builder()
			.set(GearStat.CATCH_DAMAGE, 5.0F)
			.set(GearStat.SIZE_BONUS, 0.05F)
			.set(GearStat.TREASURE_CHANCE, 0.0075F)
			.set(GearStat.LINE_STRENGTH, 10.0F), new Item.Properties());
	public static final Item IRON_FISHING_ROD = rod("iron_fishing_rod", 192, 9, Items.IRON_INGOT, GearModifiers.builder()
			.set(GearStat.CATCH_DAMAGE, 7.0F)
			.set(GearStat.LUCK, 1.0F)
			.set(GearStat.SIZE_BONUS, 0.10F)
			.set(GearStat.TREASURE_CHANCE, 0.01F)
			.set(GearStat.LINE_STRENGTH, 16.0F), new Item.Properties());
	public static final Item GOLDEN_FISHING_ROD = rod("golden_fishing_rod", 48, 22, Items.GOLD_INGOT, GearModifiers.builder()
			.set(GearStat.CATCH_DAMAGE, 5.0F)
			.set(GearStat.LUCK, 3.0F)
			.set(GearStat.TREASURE_CHANCE, 0.03F)
			.set(GearStat.LINE_STRENGTH, 8.0F), new Item.Properties());
	public static final Item DIAMOND_FISHING_ROD = rod("diamond_fishing_rod", 512, 10, Items.DIAMOND, GearModifiers.builder()
			.set(GearStat.CATCH_DAMAGE, 10.0F)
			.set(GearStat.LUCK, 2.0F)
			.set(GearStat.SIZE_BONUS, 0.20F)
			.set(GearStat.TREASURE_CHANCE, 0.015F)
			.set(GearStat.LINE_STRENGTH, 30.0F), new Item.Properties());
	public static final Item NETHERITE_FISHING_ROD = rod("netherite_fishing_rod", 768, 15, Items.NETHERITE_INGOT, GearModifiers.builder()
			.set(GearStat.CATCH_DAMAGE, 13.0F)
			.set(GearStat.LUCK, 3.0F)
			.set(GearStat.SIZE_BONUS, 0.30F)
			.set(GearStat.TREASURE_CHANCE, 0.02F)
			.set(GearStat.LINE_STRENGTH, 50.0F), new Item.Properties().fireResistant());
	/** Only found as legendary fishing treasure. */
	public static final Item HEIRLOOM_FISHING_ROD = rod("heirloom_fishing_rod", 600, 18, Items.PRISMARINE_CRYSTALS, GearModifiers.builder()
			.set(GearStat.CATCH_DAMAGE, 11.0F)
			.set(GearStat.LUCK, 5.0F)
			.set(GearStat.SIZE_BONUS, 0.25F)
			.set(GearStat.TREASURE_CHANCE, 0.04F)
			.set(GearStat.LINE_STRENGTH, 40.0F)
			.set(GearStat.PULL_RESISTANCE, 0.15F), new Item.Properties().rarity(Rarity.EPIC));

	// ---- Tackle ----
	public static final Item BARBED_HOOK = tackle("barbed_hook", TackleSlot.HOOK, GearModifiers.builder()
			.set(GearStat.CATCH_DAMAGE, 3.0F));
	public static final Item HEAVY_HOOK = tackle("heavy_hook", TackleSlot.HOOK, GearModifiers.builder()
			.set(GearStat.SIZE_BONUS, 0.20F)
			.set(GearStat.LINE_STRENGTH, 10.0F));
	public static final Item GILDED_HOOK = tackle("gilded_hook", TackleSlot.HOOK, GearModifiers.builder()
			.set(GearStat.TREASURE_CHANCE, 0.025F));
	public static final Item LUCKY_HOOK = tackle("lucky_hook", TackleSlot.HOOK, GearModifiers.builder()
			.set(GearStat.LUCK, 2.0F));
	public static final Item BRAIDED_LINE = tackle("braided_line", TackleSlot.LINE, GearModifiers.builder()
			.set(GearStat.LINE_STRENGTH, 15.0F)
			.set(GearStat.PULL_RESISTANCE, 0.10F));
	public static final Item REINFORCED_LINE = tackle("reinforced_line", TackleSlot.LINE, GearModifiers.builder()
			.set(GearStat.LINE_STRENGTH, 40.0F)
			.set(GearStat.PULL_RESISTANCE, 0.25F));
	public static final Item PHANTOM_LINE = tackle("phantom_line", TackleSlot.LINE, GearModifiers.builder()
			.set(GearStat.LINE_STRENGTH, 100.0F)
			.set(GearStat.PULL_RESISTANCE, 0.40F));
	public static final Item COPPER_REEL = tackle("copper_reel", TackleSlot.REEL, GearModifiers.builder()
			.set(GearStat.CATCH_DAMAGE_PERCENT, 0.15F));
	public static final Item IRON_REEL = tackle("iron_reel", TackleSlot.REEL, GearModifiers.builder()
			.set(GearStat.CATCH_DAMAGE_PERCENT, 0.30F));
	public static final Item CLOCKWORK_REEL = tackle("clockwork_reel", TackleSlot.REEL, GearModifiers.builder()
			.set(GearStat.CATCH_DAMAGE_PERCENT, 0.10F)
			.set(GearStat.FIGHT_TIME, 0.30F));

	// ---- Bait and lures ----
	public static final Item WORM = bait("worm", "worm", 1.0F, false);
	public static final Item CRICKET = bait("cricket", "insect", 1.0F, false);
	public static final Item CUT_BAIT = bait("cut_bait", "fish", 1.0F, false);
	public static final Item DOUGH_BALL = bait("dough_ball", "dough", 1.0F, false);
	public static final Item GLOW_BAIT = bait("glow_bait", "glow", 1.0F, false);
	public static final Item GOLDEN_GRUB = bait("golden_grub", "golden", 1.0F, false);
	public static final Item SPINNER_LURE = bait("spinner_lure", "fish", 0.6F, true);
	public static final Item SHIMMER_LURE = bait("shimmer_lure", "shimmer", 1.0F, true);

	public static final Item FISHING_INDEX = register("fishing_index", FishingIndexItem::new, new Item.Properties()
			.stacksTo(1)
			.rarity(Rarity.UNCOMMON));

	/** Base stats of the vanilla fishing rod, added through default components. */
	public static final FishingRodStats WOODEN_ROD_STATS = FishingRodStats.FALLBACK;

	private FishingItems() {
	}

	public static void init() {
		DefaultItemComponentEvents.MODIFY.register(context ->
				context.modify(Items.FISHING_ROD, builder -> builder.set(FishingComponents.ROD_STATS, WOODEN_ROD_STATS)));
	}

	public static List<Item> fish() {
		return Collections.unmodifiableList(FISH);
	}

	public static List<Item> rods() {
		return Collections.unmodifiableList(RODS);
	}

	public static List<Item> tackle() {
		return Collections.unmodifiableList(TACKLE);
	}

	public static List<Item> bait() {
		return Collections.unmodifiableList(BAIT);
	}

	private static Item fish(String name) {
		Item item = register(name, Item::new, new Item.Properties()
				.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.1F).build()));
		FISH.add(item);
		return item;
	}

	private static Item rod(String name, int durability, int enchantability, Item repairMaterial, GearModifiers.Builder stats, Item.Properties properties) {
		Item item = register(name, FishingRodItem::new, properties
				.durability(durability)
				.enchantable(enchantability)
				.repairable(repairMaterial)
				.component(FishingComponents.ROD_STATS, new FishingRodStats(stats.build())));
		RODS.add(item);
		return item;
	}

	private static Item tackle(String name, TackleSlot slot, GearModifiers.Builder modifiers) {
		Item item = register(name, Item::new, new Item.Properties()
				.stacksTo(16)
				.component(FishingComponents.TACKLE, new Tackle(slot, modifiers.build())));
		TACKLE.add(item);
		return item;
	}

	private static Item bait(String name, String type, float potency, boolean reusable) {
		Item item = register(name, Item::new, new Item.Properties()
				.stacksTo(reusable ? 16 : 64)
				.component(FishingComponents.BAIT, new Bait(FishingFeature.id(type), potency, reusable)));
		BAIT.add(item);
		return item;
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, FishingFeature.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}
}
