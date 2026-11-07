package com.sirolf2009.necromancy.registry;

import java.util.function.Supplier;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.soul.SoulData;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NecroComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Necromancy.MOD_ID);

    public static final Supplier<DataComponentType<SoulData>> SOUL = COMPONENTS.registerComponentType("soul",
            b -> b.persistent(SoulData.CODEC).networkSynchronized(SoulData.STREAM_CODEC));

    private NecroComponents() {
    }
}
