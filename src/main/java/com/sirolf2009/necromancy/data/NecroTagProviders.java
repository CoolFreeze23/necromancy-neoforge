package com.sirolf2009.necromancy.data;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.registry.NecroBlocks;
import com.sirolf2009.necromancy.registry.NecroEntities;
import com.sirolf2009.necromancy.registry.NecroItems;
import com.sirolf2009.necromancy.registry.NecroTags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EnchantmentTagsProvider;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/** Block, item, entity type and enchantment tags. All keys are in {@link NecroTags} and {@link NecroDataTags}. */
public final class NecroTagProviders {
    private NecroTagProviders() {
    }

    public static class BlockTagGen extends BlockTagsProvider {
        public BlockTagGen(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper files) {
            super(output, lookup, Necromancy.MOD_ID, files);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            altar();
            shrineFocus();
            tag(BlockTags.MINEABLE_WITH_AXE).add(NecroBlocks.SUMMONING_ALTAR.get());
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(NecroBlocks.ALTAR_SIDE.get(), NecroBlocks.SEWING_MACHINE.get());
            // soft ground a digging part (OreSpawn's Molenoid) works through
            tag(NecroTags.DIGGABLE).add(Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT, Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.MYCELIUM,
                    Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL, Blocks.CLAY, Blocks.MUD, Blocks.SOUL_SAND, Blocks.SOUL_SOIL)
                    .addOptional(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("orespawn", "mole_dirt"));
        }

        /** What the Necronomicon binds: planks in the middle, two cobblestones in a row beside it. */
        private void altar() {
            tag(NecroTags.ALTAR_BASE).addTag(BlockTags.PLANKS);
            tag(NecroTags.ALTAR_SIDES).add(Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE,
                    Blocks.COBBLED_DEEPSLATE);
        }

        /** Shrine décor around the altar (docs/REVIEW.md #6). */
        private void shrineFocus() {
            tag(NecroTags.FOCUS_CANDLES).addTag(BlockTags.CANDLES);
            tag(NecroTags.FOCUS_SOUL_LIGHTS).add(Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH, Blocks.SOUL_LANTERN, Blocks.SOUL_CAMPFIRE);
            tag(NecroTags.FOCUS_HEADS).add(
                    Blocks.SKELETON_SKULL, Blocks.SKELETON_WALL_SKULL,
                    Blocks.ZOMBIE_HEAD, Blocks.ZOMBIE_WALL_HEAD,
                    Blocks.CREEPER_HEAD, Blocks.CREEPER_WALL_HEAD,
                    Blocks.PIGLIN_HEAD, Blocks.PIGLIN_WALL_HEAD,
                    Blocks.PLAYER_HEAD, Blocks.PLAYER_WALL_HEAD);
            tag(NecroTags.FOCUS_DREAD).add(Blocks.WITHER_SKELETON_SKULL, Blocks.WITHER_SKELETON_WALL_SKULL, Blocks.SCULK_CATALYST);
            tag(NecroTags.FOCUS_DRAGON).add(Blocks.DRAGON_HEAD, Blocks.DRAGON_WALL_HEAD);
        }

    }

    public static class ItemTagGen extends ItemTagsProvider {
        public ItemTagGen(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, CompletableFuture<TagLookup<Block>> blockTags,
                ExistingFileHelper files) {
            super(output, lookup, blockTags, Necromancy.MOD_ID, files);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(NecroTags.SCYTHES).add(NecroItems.BLOOD_SCYTHE.get(), NecroItems.BONE_SCYTHE.get());
            var parts = tag(NecroTags.BODY_PARTS);
            NecroItems.BODY_PARTS.values().forEach(byType -> byType.values().forEach(item -> parts.add(item.get())));
            tag(NecroTags.ORGANS).add(NecroItems.BRAINS.get(), NecroItems.HEART.get(), NecroItems.MUSCLE.get(), NecroItems.LUNGS.get(), NecroItems.SKIN.get());
            tag(NecroTags.SEWING_THREAD).add(Items.STRING);

            // scythes take sword enchantments (Looting, Sweeping Edge, Sharpness, Fire Aspect, Unbreaking, Mending)
            for (TagKey<Item> enchantable : List.of(ItemTags.SWORD_ENCHANTABLE, ItemTags.SHARP_WEAPON_ENCHANTABLE,
                    ItemTags.WEAPON_ENCHANTABLE, ItemTags.FIRE_ASPECT_ENCHANTABLE, ItemTags.DURABILITY_ENCHANTABLE)) {
                tag(enchantable).addTag(NecroTags.SCYTHES);
            }
            // The Necronomicon stays out of #minecraft:lectern_books: LecternBlockEntity.hasBook() only accepts written
            // and writable books, so a Necronomicon on a lectern could neither be read nor taken back.
        }
    }

