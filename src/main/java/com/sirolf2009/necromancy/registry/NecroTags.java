package com.sirolf2009.necromancy.registry;

import com.sirolf2009.necromancy.Necromancy;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class NecroTags {
    /** Creatures with no blood or organs to take: skeletons, golems, slimes, spirits. */
    public static final TagKey<EntityType<?>> BLOODLESS = TagKey.create(Registries.ENTITY_TYPE, Necromancy.id("bloodless"));
    /** Natural spawns that Night Crawlers and Isaac may replace. */
    public static final TagKey<EntityType<?>> REPLACEABLE_UNDEAD = TagKey.create(Registries.ENTITY_TYPE, Necromancy.id("replaceable_undead"));

    /** Souls reaped from these are Legendary (bosses). */
    public static final TagKey<EntityType<?>> LEGENDARY_SOULS = TagKey.create(Registries.ENTITY_TYPE, Necromancy.id("legendary_souls"));

    /** Shrine décor around the altar (docs/REVIEW.md #6). */
    public static final TagKey<Block> FOCUS_CANDLES = TagKey.create(Registries.BLOCK, Necromancy.id("altar_focus/candles"));
    public static final TagKey<Block> FOCUS_SOUL_LIGHTS = TagKey.create(Registries.BLOCK, Necromancy.id("altar_focus/soul_lights"));
    public static final TagKey<Block> FOCUS_HEADS = TagKey.create(Registries.BLOCK, Necromancy.id("altar_focus/heads"));
    public static final TagKey<Block> FOCUS_DREAD = TagKey.create(Registries.BLOCK, Necromancy.id("altar_focus/dread"));
    public static final TagKey<Block> FOCUS_DRAGON = TagKey.create(Registries.BLOCK, Necromancy.id("altar_focus/dragon"));

    /** The middle block of an unbound altar: any planks. */
    public static final TagKey<Block> ALTAR_BASE = TagKey.create(Registries.BLOCK, Necromancy.id("altar_base"));
    /** The two side blocks of an unbound altar: cobblestone. */
    public static final TagKey<Block> ALTAR_SIDES = TagKey.create(Registries.BLOCK, Necromancy.id("altar_sides"));

    public static final TagKey<Item> SCYTHES = TagKey.create(Registries.ITEM, Necromancy.id("scythes"));
    public static final TagKey<Item> BODY_PARTS = TagKey.create(Registries.ITEM, Necromancy.id("body_parts"));
    public static final TagKey<Item> ORGANS = TagKey.create(Registries.ITEM, Necromancy.id("organs"));
    public static final TagKey<Item> SEWING_THREAD = TagKey.create(Registries.ITEM, Necromancy.id("sewing_thread"));

    private NecroTags() {
    }
}
