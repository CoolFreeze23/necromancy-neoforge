package com.sirolf2009.necromancy.registry;

import com.sirolf2009.necromancy.Necromancy;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** World generation: the Nether Chalice feature. The village cemetery is a jigsaw piece added in {@code VillagePieces}. */
public final class NecroWorldgen {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Necromancy.MOD_ID);

    private NecroWorldgen() {
    }

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
    }
}