    public static class EntityTagGen extends EntityTypeTagsProvider {
        public EntityTagGen(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper files) {
            super(output, lookup, Necromancy.MOD_ID, files);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(NecroTags.BLOODLESS).addTag(EntityTypeTags.SKELETONS).add(
                    EntityType.SKELETON, EntityType.STRAY, EntityType.WITHER_SKELETON, EntityType.BOGGED, EntityType.SKELETON_HORSE,
                    EntityType.IRON_GOLEM, EntityType.SNOW_GOLEM, EntityType.SLIME, EntityType.MAGMA_CUBE, EntityType.BLAZE, EntityType.BREEZE,
                    EntityType.VEX, EntityType.ALLAY, EntityType.SHULKER, EntityType.ARMOR_STAND,
                    NecroEntities.MINION.get(), NecroEntities.TEDDY.get());
            tag(NecroTags.REPLACEABLE_UNDEAD).add(EntityType.ZOMBIE, EntityType.SKELETON);
            tag(NecroTags.MINION_IGNORES).add(EntityType.CREEPER, EntityType.ENDERMAN, EntityType.ZOMBIFIED_PIGLIN, EntityType.PIGLIN);
            tag(NecroTags.LEGENDARY_SOULS).add(EntityType.WITHER, EntityType.ENDER_DRAGON, EntityType.WARDEN, EntityType.ELDER_GUARDIAN);

            // Minions are undead: Smite hurts them, Harming heals them, Regeneration and Poison do nothing.
            // Not in #minecraft:wither_friends, or minions could farm the Wither.
            for (TagKey<EntityType<?>> undead : List.of(EntityTypeTags.UNDEAD, EntityTypeTags.SENSITIVE_TO_SMITE,
                    EntityTypeTags.INVERTED_HEALING_AND_HARM, EntityTypeTags.IGNORES_POISON_AND_REGEN)) {
                tag(undead).add(NecroEntities.MINION.get());
            }

            organAffinity();
            tag(NecroTags.NO_SOUL);
            // OreSpawn's creatures, optional so the tags load without it
            com.sirolf2009.necromancy.compat.OreSpawnCompat.tags().forEach((path, ids) -> {
                var builder = tag(TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, Necromancy.id(path)));
                ids.forEach(builder::addOptional);
            });
        }

        /** Which creatures are rich in which organ (docs/REVIEW.md #8). */
        private void organAffinity() {
            tag(NecroDataTags.ORGAN_RICH_BRAINS).add(EntityType.VILLAGER, EntityType.WITCH, EntityType.EVOKER, EntityType.VINDICATOR,
                    EntityType.PILLAGER, EntityType.ILLUSIONER, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.ENDERMAN);
            tag(NecroDataTags.ORGAN_RICH_HEART).add(EntityType.HOGLIN, EntityType.RAVAGER, EntityType.COW, EntityType.MOOSHROOM, EntityType.PIG);
            tag(NecroDataTags.ORGAN_RICH_MUSCLE).add(EntityType.RAVAGER, EntityType.HOGLIN, EntityType.GOAT, EntityType.CAMEL, EntityType.HORSE);
            tag(NecroDataTags.ORGAN_RICH_LUNGS).add(EntityType.BLAZE, EntityType.BREEZE, EntityType.PHANTOM);
            tag(NecroDataTags.ORGAN_RICH_LUNGS_PLENTY).add(EntityType.GHAST);
        }
    }

    public static class EnchantmentTagGen extends EnchantmentTagsProvider {
        public EnchantmentTagGen(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper files) {
            super(output, lookup, Necromancy.MOD_ID, files);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(NecroDataTags.HARVEST_EXCLUSIVE).add(Enchantments.LOOTING, NecroEnchantments.REAPING);
            tag(EnchantmentTags.IN_ENCHANTING_TABLE).add(NecroEnchantments.REAPING);
            tag(EnchantmentTags.NON_TREASURE).add(NecroEnchantments.REAPING);
            tag(EnchantmentTags.TRADEABLE).add(NecroEnchantments.REAPING);
            tag(EnchantmentTags.ON_RANDOM_LOOT).add(NecroEnchantments.REAPING);
        }
    }
}
