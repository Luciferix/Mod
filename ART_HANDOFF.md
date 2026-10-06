# Fishing Rework: art and polish handoff

This file is for the AI (or person) doing the art and polish pass on the fishing rework. It lists every
placeholder asset, where it lives, what the code expects from it, and what still needs checking in game.
Keep it up to date: when you replace something, change its status here.

Everything below was built and compiled without launching Minecraft. Nothing visual has been seen in game
yet, so treat every size, position and color as a first guess.

## Quick facts

- Mod id `examplemod` (a placeholder; see [MERGING.md](MERGING.md)). All paths below are under
  `src/main/resources/assets/examplemod/` unless they start with `assets/minecraft/`.
- Every texture listed here is a generated placeholder: flat pixel art from a script, not hand-made.
  Replacing a file at the same path with the same size needs no code change.
- Sizes marked **fixed** are hard-coded in Java. Change the constant named in the table if you change the size.
- "Tinted" means the code multiplies the texture by a color, so keep those pixels white or gray.
- Blockbench MCP was not available in the environment that built this, so there are no 3D models yet.
  Every item uses a flat `item/generated` (or `item/handheld_rod`) model.

## Priorities

| Priority | What | Why |
|---|---|---|
| P1 | Bite indicator, fight HUD bars | Seen on every single catch |
| P1 | The 34 fish icons | The collection is the heart of the feature; they also become the Index silhouettes |
| P1 | Fishing Index book and its sprites | The main UI of the feature |
| P2 | Rod textures (normal and cast) | Held all the time |
| P2 | Sounds (all 24 events) | Currently vanilla stand-ins, some reused for several events |
| P3 | Tackle and bait icons, cooked fish, Index item icon | Inventory-only |
| P3 | Particles, advancement tab background | Vanilla stand-ins that work fine for now |

## 1. In-world bite indicator

| File | Size | Notes |
|---|---|---|
| `textures/entity/fishing/bite_indicator.png` | 48×16, **fixed** (3 frames of 16×16) | Strip of three frames, left to right: **halo** (soft round glow), **glyph** (the "!"), **rays** (spinning burst, Epic and Legendary only). All three are tinted with the rarity color, so draw them white with alpha. The glyph's dark outline stays dark. |

Code: `src/client/java/.../fishing/client/render/BiteIndicatorRenderer.java`, drawn from
`client/mixin/FishingHookRendererMixin.java`. Render type `entityTranslucent`, full-bright.

- Layout constants: `HEIGHT_ABOVE_HOOK` (1.0 block), `GLYPH_SIZE` (0.55), `HALO_SIZE` (1.0), `RAYS_SIZE` (1.6), `FRAME_COUNT`.
- Animation is all code-driven:
  - It pops in with an overshoot, bobs up and down, and pops again at each reveal step.
  - It climbs white → green → blue → purple → gold and stops at the fish's rarity (`FishingBalance.REVEAL_STEP_TICKS` per step).
  - During the fight it pulses, faster at low HP. It flashes red and shakes when the fish pulls back, squashes on each reel and pops on a critical reel.
  - It sinks and fades gray when the fish escapes or loses interest.
- Rarity colors live in `FishRarity` (`0xFFFFFF`, `0x55FF55`, `0x55AAFF`, `0xC060FF`, `0xFFD84A`).
- Ideas: a hand-drawn "!" with a stronger silhouette, a separate frame for "fish pulling" (e.g. "!!"), a splash ring under the bobber on a bite.

## 2. Fight HUD (under the crosshair)

| File | Size | Notes |
|---|---|---|
| `textures/gui/sprites/fishing/hud/hp_bar_background.png` | 122×7, **fixed** | Frame of the fish HP bar |
| `textures/gui/sprites/fishing/hud/hp_bar_fill.png` | 120×5, **fixed** | **Tinted** with the rarity color (plus white flashes, red on pull). Drawn partially from the left, plus a pale "ghost" segment of recent damage |
| `textures/gui/sprites/fishing/hud/timer_bar_background.png` | 122×5, **fixed** | Frame of the fight timer and the bite window bar |
| `textures/gui/sprites/fishing/hud/timer_bar_fill.png` | 120×3, **fixed** | **Tinted** white → yellow → red as time runs out, blinking at the end |

Code: `src/client/java/.../fishing/client/hud/FightHud.java` (constants `BAR_WIDTH`, `BAR_HEIGHT`, `TIMER_HEIGHT`, colors at the top).

- What it shows:
  - While a fish bites: "Something's biting!" (or "<Rarity> bite!" once the reveal settles), "Press <use key> to set the hook", and a shrinking bite-window bar.
  - While fighting: the rarity name, the HP bar, the timer bar, "HP / max" and seconds left, and an orange "Line strained" warning when the fish outweighs the line.
- Ideas: a small fish or rod icon beside the bar, a tension gauge, number pop-ups for critical reels.

## 3. Fishing Index (book screen)

