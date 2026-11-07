package com.sirolf2009.necromancy.data;

import com.sirolf2009.necromancy.Necromancy;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Tags that only data files use so far (docs/REVIEW.md #8). The organ loot code can read the organ affinity tags from
 * here; they live in the data package so the registry classes stay untouched.
 */
public final class NecroDataTags {
    /** Brains come easily from these: villagers, witches, illagers, piglins, endermen. */
    public static final TagKey<EntityType<?>> ORGAN_RICH_BRAINS = entity("organ_rich/brains");
    /** Heart-heavy beasts: hoglins, ravagers, cows, mooshrooms, pigs. */
    public static final TagKey<EntityType<?>> ORGAN_RICH_HEART = entity("organ_rich/heart");
    /** Muscle-bound creatures: ravagers, hoglins, goats, camels, horses. */
    public static final TagKey<EntityType<?>> ORGAN_RICH_MUSCLE = entity("organ_rich/muscle");
    /** Creatures that live on their breath: blazes, breezes, phantoms. */
    public static final TagKey<EntityType<?>> ORGAN_RICH_LUNGS = entity("organ_rich/lungs");
    /** A ghast is mostly lung. */
    public static final TagKey<EntityType<?>> ORGAN_RICH_LUNGS_PLENTY = entity("organ_rich/lungs_plenty");

    /** Looting and Reaping: you choose vanilla drops or organs. */
    public static final TagKey<Enchantment> HARVEST_EXCLUSIVE = TagKey.create(Registries.ENCHANTMENT, Necromancy.id("exclusive_set/harvest"));

    private NecroDataTags() {
    }

    private static TagKey<EntityType<?>> entity(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, Necromancy.id(path));
    }
}
