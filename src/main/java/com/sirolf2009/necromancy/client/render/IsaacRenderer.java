package com.sirolf2009.necromancy.client.render;

import com.sirolf2009.necromancy.entity.isaac.IsaacBodyEntity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Isaac, Blood Isaac and Isaac's Body: a biped with Isaac's big head (ModelIsaacNormal) or the headless body
 * (ModelIsaacSevered), with what they hold in their hands.
 */
public class IsaacRenderer<T extends IsaacBodyEntity> extends HumanoidMobRenderer<T, HumanoidModel<T>> {
    private final ResourceLocation texture;

    public IsaacRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer, ResourceLocation texture) {
        super(context, new HumanoidModel<>(context.bakeLayer(layer)), 0.3F);
        this.texture = texture;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
