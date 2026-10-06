package com.example.examplemod.fishing.item;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** The Fishing Index: right-click to open the journal of species, records and treasure. */
public class FishingIndexItem extends Item {
	private static Runnable screenOpener = () -> { };

	public FishingIndexItem(Properties properties) {
		super(properties);
	}

	/** Called by the client entrypoint so this common class never references client code. */
	public static void setScreenOpener(Runnable opener) {
		screenOpener = opener;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			screenOpener.run();
		}

		player.awardStat(Stats.ITEM_USED.get(this));
		return InteractionResult.SUCCESS;
	}
}
