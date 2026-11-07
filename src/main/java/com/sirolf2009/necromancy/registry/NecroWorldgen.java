package com.sirolf2009.necromancy.registry;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.worldgen.NetherChaliceFeature;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * World generation: the Nether Chalice feature (placed by data/necromancy/neoforge/biome_modifier/nether_chalice.json).
 * The village cemetery is a jigsaw piece added to the village house pools in {@code worldgen.VillageCemetery}.
 */
public final class NecroWorldgen {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Necromancy.MOD_ID);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> NETHER_CHALICE = FEATURES.register("nether_chalice",
            NetherChaliceFeature::new);

    private NecroWorldgen() {
    }

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
    }
}