| File | Size | Notes |
|---|---|---|
| `textures/gui/fishing_index/book.png` | 300×190, **fixed** | The two-page spread. Left page text area x 18..140, right page x 160..282, from y 14. Grid of 5×4 slots at y 50, page arrows at y 160 |
| `textures/gui/sprites/fishing/index/slot.png` | 22×22, **fixed** | Grid cell |
| `textures/gui/sprites/fishing/index/slot_highlighted.png` | 22×22 | Hovered or focused cell |
| `textures/gui/sprites/fishing/index/slot_selected.png` | 22×22 | Selected cell |
| `textures/gui/sprites/fishing/index/tab.png`, `tab_highlighted.png`, `tab_selected.png` | 26×24, **fixed** | Bookmark ribbons above the right page. The selected one is drawn 3 px higher. The tab icons are vanilla items (tropical fish, nautilus shell, book and quill) |
| `textures/gui/sprites/fishing/index/unknown.png` | 16×16 | "?" for treasure not found yet, also drawn at 32×32 in the detail header |
| `textures/gui/sprites/fishing/index/here_marker.png` | 7×7 | Corner marker on fish that live in the biome the player is standing in |
| `textures/gui/sprites/fishing/index/icon_frame.png` | 36×36, **fixed** | Frame around the double-size icon on the detail page |
| `textures/gui/sprites/fishing/index/progress_background.png` | 100×5, **fixed** | Collection progress bar |
| `textures/gui/sprites/fishing/index/progress_fill.png` | 98×3 | Its fill (not tinted) |

Code: `src/client/java/.../fishing/client/index/`
- `FishingIndexScreen` holds the layout constants.
- `PageWriter` holds the ink colors and the header, row and progress-bar drawing.
- `IndexPages` holds what each page says.

The page arrows are vanilla `PageButton`s.

**Silhouettes:** undiscovered fish are drawn with their own item model tinted dark brown. Every fish item
definition wraps its model in a `minecraft:condition` on the `examplemod:index_silhouette` component,
with a `minecraft:constant` tint (`0xFF2A2014`) on layer 0. Keep this when replacing fish models. If a fish
gets a 3D model, its faces need `tintindex: 0` for the silhouette to work.

The same wrapper overrides four vanilla files, which are also listed as shared files in MERGING.md:
`assets/minecraft/items/cod.json`, `salmon.json`, `tropical_fish.json` and `pufferfish.json`.

Ideas:
- A proper leather and brass book cover.
- Hand-lettered titles, page-curl corners and ink stamps for "caught" and "trophy".
- A page-turn animation.

## 4. Item textures (all 16×16)

All in `textures/item/`, models in `models/item/`, item definitions in `items/`.

### Fish (34). Status: generated placeholder

Side view, head facing left. Each has a distinct palette and pattern; most need a real artist's eye.
In-game sizes run from 5 g minnows to 800 kg leviathans, so the art could hint at scale.

| Item | Rarity | Look it should have |
|---|---|---|
| `minnow`, `bluegill`, `perch`, `mudskipper`, `piranha`, `mackerel`, `clownfish`, `icefish`, `blind_cavefish` | Common | Simple, readable everyday fish |
| `rainbow_trout`, `carp`, `catfish`, `peacock_bass`, `sea_bass`, `arctic_char`, `sporefin` | Uncommon | Sporefin: red with white spots, like a mooshroom |
| `pike`, `bowfin`, `halibut`, `mahi_mahi`, `frostjaw`, `anglerfish`, `lumen_eel` | Rare | Anglerfish: glowing lure. Lumen eel: glowing spots. Frostjaw: icy teeth |
| `sturgeon`, `arapaima`, `bog_lurker`, `bluefin_tuna`, `swordfish`, `coelacanth` | Epic | Bog lurker: glowing eyes |
| `ancient_koi`, `river_monarch`, `stormcaller_marlin`, `glacial_behemoth`, `abyssal_leviathan` | Legendary | These deserve the most love: unique silhouettes, maybe subtle animation (`.png.mcmeta`) |
| `cooked_fish` | Food | One cooked item for all new fish |

### Rods (6 plus 6 cast variants). Status: generated placeholder

`copper_fishing_rod`, `iron_fishing_rod`, `golden_fishing_rod`, `diamond_fishing_rod`, `netherite_fishing_rod`,
`heirloom_fishing_rod` (treasure-only, deserves a special look), each with a `_cast` texture used while the
bobber is out. The models use `minecraft:item/handheld_rod` like the vanilla rod.

### Tackle (10) and bait (8). Status: generated placeholder

- Hooks: `barbed_hook`, `heavy_hook`, `gilded_hook`, `lucky_hook`.
- Lines: `braided_line`, `reinforced_line`, `phantom_line`.
- Reels: `copper_reel`, `iron_reel`, `clockwork_reel`.
- Bait: `worm`, `cricket`, `cut_bait`, `dough_ball`, `glow_bait`, `golden_grub`.
- Reusable lures: `spinner_lure`, `shimmer_lure`.
- The Fishing Index item: `fishing_index` (a teal book with a gold fish; could become a 3D book model).

Attached tackle is not visible on the rod. Showing it (a reel on the rod, a colored line) would need
`minecraft:select` or `minecraft:has_component` item-model conditions on `examplemod:rod_loadout`, or a
small client-side item-model property. That is a nice stretch goal.

