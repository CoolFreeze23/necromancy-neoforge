package com.sirolf2009.necromancy.worldgen;

import java.util.ArrayList;
import java.util.List;

import com.mojang.datafixers.util.Pair;
import com.sirolf2009.necromancy.Necromancy;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/**
 * The village cemetery (sirolf2009's ComponentVillageCemetery): a walled graveyard with graves to rob, a stone tomb with
 * two chests and the necromancer's house with a Sewing Machine. It joins the house pools of the five village types as an
 * uncommon jigsaw piece ({@code data/necromancy/structure/village/<type>/cemetery.nbt}, made by tools/gen_worldgen.py).
 * The piece is 13 x 10: village streets leave little room deeper than about 11 blocks, and the 1.7 size (18 x 19) or
 * anything near it never fitted in a desert or snowy village.
 */
@EventBusSubscriber(modid = Necromancy.MOD_ID)
public final class VillageCemetery {
    /**
     * Pool weight, in house pools that total 68 to 87. The JVM flag {@code -Dnecromancy.villageCemeteryWeight=N} overrides
     * it, and 0 leaves the cemetery out.
     */
    public static final int WEIGHT = Integer.getInteger("necromancy.villageCemeteryWeight", 2);

    private static final String[] VILLAGE_TYPES = {"plains", "taiga", "snowy", "savanna", "desert"};

    private VillageCemetery() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        if (WEIGHT <= 0) {
            return;
        }
        RegistryAccess access = event.getServer().registryAccess();
        Registry<StructureTemplatePool> pools = access.registryOrThrow(Registries.TEMPLATE_POOL);
        Registry<StructureProcessorList> processors = access.registryOrThrow(Registries.PROCESSOR_LIST);
        int added = 0;
        for (String type : VILLAGE_TYPES) {
            ResourceLocation poolId = ResourceLocation.withDefaultNamespace("village/" + type + "/houses");
            StructureTemplatePool pool = pools.get(poolId);
            Holder<StructureProcessorList> list = processors.getHolder(ResourceKey.create(Registries.PROCESSOR_LIST,
                    Necromancy.id("cemetery_" + type))).orElse(null);
            if (pool == null || list == null) {
                Necromancy.LOGGER.warn("Village cemetery: no {} or no processor list for it, skipped", poolId);
                continue;
            }
            if (addToPool(pool, StructurePoolElement.single(Necromancy.MOD_ID + ":village/" + type + "/cemetery", list)
                    .apply(StructureTemplatePool.Projection.RIGID), WEIGHT)) {
                added++;
            }
        }
        Necromancy.LOGGER.info("Village cemetery added to {} village house pools at weight {}", added, WEIGHT);
    }

    private static boolean addToPool(StructureTemplatePool pool, SinglePoolElement element, int weight) {
        String key = element.toString();
        for (StructurePoolElement existing : pool.templates) {
            if (existing.toString().equals(key)) {
                return false; // already there (a datapack or an earlier start of the same registries)
            }
        }
        for (int i = 0; i < weight; i++) {
            pool.templates.add(element);
        }
        List<Pair<StructurePoolElement, Integer>> raw = new ArrayList<>(pool.rawTemplates);
        raw.add(Pair.of(element, weight));
        pool.rawTemplates = raw;
        return true;
    }
}
