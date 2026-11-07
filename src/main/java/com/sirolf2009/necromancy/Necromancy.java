package com.sirolf2009.necromancy;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.sirolf2009.necromancy.mobs.VanillaNecroMobs;
import com.sirolf2009.necromancy.registry.NecroAttachments;
import com.sirolf2009.necromancy.registry.NecroBlockEntities;
import com.sirolf2009.necromancy.registry.NecroBlocks;
import com.sirolf2009.necromancy.registry.NecroComponents;
import com.sirolf2009.necromancy.registry.NecroEntities;
import com.sirolf2009.necromancy.registry.NecroFluids;
import com.sirolf2009.necromancy.registry.NecroItems;
import com.sirolf2009.necromancy.registry.NecroLoot;
import com.sirolf2009.necromancy.registry.NecroMenus;
import com.sirolf2009.necromancy.registry.NecroRecipes;
import com.sirolf2009.necromancy.registry.NecroSounds;
import com.sirolf2009.necromancy.registry.NecroTabs;
import com.sirolf2009.necromancy.registry.NecroVillagers;
import com.sirolf2009.necromancy.registry.NecroWorldgen;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(Necromancy.MOD_ID)
public class Necromancy {
    public static final String MOD_ID = "necromancy";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Necromancy(IEventBus modBus, ModContainer container) {
        NecroSounds.SOUNDS.register(modBus);
        NecroFluids.TYPES.register(modBus);
        NecroFluids.FLUIDS.register(modBus);
        NecroBlocks.BLOCKS.register(modBus);
        NecroComponents.COMPONENTS.register(modBus);
        NecroItems.ITEMS.register(modBus);
        NecroItems.ARMOR_MATERIALS.register(modBus);
        NecroEntities.ENTITIES.register(modBus);
        NecroBlockEntities.BLOCK_ENTITIES.register(modBus);
        NecroMenus.MENUS.register(modBus);
        NecroRecipes.TYPES.register(modBus);
        NecroRecipes.SERIALIZERS.register(modBus);
        NecroTabs.TABS.register(modBus);
        NecroAttachments.ATTACHMENTS.register(modBus);
        NecroLoot.register(modBus);
        NecroVillagers.register(modBus);
        NecroWorldgen.register(modBus);

        com.sirolf2009.necromancy.item.CreaturePartItem.bind(NecroItems.CREATURE_PART);
        VanillaNecroMobs.register();

        container.registerConfig(ModConfig.Type.COMMON, NecroConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
