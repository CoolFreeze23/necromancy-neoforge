package com.sirolf2009.necromancy.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.client.model.NecroMobModels;
import com.sirolf2009.necromancy.client.model.VanillaPartModels;
import com.sirolf2009.necromancy.client.render.MinionRenderer;
import com.sirolf2009.necromancy.client.screen.AltarScreen;
import com.sirolf2009.necromancy.client.screen.SewingScreen;
import com.sirolf2009.necromancy.registry.NecroEntities;
import com.sirolf2009.necromancy.registry.NecroFluids;
import com.sirolf2009.necromancy.registry.NecroMenus;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

@EventBusSubscriber(modid = Necromancy.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class NecroClient {
    public static final String KEY_CATEGORY = "key.categories.necromancy";
    /** F and G in the original; F is the off-hand key now, so tears moved to R and G. */
    public static final KeyMapping SHOOT_TEAR = new KeyMapping("key.necromancy.shoot_tear", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R, KEY_CATEGORY);
    public static final KeyMapping SHOOT_BLOOD_TEAR = new KeyMapping("key.necromancy.shoot_blood_tear", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G, KEY_CATEGORY);

    private NecroClient() {
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            VanillaPartModels.registerAll();
            com.sirolf2009.necromancy.item.NecronomiconItem.openGuide = NecroClientEvents::openGuide;
            ClientParts.registerAll();
        });
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(NecroEntities.MINION.get(), MinionRenderer::new);
        event.registerEntityRenderer(NecroEntities.ABOMINATION.get(), MinionRenderer::new);
        event.registerEntityRenderer(NecroEntities.TEAR.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(NecroEntities.BLOOD_TEAR.get(), ThrownItemRenderer::new);
        ClientParts.registerRenderers(event);
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        ClientParts.registerLayers(event);
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(NecroMenus.ALTAR.get(), AltarScreen::new);
        event.register(NecroMenus.SEWING.get(), SewingScreen::new);
        event.register(NecroMenus.MINION.get(), com.sirolf2009.necromancy.client.screen.MinionScreen::new);
    }

    /** Sewing recipes have no recipe book tab; say so, so the client does not warn about every one of them. */
    @SubscribeEvent
    public static void recipeBook(net.neoforged.neoforge.client.event.RegisterRecipeBookCategoriesEvent event) {
        event.registerRecipeCategoryFinder(com.sirolf2009.necromancy.registry.NecroRecipes.SEWING.get(),
                recipe -> net.minecraft.client.RecipeBookCategories.UNKNOWN);
    }

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent event) {
        event.register(SHOOT_TEAR);
        event.register(SHOOT_BLOOD_TEAR);
    }

    @SubscribeEvent
    public static void reload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> NecroMobModels.invalidate());
    }

    @SubscribeEvent
    public static void extensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final ResourceLocation STILL = ResourceLocation.withDefaultNamespace("block/water_still");
            private static final ResourceLocation FLOWING = ResourceLocation.withDefaultNamespace("block/water_flow");
            private static final ResourceLocation OVERLAY = ResourceLocation.withDefaultNamespace("block/water_overlay");

            @Override
            public ResourceLocation getStillTexture() {
                return STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOWING;
            }

            @Override
            public ResourceLocation getOverlayTexture() {
                return OVERLAY;
            }

            @Override
            public int getTintColor() {
                return 0xFFB00000;
            }

            @Override
            public org.joml.Vector3f modifyFogColor(net.minecraft.client.Camera camera, float partialTick, net.minecraft.client.multiplayer.ClientLevel level,
                    int renderDistance, float darkenWorldAmount, org.joml.Vector3f fluidFogColor) {
                return new org.joml.Vector3f(0.45F, 0.0F, 0.0F);
            }

            @Override
            public void modifyFogRender(net.minecraft.client.Camera camera, net.minecraft.client.renderer.FogRenderer.FogMode mode, float renderDistance,
                    float partialTick, float nearDistance, float farDistance, com.mojang.blaze3d.shaders.FogShape shape) {
                com.mojang.blaze3d.systems.RenderSystem.setShaderFogStart(0F);
                com.mojang.blaze3d.systems.RenderSystem.setShaderFogEnd(4F);
            }
        }, NecroFluids.BLOOD_TYPE.get());
        ClientParts.registerExtensions(event);
    }
}
