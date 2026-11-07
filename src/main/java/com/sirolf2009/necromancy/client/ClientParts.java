package com.sirolf2009.necromancy.client;

import com.sirolf2009.necromancy.registry.NecroEntities;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
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
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(NecroEntities.TEDDY.get(), NoopRenderer::new);
        event.registerEntityRenderer(NecroEntities.NIGHT_CRAWLER.get(), NoopRenderer::new);
        event.registerEntityRenderer(NecroEntities.ISAAC.get(), NoopRenderer::new);
        event.registerEntityRenderer(NecroEntities.BLOOD_ISAAC.get(), NoopRenderer::new);
        event.registerEntityRenderer(NecroEntities.ISAAC_HEAD.get(), NoopRenderer::new);
        event.registerEntityRenderer(NecroEntities.ISAAC_BODY.get(), NoopRenderer::new);
    }

    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
    }

    public static void registerExtensions(RegisterClientExtensionsEvent event) {
    }
}
