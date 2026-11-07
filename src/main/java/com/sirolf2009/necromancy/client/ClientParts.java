package com.sirolf2009.necromancy.client;

import com.sirolf2009.necromancy.client.model.NecroModelLayers;
import com.sirolf2009.necromancy.client.model.NecroPartModels;
import com.sirolf2009.necromancy.client.model.entity.IsaacHeadModel;
import com.sirolf2009.necromancy.client.model.entity.NightCrawlerModel;
import com.sirolf2009.necromancy.client.model.entity.TeddyModel;
import com.sirolf2009.necromancy.client.render.AltarRenderer;
import com.sirolf2009.necromancy.client.render.IsaacRenderer;
import com.sirolf2009.necromancy.client.render.NecroItemRenderer;
import com.sirolf2009.necromancy.client.render.SewingMachineRenderer;
import com.sirolf2009.necromancy.client.render.TexturedMobRenderer;
import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.registry.NecroBlockEntities;
import com.sirolf2009.necromancy.registry.NecroEntities;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Renderers, layers and client extensions for everything that is not the Minion itself: the Teddy, Night Crawler and
 * Isaac family, the altar and Sewing Machine block renderers, and item renderers. Also registers the Minion parts that
 * are not plain vanilla models (Isaac, the Night Crawler).
 */
public final class ClientParts {
    private ClientParts() {
    }

    public static void registerAll() {
        NecroPartModels.registerAll();
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(NecroEntities.TEDDY.get(), context -> new TexturedMobRenderer<>(context,
                new TeddyModel(context.bakeLayer(NecroModelLayers.TEDDY)), Necromancy.id("textures/entity/teddy.png"), 0.3F, 1F));
        event.registerEntityRenderer(NecroEntities.NIGHT_CRAWLER.get(), context -> new TexturedMobRenderer<>(context,
                new NightCrawlerModel(context.bakeLayer(NecroModelLayers.NIGHT_CRAWLER)), NecroPartModels.NIGHT_CRAWLER, 0.3F,
                NecroPartModels.NIGHT_CRAWLER_SCALE));
        event.registerEntityRenderer(NecroEntities.ISAAC.get(), context -> new IsaacRenderer<>(context, NecroModelLayers.ISAAC, NecroPartModels.ISAAC));
        event.registerEntityRenderer(NecroEntities.BLOOD_ISAAC.get(),
                context -> new IsaacRenderer<>(context, NecroModelLayers.ISAAC, NecroPartModels.BLOOD_ISAAC));
        event.registerEntityRenderer(NecroEntities.ISAAC_BODY.get(),
                context -> new IsaacRenderer<>(context, NecroModelLayers.ISAAC_BODY, NecroPartModels.BLOOD_ISAAC));
        event.registerEntityRenderer(NecroEntities.ISAAC_HEAD.get(), context -> new TexturedMobRenderer<>(context,
                new IsaacHeadModel(context.bakeLayer(NecroModelLayers.ISAAC_HEAD)), NecroPartModels.BLOOD_ISAAC, 0.3F, 1F));
        event.registerBlockEntityRenderer(NecroBlockEntities.ALTAR.get(), AltarRenderer::new);
        event.registerBlockEntityRenderer(NecroBlockEntities.SEWING_MACHINE.get(), SewingMachineRenderer::new);
    }

    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(NecroModelLayers.TEDDY, NecroModelLayers::teddy);
        event.registerLayerDefinition(NecroModelLayers.NIGHT_CRAWLER, NecroModelLayers::nightCrawler);
        event.registerLayerDefinition(NecroModelLayers.ISAAC, NecroModelLayers::isaac);
        event.registerLayerDefinition(NecroModelLayers.ISAAC_BODY, NecroModelLayers::isaacBody);
        // the severed head rests on the ground: its bottom (with the sinews under it) at y = 21
        event.registerLayerDefinition(NecroModelLayers.ISAAC_HEAD, () -> NecroModelLayers.isaacHead(21F));
        event.registerLayerDefinition(NecroModelLayers.ALTAR, NecroModelLayers::altar);
        event.registerLayerDefinition(NecroModelLayers.SEWING_MACHINE, NecroModelLayers::sewingMachine);
        event.registerLayerDefinition(NecroModelLayers.BLOOD_SCYTHE, NecroModelLayers::bloodScythe);
        event.registerLayerDefinition(NecroModelLayers.BONE_SCYTHE, NecroModelLayers::boneScythe);
    }

    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return NecroItemRenderer.get();
            }
        }, NecroItemRenderer.items().toArray(net.minecraft.world.item.Item[]::new));
    }
}
