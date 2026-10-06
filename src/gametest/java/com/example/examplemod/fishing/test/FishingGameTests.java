package com.example.examplemod.fishing.test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.example.examplemod.fishing.FishingBalance;
import com.example.examplemod.fishing.FishingFeature;
import com.example.examplemod.fishing.catching.EncounterRoller;
import com.example.examplemod.fishing.catching.FishingController;
import com.example.examplemod.fishing.catching.FishingHookAccess;
import com.example.examplemod.fishing.catching.HookSession;
import com.example.examplemod.fishing.gear.Bait;
import com.example.examplemod.fishing.gear.GearStats;
import com.example.examplemod.fishing.gear.RodLoadout;
import com.example.examplemod.fishing.gear.TackleSlot;
import com.example.examplemod.fishing.journal.FishingJournal;
import com.example.examplemod.fishing.registry.FishingAttachments;
import com.example.examplemod.fishing.registry.FishingComponents;
import com.example.examplemod.fishing.registry.FishingItems;
import com.example.examplemod.fishing.registry.FishingRegistries;
import com.example.examplemod.fishing.species.FishRarity;
import com.example.examplemod.fishing.species.FishSpecies;
import com.example.examplemod.fishing.treasure.TreasureTier;

/**
 * Server game tests for the fishing rework, run by {@code ./gradlew runGameTest} (part of
 * {@code build}). They check that the feature's data loads (species, habitat tags, loot tables,
 * recipes, advancements, trades), that the core rules hold, and that a hooked fish can be landed.
 *
 * <p>Update the expected counts when content is added.
 */
public class FishingGameTests {
	private static final int SPECIES = 38;
	private static final int RECIPES = 24;
	private static final int ADVANCEMENTS = 13;
	private static final int MIN_INDEX_TREASURES = 20;
	private static final TagKey<Item> INDEX_TREASURES = TagKey.create(Registries.ITEM, FishingFeature.id("fishing_index_treasures"));

	@GameTest
	public void speciesDataLoads(GameTestHelper helper) {
		Registry<FishSpecies> registry = species(helper);
		helper.assertValueEqual(registry.size(), SPECIES, "number of fish species");

		Set<Identifier> baitTypes = new HashSet<>();
		for (Item bait : FishingItems.bait()) {
			Bait component = bait.components().get(FishingComponents.BAIT);
			helper.assertTrue(component != null, "bait item without a bait component: " + bait);
			baitTypes.add(component.type());
		}

		registry.listElements().forEach(holder -> {
			FishSpecies species = holder.value();
			String id = holder.key().identifier().toString();
			helper.assertFalse(species.item().value() == Items.AIR, id + " has no item");
			helper.assertTrue(species.size().minKg() < species.size().maxKg(), id + " has an empty size range");
			helper.assertFalse(species.habitats().isEmpty(), id + " has no habitat");
			for (HolderSet<Biome> habitat : species.habitats()) {
				helper.assertTrue(habitat.size() > 0, id + " has an empty habitat " + habitat.unwrapKey().map(Object::toString).orElse("list"));
			}

			helper.assertTrue(!species.bait().affinities().isEmpty() || species.bait().unbaited() > 0.0F, id + " bites on nothing");
			for (Identifier type : species.bait().affinities().keySet()) {
				helper.assertTrue(baitTypes.contains(type), id + " wants bait type " + type + " that no bait item has");
			}
		});
		helper.succeed();
	}

	@GameTest
	public void everyFishItemIsASpecies(GameTestHelper helper) {
		Set<Item> speciesItems = new HashSet<>();
		species(helper).listElements().forEach(holder -> speciesItems.add(holder.value().item().value()));
		for (Item fish : FishingItems.fish()) {
			helper.assertTrue(speciesItems.contains(fish), BuiltInRegistries.ITEM.getKey(fish) + " is not caught as any species");
		}

		helper.succeed();
	}

	@GameTest
	public void habitatsAndBaitDecideWhatBites(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Registry<FishSpecies> registry = species(helper);
		Holder<Biome> river = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.RIVER);
		LootContext context = new LootContext.Builder(fishingParams(helper, new ItemStack(Items.FISHING_ROD))).create(Optional.empty());

