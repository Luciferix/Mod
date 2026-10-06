package com.example.examplemod.fishing.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import com.example.examplemod.fishing.FishingFeature;
import com.example.examplemod.fishing.species.FishRarity;

/**
 * Sound events of the fishing rework. All of them currently point at vanilla sounds in
 * {@code assets/examplemod/sounds.json} as placeholders (see ART_HANDOFF.md).
 */
public final class FishingSounds {
	public static final SoundEvent BITE = register("fishing.bite");
	public static final SoundEvent REVEAL_TICK = register("fishing.reveal_tick");
	public static final SoundEvent REVEAL_UNCOMMON = register("fishing.reveal.uncommon");
	public static final SoundEvent REVEAL_RARE = register("fishing.reveal.rare");
	public static final SoundEvent REVEAL_EPIC = register("fishing.reveal.epic");
	public static final SoundEvent REVEAL_LEGENDARY = register("fishing.reveal.legendary");
	public static final SoundEvent HOOK_SET = register("fishing.hook_set");
	public static final SoundEvent REEL = register("fishing.reel");
	public static final SoundEvent REEL_CRITICAL = register("fishing.reel_critical");
	public static final SoundEvent FISH_PULL = register("fishing.fish_pull");
	public static final SoundEvent LINE_STRAIN = register("fishing.line_strain");
	public static final SoundEvent LOW_HP = register("fishing.low_hp");
	public static final SoundEvent CATCH = register("fishing.catch");
	public static final SoundEvent CATCH_RARE = register("fishing.catch_rare");
	public static final SoundEvent ESCAPE = register("fishing.escape");
	public static final SoundEvent TREASURE = register("fishing.treasure");
	public static final SoundEvent NEW_SPECIES = register("fishing.new_species");
	public static final SoundEvent NEW_RECORD = register("fishing.new_record");
	public static final SoundEvent TACKLE_ATTACH = register("fishing.tackle_attach");
	public static final SoundEvent TACKLE_DETACH = register("fishing.tackle_detach");
	public static final SoundEvent INDEX_OPEN = register("fishing.index_open");
	public static final SoundEvent INDEX_PAGE = register("fishing.index_page");

	private FishingSounds() {
	}

	public static void init() {
	}

	/** Sting played when the bite indicator settles on this rarity, or null for Common. */
	public static SoundEvent revealSound(FishRarity rarity) {
		return switch (rarity) {
			case COMMON -> null;
			case UNCOMMON -> REVEAL_UNCOMMON;
			case RARE -> REVEAL_RARE;
			case EPIC -> REVEAL_EPIC;
			case LEGENDARY -> REVEAL_LEGENDARY;
		};
	}

	private static SoundEvent register(String name) {
		Identifier id = FishingFeature.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}
}
