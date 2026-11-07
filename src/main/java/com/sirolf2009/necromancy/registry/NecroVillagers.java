package com.sirolf2009.necromancy.registry;

import com.google.common.collect.ImmutableSet;
import com.sirolf2009.necromancy.Necromancy;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Necromancer: a villager whose job site is the Sewing Machine (the original registered a "necro" villager id). */
public final class NecroVillagers {
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, Necromancy.MOD_ID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(Registries.VILLAGER_PROFESSION, Necromancy.MOD_ID);

    public static final DeferredHolder<PoiType, PoiType> SEWING_MACHINE_POI = POI_TYPES.register("sewing_machine",
            () -> new PoiType(ImmutableSet.copyOf(NecroBlocks.SEWING_MACHINE.get().getStateDefinition().getPossibleStates()), 1, 1));

    public static final DeferredHolder<VillagerProfession, VillagerProfession> NECROMANCER = PROFESSIONS.register("necromancer",
            () -> new VillagerProfession("necromancer",
                    holder -> holder.is(SEWING_MACHINE_POI.getKey()),
                    holder -> holder.is(SEWING_MACHINE_POI.getKey()),
                    ImmutableSet.of(), ImmutableSet.of(), net.minecraft.sounds.SoundEvents.VILLAGER_WORK_LEATHERWORKER));

    private NecroVillagers() {
    }

    public static void register(IEventBus bus) {
        POI_TYPES.register(bus);
        PROFESSIONS.register(bus);
    }
}