		Set<Identifier> bareHook = candidateIds(EncounterRoller.candidates(registry, river, context, Optional.empty()));
		Set<Identifier> insects = candidateIds(EncounterRoller.candidates(registry, river, context,
				Optional.of(new Bait(FishingFeature.id("insect"), 1.0F, false))));

		helper.assertTrue(bareHook.contains(FishingFeature.id("minnow")), "minnows should bite a bare hook in rivers");
		helper.assertFalse(bareHook.contains(FishingFeature.id("rainbow_trout")), "rainbow trout should ignore a bare hook");
		helper.assertTrue(insects.contains(FishingFeature.id("rainbow_trout")), "rainbow trout should bite insects in rivers");
		helper.assertFalse(bareHook.contains(FishingFeature.id("mackerel")) || insects.contains(FishingFeature.id("mackerel")),
				"mackerel should not live in rivers");
		helper.succeed();
	}

	@GameTest
	public void treasureTablesRoll(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		for (TreasureTier tier : TreasureTier.values()) {
			LootTable table = server.reloadableRegistries().getLootTable(tier.lootTable());
			helper.assertFalse(table == LootTable.EMPTY, "missing treasure table for " + tier.getSerializedName());
			helper.assertFalse(table.getRandomItems(fishingParams(helper, new ItemStack(Items.FISHING_ROD))).isEmpty(),
					tier.getSerializedName() + " treasure rolled nothing");
		}

		int listed = BuiltInRegistries.ITEM.get(INDEX_TREASURES).map(HolderSet::size).orElse(0);
		helper.assertTrue(listed >= MIN_INDEX_TREASURES, "the Fishing Index treasure tag lists only " + listed + " items");
		helper.succeed();
	}

	@GameTest
	public void recipesAdvancementsAndTradesLoad(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		long recipes = server.getRecipeManager().getRecipes().stream()
				.filter(holder -> holder.id().identifier().getNamespace().equals(FishingFeature.MOD_ID))
				.count();
		helper.assertValueEqual((int) recipes, RECIPES, "number of fishing recipes");

		long advancements = server.getAdvancements().getAllAdvancements().stream()
				.filter(holder -> holder.id().getNamespace().equals(FishingFeature.MOD_ID) && holder.id().getPath().startsWith("fishing/"))
				.count();
		helper.assertValueEqual((int) advancements, ADVANCEMENTS, "number of fishing advancements");

		boolean indexTrade = helper.getLevel().registryAccess().lookupOrThrow(Registries.TRADE_SET)
				.get(ResourceKey.create(Registries.TRADE_SET, FishingFeature.id("fisherman/fishing_index")))
				.isPresent();
		helper.assertTrue(indexTrade, "the Fishing Index trade set is missing");
		helper.succeed();
	}

	@GameTest
	public void tackleAddsToRodStats(GameTestHelper helper) {
		helper.assertTrue(new ItemStack(Items.FISHING_ROD).has(FishingComponents.ROD_STATS), "the vanilla rod should have rod stats");

		ItemStack rod = new ItemStack(FishingItems.IRON_FISHING_ROD);
		RodLoadout loadout = RodLoadout.EMPTY
				.with(TackleSlot.HOOK, Optional.of(ItemStackTemplate.fromNonEmptyStack(new ItemStack(FishingItems.BARBED_HOOK), 1)))
				.with(TackleSlot.LINE, Optional.of(ItemStackTemplate.fromNonEmptyStack(new ItemStack(FishingItems.BRAIDED_LINE), 1)));
		rod.set(FishingComponents.ROD_LOADOUT, loadout);
		GearStats stats = GearStats.of(rod);
		helper.assertValueEqual(stats.catchDamage(), 10.0F, "catch damage of an iron rod with a barbed hook");
		helper.assertValueEqual(stats.lineStrength(), 31.0F, "line strength of an iron rod with a braided line");
		helper.succeed();
	}

	/** The HP bands the design asks for, at an average size and difficulty. */
	@GameTest
	public void fishHpMatchesTheDesign(GameTestHelper helper) {
		helper.assertTrue(hp(FishRarity.COMMON) < 60.0F, "Common fish should have about 40 HP");
		float rare = hp(FishRarity.RARE);
		helper.assertTrue(rare >= 150.0F && rare <= 250.0F, "Rare fish should have 150-250 HP, not " + rare);
		helper.assertTrue(hp(FishRarity.EPIC) >= 300.0F, "Epic fish should have 300+ HP");
		helper.assertTrue(hp(FishRarity.LEGENDARY) >= 500.0F, "Legendary fish should have 500+ HP");
		helper.succeed();
	}

	@GameTest
	public void journalKeepsRecordsAndSaves(GameTestHelper helper) {
		Identifier minnow = FishingFeature.id("minnow");
		FishingJournal journal = FishingJournal.EMPTY.withCatch(minnow, 0.02F, 3L).withCatch(minnow, 0.05F, 4L).withTreasure(Identifier.withDefaultNamespace("saddle"));
		helper.assertValueEqual(journal.record(minnow).caught(), 2, "minnows caught");
		helper.assertValueEqual(journal.record(minnow).bestKg(), 0.05F, "best minnow weight");
		helper.assertValueEqual(journal.record(minnow).firstCaughtDay(), 3L, "first catch day");

		JsonElement saved = FishingJournal.CODEC.encodeStart(JsonOps.INSTANCE, journal).getOrThrow();
		FishingJournal loaded = FishingJournal.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
		helper.assertValueEqual(loaded, journal, "journal after saving and loading");
		helper.succeed();
	}

	@GameTest
	public void noviceFishermanSellsTheIndex(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
		MinecraftServer server = helper.getLevel().getServer();
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
				"summon minecraft:villager " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
						+ " {NoAI:1b,VillagerData:{level:1,profession:\"minecraft:fisherman\",type:\"minecraft:plains\"}}");
		List<Villager> villagers = helper.getLevel().getEntitiesOfClass(Villager.class, new AABB(pos).inflate(2.0));
		helper.assertFalse(villagers.isEmpty(), "the fisherman was not summoned");
		boolean sellsIndex = villagers.getFirst().getOffers().stream().anyMatch(offer -> offer.getResult().is(FishingItems.FISHING_INDEX));
		helper.assertTrue(sellsIndex, "a novice fisherman should sell the Fishing Index");
		helper.succeed();
	}

	/** Bite, set the hook, reel until landed: the fish ends up in the journal. */
	@GameTest(maxTicks = 300)
	public void hookedFishCanBeLanded(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Vec3 standing = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2)));
		player.setPos(standing.x, standing.y, standing.z);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FishingItems.NETHERITE_FISHING_ROD));

		FishingHook hook = new FishingHook(player, level, 0, 0);
		hook.setPos(standing.x + 2.0, standing.y, standing.z);
		hook.setDeltaMovement(Vec3.ZERO);
		level.addFreshEntity(hook);

		HookSession session = FishingHookAccess.session(hook);
		FishingController.onVanillaBite(hook);
		helper.assertTrue(session.phase() == HookSession.Phase.BITING, "a fish should bite");
		FishingController.onRodUse(player, hook);
		helper.assertTrue(session.phase() == HookSession.Phase.FIGHTING, "setting the hook should start the fight");

		helper.succeedWhen(() -> {
			if (!hook.isRemoved()) {
				FishingController.onRodUse(player, hook);
			}

			helper.assertTrue(hook.isRemoved(), "the fish is still on the line");
			helper.assertValueEqual(FishingAttachments.journal(player).totalCaught(), 1, "fish in the journal");
		});
	}

	private static Registry<FishSpecies> species(GameTestHelper helper) {
		return helper.getLevel().registryAccess().lookupOrThrow(FishingRegistries.FISH_SPECIES);
	}

	private static LootParams fishingParams(GameTestHelper helper, ItemStack tool) {
		return new LootParams.Builder(helper.getLevel())
				.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
				.withParameter(LootContextParams.TOOL, tool)
				.create(LootContextParamSets.FISHING);
	}

	private static Set<Identifier> candidateIds(List<EncounterRoller.Candidate> candidates) {
		Set<Identifier> ids = new HashSet<>();
		for (EncounterRoller.Candidate candidate : candidates) {
			ids.add(candidate.species().key().identifier());
		}

		return ids;
	}

	private static float hp(FishRarity rarity) {
		return FishingBalance.fishHp(rarity, 1.0F, 0.5F, false);
	}
}
