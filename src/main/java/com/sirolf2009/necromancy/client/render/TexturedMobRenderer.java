package com.sirolf2009.necromancy.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** A mob drawn with one model and one texture, optionally scaled (the original's RenderTeddy, RenderNightCrawler, ...). */
public class TexturedMobRenderer<T extends Mob, M extends EntityModel<T>> extends MobRenderer<T, M> {
    private final ResourceLocation texture;
    private final float scale;

    public TexturedMobRenderer(EntityRendererProvider.Context context, M model, ResourceLocation texture, float shadow, float scale) {
        super(context, model, shadow * scale);
        this.texture = texture;
        this.scale = scale;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        if (scale != 1F) {
            poseStack.scale(scale, scale, scale);
        }
    }
}
