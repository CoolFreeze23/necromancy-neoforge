package com.sirolf2009.necromancy.client.render;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.api.BodyPartLocation;
import com.sirolf2009.necromancy.client.model.PartAnimator;
import com.sirolf2009.necromancy.entity.MinionEntity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
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

    private static net.minecraft.client.renderer.ItemInHandRenderer itemInHand;
    private static ModelPart helmet;

    public MinionRenderer(EntityRendererProvider.Context context) {
        super(context, new EmptyModel(), 0.5F);
        itemInHand = context.getItemInHandRenderer();
        helmet = context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER_OUTER_ARMOR).getChild("head");
        addLayer(new BodyLayer(this));
    }

    /** Held items at the end of each arm (right arm: main hand) and a worn helmet on the head. */
    static void decorate(MinionEntity minion, BodyPartLocation location, ModelPart part, PoseStack poseStack, MultiBufferSource buffers, int light) {
        if (location == BodyPartLocation.HEAD) {
            renderHelmet(minion, part, poseStack, buffers, light);
            return;
        }
        boolean left = location == BodyPartLocation.ARM_LEFT;
        net.minecraft.world.item.ItemStack stack = left ? minion.getOffhandItem() : minion.getMainHandItem();
        if (stack.isEmpty() || itemInHand == null) {
            return;
        }
        poseStack.pushPose();
        part.translateAndRotate(poseStack);
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
        poseStack.translate((left ? -1 : 1) / 16.0F, 0.125F, -0.625F);
        itemInHand.renderItem(minion, stack, left ? net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                : net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, left, poseStack, buffers, light);
        poseStack.popPose();
    }

    private static void renderHelmet(MinionEntity minion, ModelPart head, PoseStack poseStack, MultiBufferSource buffers, int light) {
        net.minecraft.world.item.ItemStack stack = minion.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        if (stack.isEmpty() || helmet == null) {
            return;
        }
        poseStack.pushPose();
        head.translateAndRotate(poseStack);
        if (stack.getItem() instanceof net.minecraft.world.item.ArmorItem armor) {
            var layers = armor.getMaterial().value().layers();
            helmet.resetPose();
            helmet.setPos(0, 0, 0);
            int color = stack.is(net.minecraft.tags.ItemTags.DYEABLE)
                    ? net.minecraft.util.FastColor.ARGB32.opaque(net.minecraft.world.item.component.DyedItemColor.getOrDefault(stack, -6265536)) : -1;
            for (var layer : layers) {
                var texture = layer.texture(false);
                int tint = layer.dyeable() ? color : -1;
                helmet.render(poseStack, buffers.getBuffer(net.minecraft.client.renderer.RenderType.armorCutoutNoCull(texture)), light,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, tint);
            }
            if (stack.hasFoil()) {
                helmet.render(poseStack, buffers.getBuffer(net.minecraft.client.renderer.RenderType.armorEntityGlint()), light,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            }
        } else if (itemInHand != null) {
            // pumpkins, mob heads and blocks sit on the head like vanilla's custom head layer
            poseStack.translate(0.0F, -0.25F, 0.0F);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            poseStack.scale(0.625F, -0.625F, -0.625F);
            itemInHand.renderItem(minion, stack, net.minecraft.world.item.ItemDisplayContext.HEAD, false, poseStack, buffers, light);
        }
        poseStack.popPose();
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
            public void decorate(BodyPartLocation location, ModelPart part, PoseStack poseStack, MultiBufferSource buffers, int light) {
                MinionRenderer.decorate(minion, location, part, poseStack, buffers, light);
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
            MinionBodyRenderer.render(MinionBodyRenderer.of(minion.parts()), pose, poseStack, swirl, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0xFF808080);
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
