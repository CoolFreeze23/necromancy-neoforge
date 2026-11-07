package com.sirolf2009.necromancy.data;

import java.util.concurrent.CompletableFuture;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.registry.NecroLoot;

import net.minecraft.advancements.critereon.EntityFlagsPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;

/**
 * The organ drops modifier and its entry in neoforge's global_loot_modifiers.json. Organs only come from grown creatures
 * killed by a player or by a player's Minion (a tame Minion's kills count as its master's), docs/REVIEW.md #8.
 */
public class NecroLootModifiers extends GlobalLootModifierProvider {
    public NecroLootModifiers(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Necromancy.MOD_ID);
    }

    @Override
    protected void start() {
        add("organ_drops", new NecroLoot.OrganDrops(new LootItemCondition[] {
                LootItemKilledByPlayerCondition.killedByPlayer().build(),
                LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS,
                        EntityPredicate.Builder.entity().flags(EntityFlagsPredicate.Builder.flags().setIsBaby(false))).build()
        }));
    }
}
