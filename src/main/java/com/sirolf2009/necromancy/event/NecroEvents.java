package com.sirolf2009.necromancy.event;

import java.util.List;
import java.util.Optional;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.NecroConfig;
import com.sirolf2009.necromancy.api.NecroMob;
import com.sirolf2009.necromancy.api.NecroMobRegistry;
import com.sirolf2009.necromancy.api.PartType;
import com.sirolf2009.necromancy.command.MinionCommand;
import com.sirolf2009.necromancy.item.BoneNeedleItem;
import com.sirolf2009.necromancy.item.ScytheItem;
import com.sirolf2009.necromancy.registry.NecroEntities;
import com.sirolf2009.necromancy.registry.NecroItems;
import com.sirolf2009.necromancy.registry.NecroTags;
import com.sirolf2009.necromancy.registry.NecroVillagers;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;

@EventBusSubscriber(modid = Necromancy.MOD_ID)
public final class NecroEvents {
    private NecroEvents() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        MinionCommand.register(event.getDispatcher());
    }

    /** One in N naturally spawning Zombies/Skeletons becomes a Night Crawler or Isaac (original SpecialSpawn handler). */
    @SubscribeEvent
    public static void replaceSpawns(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        if (event.getSpawnType() != MobSpawnType.NATURAL || !mob.getType().is(NecroTags.REPLACEABLE_UNDEAD) || event.isSpawnCancelled()) {
            return;
        }
        RandomSource random = mob.getRandom();
        int crawlers = NecroConfig.RARITY_NIGHT_CRAWLERS.get();
        int isaacs = NecroConfig.RARITY_ISAACS.get();
        EntityType<? extends Mob> replacement = null;
        if (crawlers > 0 && random.nextInt(crawlers) == 0) {
            replacement = NecroEntities.NIGHT_CRAWLER.get();
        } else if (isaacs > 0 && event.getY() < 40 && random.nextInt(isaacs) == 0) {
            replacement = NecroEntities.ISAAC.get();
        }
        if (replacement == null) {
            return;
        }
        ServerLevel level = event.getLevel().getLevel();
        Mob spawned = replacement.create(level);
        if (spawned == null) {
            return;
        }
        spawned.moveTo(event.getX(), event.getY(), event.getZ(), mob.getYRot(), 0F);
        spawned.finalizeSpawn(event.getLevel(), event.getDifficulty(), MobSpawnType.NATURAL, null);
        event.setSpawnCancelled(true);
        level.addFreshEntity(spawned);
    }

    /** A scythe kill bottles the victim's soul; a Minion's kill counts towards its rank (and Soul Harvest). */
    @SubscribeEvent
    public static void reapSouls(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        Entity killer = event.getSource().getEntity();
        if (killer instanceof Player player && event.getSource().getDirectEntity() == player) {
            ScytheItem.reapSoul(player, event.getEntity());
        }
        if (killer instanceof com.sirolf2009.necromancy.entity.MinionEntity minion) {
            minion.addKill(event.getEntity());
            if (minion.getOwner() instanceof Player owner && !ScytheItem.harvestForMinion(owner, event.getEntity())
                    && (minion.hasFlag(com.sirolf2009.necromancy.api.BodyPartLocation.ARM_LEFT, com.sirolf2009.necromancy.api.TraitFlag.REAPS)
                            || minion.hasFlag(com.sirolf2009.necromancy.api.BodyPartLocation.ARM_RIGHT, com.sirolf2009.necromancy.api.TraitFlag.REAPS))
                    && !(event.getEntity() instanceof Player) && owner.distanceToSqr(minion) < 256) {
                // a reaping arm bottles the soul as a scythe would
                ScytheItem.bottleSoul(owner, event.getEntity());
            }
        }
    }

    /** Takes away the cobwebs and other blocks Minion traits put down for a while. */
    @SubscribeEvent
    public static void placedBlocks(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level && level.getGameTime() % 20 == 0) {
            com.sirolf2009.necromancy.world.PlacedBlocks.tick(level);
        }
    }

    /** A cloud a Minion leaves when it dies (or any effect it gives out) never touches its master's side. */
    @SubscribeEvent
    public static void friendlyEffects(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable event) {
        Entity source = event.getEffectSource();
        if (source instanceof net.minecraft.world.entity.AreaEffectCloud cloud) {
            source = cloud.getOwner();
        }
        if (source instanceof com.sirolf2009.necromancy.entity.MinionEntity minion && minion != event.getEntity() && minion.isFriendly(event.getEntity())
                && event.getEffectInstance().getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
            event.setResult(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /** Wading in blood makes a living player hungry. */
    @SubscribeEvent
    public static void bloodBath(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide && player.tickCount % 20 == 0 && player.isInFluidType(com.sirolf2009.necromancy.registry.NecroFluids.BLOOD_TYPE.value())) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.HUNGER, 60, 0, false, false, true));
        }
    }

    /** The original drew blood on a left click with the Bone Needle; keep that working next to the right click. */
    @SubscribeEvent
    public static void needleAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        ItemStack held = player.getMainHandItem();
        if (held.is(NecroItems.BONE_NEEDLE.get()) && event.getTarget() instanceof LivingEntity living) {
            if (BoneNeedleItem.drawBlood(held, player, living, EquipmentSlot.MAINHAND)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void necromancerTrades(VillagerTradesEvent event) {
        if (event.getType() != NecroVillagers.NECROMANCER.get()) {
            return;
        }
        var trades = event.getTrades();
        trades.get(1).add(sell(NecroItems.BONE_NEEDLE.get(), 1, 1, 12, 2));
        trades.get(1).add(buy(Items.ROTTEN_FLESH, 24, 16, 2));
        trades.get(1).add((trader, random) -> randomPart(random, 1 + random.nextInt(2), 8, 2));
        trades.get(2).add((trader, random) -> new MerchantOffer(new ItemCost(Items.EMERALD, 6), Optional.of(new ItemCost(Items.BOOK)),
                new ItemStack(NecroItems.NECRONOMICON.get()), 4, 10, 0.05F));
        trades.get(2).add(sell(NecroItems.JAR_OF_BLOOD.get(), 2, 1, 12, 5));
        trades.get(2).add((trader, random) -> buyRandomPart(random, 10));
        trades.get(3).add(sell(NecroItems.SOUL_IN_A_JAR.get(), 5, 1, 8, 10));
        trades.get(3).add(buy(NecroItems.BRAINS.get(), 1, 12, 10));
        trades.get(3).add(buy(NecroItems.HEART.get(), 1, 12, 10));
        trades.get(4).add(sell(NecroItems.BRAIN_ON_A_STICK.get(), 6, 1, 4, 15));
        trades.get(4).add((trader, random) -> randomPart(random, 3, 6, 15));
        trades.get(5).add(sell(NecroItems.SOUL_HEART.get(), 16, 1, 3, 30));
        trades.get(5).add(sell(NecroItems.BONE_SCYTHE.get(), 24, 1, 2, 30));
    }

    @SubscribeEvent
    public static void wandererTrades(WandererTradesEvent event) {
        event.getRareTrades().add(sell(NecroItems.SOUL_IN_A_JAR.get(), 6, 1, 3, 1));
    }

    private static VillagerTrades.ItemListing sell(Item item, int emeralds, int count, int uses, int xp) {
        return (trader, random) -> new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), new ItemStack(item, count), uses, xp, 0.05F);
    }

    private static VillagerTrades.ItemListing buy(Item item, int count, int uses, int xp) {
        return (trader, random) -> new MerchantOffer(new ItemCost(item, count), new ItemStack(Items.EMERALD), uses, xp, 0.05F);
    }

    /** The Necromancer deals in the sewn parts of vanilla creatures (parts with an item of their own). */
    private static List<Item> sewnParts() {
        return NecroMobRegistry.all().stream()
                .flatMap(mob -> mob.parts().entrySet().stream().filter(e -> mob.isSewable(e.getKey()) && !mob.isCreaturePart(e.getKey()))
                        .map(e -> (Item) e.getValue().get()))
                .toList();
    }

    private static MerchantOffer randomPart(RandomSource random, int emeralds, int uses, int xp) {
        List<Item> parts = sewnParts();
        Item part = parts.get(random.nextInt(parts.size()));
        return new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), new ItemStack(part), uses, xp, 0.05F);
    }

    private static MerchantOffer buyRandomPart(RandomSource random, int xp) {
        List<Item> parts = sewnParts();
        Item part = parts.get(random.nextInt(parts.size()));
        return new MerchantOffer(new ItemCost(part), new ItemStack(Items.EMERALD, 1 + random.nextInt(2)), 8, xp, 0.05F);
    }

    /** Used by datagen and tests: every vanilla-sourced head that counts as a body part. */
    public static boolean isFoundPart(NecroMob mob, PartType type) {
        return mob.hasPart(type) && !mob.isSewable(type);
    }
}
