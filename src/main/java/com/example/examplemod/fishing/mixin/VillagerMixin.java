package com.example.examplemod.fishing.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.level.Level;

import com.example.examplemod.fishing.FishingFeature;

/**
 * Novice fishermen always offer the Fishing Index, on top of their usual random trades.
 * The offer itself is data: {@code data/examplemod/trade_set/fisherman/fishing_index.json}.
 */
@Mixin(Villager.class)
public abstract class VillagerMixin extends AbstractVillager {
	@Unique
	private static final ResourceKey<TradeSet> FISHING_INDEX_TRADES = ResourceKey.create(Registries.TRADE_SET,
			FishingFeature.id("fisherman/fishing_index"));

	protected VillagerMixin(EntityType<? extends AbstractVillager> type, Level level) {
		super(type, level);
	}

	@Inject(method = "updateTrades", at = @At("TAIL"))
	private void examplemod$addFishingIndexTrade(ServerLevel level, CallbackInfo ci) {
		var data = ((Villager) (Object) this).getVillagerData();
		if (data.level() == 1 && data.profession().is(VillagerProfession.FISHERMAN)) {
			this.addOffersFromTradeSet(level, this.getOffers(), FISHING_INDEX_TRADES);
		}
	}
}