## 5. Sounds. Status: all placeholders

`assets/examplemod/sounds.json` points every event at a vanilla sound (`"type": "event"`). To use real
sounds, put `.ogg` files under `assets/examplemod/sounds/fishing/` and change the entry's `name` to
`examplemod:fishing/<file>` (and drop `"type": "event"`). Event ids are registered in `FishingSounds.java`.

| Event `examplemod:fishing.*` | Placeholder | What it should sound like |
|---|---|---|
| `bite` | bobber splash | A sharp tug and splash |
| `reveal_tick` | note block hat | A soft tick per color step (code raises the pitch each step) |
| `reveal.uncommon` / `.rare` / `.epic` / `.legendary` | chime / amethyst chime / amethyst resonate / challenge fanfare | Escalating rarity stings; legendary should feel huge |
| `hook_set` | bobber retrieve | A firm yank and line snap |
| `reel` | crossbow loading | A reel ratchet click (code shifts pitch up as the fish tires) |
| `reel_critical` | crit | A heavier, satisfying crank |
| `fish_pull` | splash | Line zinging out, thrash |
| `line_strain` | chain step | A creaking, stretched line (only when the fish outweighs the line) |
| `low_hp` | note block bell | "Almost there" cue |
| `catch` / `catch_rare` | player splash / level up | Landing a fish; a bigger version for Rare and above |
| `escape` | bubble pop | Line going slack, fish gone |
| `treasure` | XP pickup | Treasure chest sparkle |
| `new_species` / `new_record` | note block pling / bit | Discovery jingle / personal best jingle |
| `tackle_attach` / `tackle_detach` | bundle insert / remove | A small metal clip |
| `index_open` / `index_page` / `index_select` | book put / page turn / button click | Book sounds; select could be a quill scratch |

## 6. Particles. Status: vanilla stand-ins

Client side, in `ClientFishing.java`:
- Reel: splash.
- Critical reel: crit.
- Pull-back: bubbles.
- Catch: splash, happy villager, glow, end rod or totem, depending on rarity.
- Escape: smoke.
- Reveal bursts: end rod for Epic, totem for Legendary.

Server side, in `FishingController.java` and `CatchRewards.java`: splashes on hook set, pull and catch.
Custom particles (water droplets, rarity sparkles) would need a `ParticleType` and a client provider.

## 7. Other placeholders

- Advancement tab background: uses vanilla `minecraft:gui/advancements/backgrounds/husbandry`
  (`data/examplemod/advancement/fishing/root.json`).
- Creative tab icon: the Fishing Index item.
- Translations: only `en_us.json`. Text was written to fit the book; check other languages for overflow.

## 8. Needs an in-game check

The code compiles and a server smoke test runs in CI, but none of this has been seen in game.

**Visuals**
- [ ] Bite indicator: the height above the bobber, its size at 5–30 blocks away, how it looks over and under the water surface, the halo/glyph/rays layering, readability at night, and whether other players see it on their bobbers.
- [ ] Fish silhouettes in the Index: does the `has_component` condition plus constant tint render dark? This includes the vanilla cod, salmon, tropical fish and pufferfish overrides.
- [ ] HUD: overlap with other HUD elements and the boss bar, readability at GUI scales 1–4, and the "Press [key]" text with rebound keys.
- [ ] Index screen:
  - Tab placement on the smallest window (GUI width 320).
  - Text overflow: long fish names, long habitat lists, three-line hints.
  - Double-size icon crispness.
  - Page arrows and the page indicator.
- [ ] Rod cast textures swap while fishing (`minecraft:fishing_rod/cast` works with any `FishingRodItem`).

**Mechanics and balance** (all numbers in `FishingBalance.java`)
- [ ] Fight feel: HP, damage and timers were tuned on paper for roughly 3–6 clicks per second.
  - Holding right-click auto-repeats at about 5 per second in vanilla. Decide whether that is fine or whether reels should need fresh clicks.
  - Legendary fish with the best gear should be hard but beatable.
- [ ] Bite flow: rarity reveal timing, the length of the bite window, and what happens when the player switches items mid-fight. The line should "go slack" and the session should reset.
- [ ] Bait gating: rare fish need the right bait. Check that it feels like discovery, not like a wall. Hints in the Index say which bait each fish likes once it is caught.
- [ ] Time and weather species: the night, dusk, rain and thunder conditions (vanilla `time_check` on the overworld clock, and `weather_check`).
- [ ] Treasure rate: base chance 0.5% (vanilla rod) up to about 4% (heirloom rod) plus Luck, capped at 25%.
- [ ] The Fishing Index trade only appears for fishermen that get their level-1 trades after the mod is installed. Existing fishermen keep their old offers.
- [ ] Tackle handling in the creative inventory (Fabric's click callback runs in survival inventories; creative may behave differently).
- [ ] Multiplayer: the HUD only for the angler, the indicator for everyone, and the legendary broadcast message.

## 9. Changelog of this file

- Initial version: all placeholders listed.
