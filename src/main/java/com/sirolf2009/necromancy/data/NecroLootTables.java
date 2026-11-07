package com.sirolf2009.necromancy.data;

import java.util.Set;

import com.sirolf2009.necromancy.registry.NecroBlocks;
import com.sirolf2009.necromancy.registry.NecroEntities;
import com.sirolf2009.necromancy.registry.NecroItems;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/**
 * Block and entity loot tables. Chest and archaeology tables (cemetery, nether chalice) belong to the world generation
 * data and are not written here.
 */
public final class NecroLootTables {
    private NecroLootTables() {
    }

    public static class Blocks extends BlockLootSubProvider {
        public Blocks(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override
        protected void generate() {
            dropSelf(NecroBlocks.SEWING_MACHINE.get());
            // A bound altar gives back the planks and cobblestone it was made from (AltarBlock / AltarSideBlock).
            add(NecroBlocks.SUMMONING_ALTAR.get(), noDrop());
            add(NecroBlocks.ALTAR_SIDE.get(), noDrop());
            // Blood has no loot table at all (noLootTable).
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return NecroBlocks.BLOCKS.getEntries().stream().map(Holder::value).map(b -> (Block) b).toList();
        }
    }

    public static class Entities extends EntityLootSubProvider {
        public Entities(HolderLookup.Provider registries) {
            super(FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override
        public void generate() {
            // Night Crawler: rotten flesh, and now and then a real ender pearl (docs/REVIEW.md #13).
            add(NecroEntities.NIGHT_CRAWLER.get(), LootTable.lootTable()
                    .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(Items.ROTTEN_FLESH)
                                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(0, 2)))
                                    .apply(EnchantedCountIncreaseFunction.lootingMultiplier(registries, UniformGenerator.between(0, 1)))))
                    .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(Items.ENDER_PEARL))
                            .when(LootItemKilledByPlayerCondition.killedByPlayer())
                            .when(LootItemRandomChanceCondition.randomChance(0.25F))));
            // Isaac's three stages pay out in bones, blood and organs.
            add(NecroEntities.ISAAC.get(), LootTable.lootTable()
                    .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(Items.BONE)
                                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2)))
                                    .apply(EnchantedCountIncreaseFunction.lootingMultiplier(registries, UniformGenerator.between(0, 1))))));
            add(NecroEntities.BLOOD_ISAAC.get(), LootTable.lootTable()
                    .withPool(single(NecroItems.JAR_OF_BLOOD.get(), 2)));
            add(NecroEntities.ISAAC_BODY.get(), LootTable.lootTable()
                    .withPool(single(NecroItems.HEART.get(), 1))
                    .withPool(single(NecroItems.LUNGS.get(), 1))
                    .withPool(single(NecroItems.SOUL_HEART.get(), 1).when(LootItemRandomChanceCondition.randomChance(0.05F))));
            add(NecroEntities.ISAAC_HEAD.get(), LootTable.lootTable()
                    .withPool(single(NecroItems.ISAACS_HEAD.get(), 1)));
            add(NecroEntities.TEDDY.get(), LootTable.lootTable()
                    .withPool(single(Items.LEATHER, 1)));
            // A Minion's body is the soul's to keep: the organ modifier skips it too (#necromancy:bloodless).
            add(NecroEntities.MINION.get(), LootTable.lootTable());
            // the Abomination drops its parts, its heart and organs in code
            add(NecroEntities.ABOMINATION.get(), LootTable.lootTable());
        }

        private static LootPool.Builder single(ItemLike item, int count) {
            LootItem.Builder<?> entry = LootItem.lootTableItem(item);
            if (count != 1) {
                entry.apply(SetItemCountFunction.setCount(ConstantValue.exactly(count)));
            }
            return LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(entry);
        }

        @Override
        protected java.util.stream.Stream<EntityType<?>> getKnownEntityTypes() {
            return NecroEntities.ENTITIES.getEntries().stream().map(Holder::value);
        }
    }
}
