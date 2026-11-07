package com.sirolf2009.necromancy.client.render;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.api.BodyPartLocation;
import com.sirolf2009.necromancy.client.model.PartAnimator;
import com.sirolf2009.necromancy.entity.MinionEntity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/** Renders a Minion from its parts: the main model is empty and {@link BodyLayer} draws every part with its own texture. */
public class MinionRenderer extends MobRenderer<MinionEntity, MinionRenderer.EmptyModel> {
    private static final ResourceLocation NONE = ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");

    public MinionRenderer(EntityRendererProvider.Context context) {
        super(context, new EmptyModel(), 0.5F);
        addLayer(new BodyLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(MinionEntity entity) {
        return NONE;
    }

    @Nullable
    public static ResourceLocation saddleTexture(ResourceLocation torsoMob) {
        return switch (torsoMob.getPath()) {
            case "pig" -> ResourceLocation.withDefaultNamespace("textures/entity/pig/pig_saddle.png");
            case "cow", "mooshroom" -> Necromancy.id("textures/entity/saddle/cow.png");
            case "spider", "cave_spider" -> Necromancy.id("textures/entity/saddle/spider.png");
            case "squid" -> Necromancy.id("textures/entity/saddle/squid.png");
            default -> null;
        };
    }

    /** A body read from a Minion entity. */
    public static MinionBodyRenderer.Body body(MinionEntity minion) {
        return new MinionBodyRenderer.Body() {
            @Override
            public ResourceLocation mob(BodyPartLocation location) {
                var mob = minion.part(location);
                return mob == null ? null : mob.id();
            }

            @Override
            public boolean saddled() {
                return minion.isSaddled();
            }

            @Override
            public ResourceLocation saddleTexture(ResourceLocation torsoMob) {
                return MinionRenderer.saddleTexture(torsoMob);
            }
        };
    }

    static class BodyLayer extends RenderLayer<MinionEntity, EmptyModel> {
        BodyLayer(RenderLayerParent<MinionEntity, EmptyModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int light, MinionEntity minion, float limbSwing, float limbSwingAmount,
                float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (minion.isInvisible()) {
                return;
            }
            float attack = minion.getAttackAnimation() > 0 ? minion.getAttackAnimation() - partialTick : 0;
            PartAnimator.Pose pose = new PartAnimator.Pose(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, attack, minion.isInSittingPose());
            int overlay = LivingEntityRenderer.getOverlayCoords(minion, 0F);
            MinionBodyRenderer.render(body(minion), pose, poseStack, buffers, light, overlay, -1);
            if (minion.isPowered()) {
                PoweredAura.render(minion, poseStack, buffers, light, ageInTicks, pose);
            }
        }
    }

    /** The charged-creeper swirl around a lightning-struck Minion with a Creeper torso. */
    static final class PoweredAura {
        private static final ResourceLocation ARMOR = ResourceLocation.withDefaultNamespace("textures/entity/creeper/creeper_armor.png");

        static void render(MinionEntity minion, PoseStack poseStack, MultiBufferSource buffers, int light, float ageInTicks, PartAnimator.Pose pose) {
            float t = ageInTicks * 0.01F;
            MultiBufferSource swirl = type -> buffers.getBuffer(net.minecraft.client.renderer.RenderType.energySwirl(ARMOR, t % 1F, t % 1F));
            poseStack.pushPose();
            poseStack.scale(1.05F, 1.02F, 1.05F);
            poseStack.translate(0, -0.015, 0);
            MinionBodyRenderer.render(body(minion), pose, poseStack, swirl, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0xFF808080);
            poseStack.popPose();
        }
    }

    public static class EmptyModel extends EntityModel<MinionEntity> {
        @Override
        public void setupAnim(MinionEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        }
    }
}
