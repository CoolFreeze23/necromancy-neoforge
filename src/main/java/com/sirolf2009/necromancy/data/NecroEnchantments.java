package com.sirolf2009.necromancy.data;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.registry.NecroTags;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;

/** The mod's data-driven enchantments. */
public final class NecroEnchantments {
    /**
     * Reaping I-III, for scythes: better organ drops. It has no effects of its own; the organ loot code reads its level.
     * Exclusive with Looting (docs/REVIEW.md #8).
     */
    public static final ResourceKey<Enchantment> REAPING = ResourceKey.create(Registries.ENCHANTMENT, Necromancy.id("reaping"));

    private NecroEnchantments() {
    }

    public static void bootstrap(BootstrapContext<Enchantment> context) {
        var items = context.lookup(Registries.ITEM);
        var enchantments = context.lookup(Registries.ENCHANTMENT);
        context.register(REAPING, Enchantment.enchantment(Enchantment.definition(
                        items.getOrThrow(NecroTags.SCYTHES),
                        2,
                        3,
                        Enchantment.dynamicCost(15, 9),
                        Enchantment.dynamicCost(65, 9),
                        4,
                        EquipmentSlotGroup.MAINHAND))
                .exclusiveWith(enchantments.getOrThrow(NecroDataTags.HARVEST_EXCLUSIVE))
                .build(REAPING.location()));
    }
}
