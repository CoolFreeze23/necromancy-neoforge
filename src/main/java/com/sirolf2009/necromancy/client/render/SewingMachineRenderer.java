package com.sirolf2009.necromancy.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.block.SewingMachineBlock;
import com.sirolf2009.necromancy.blockentity.SewingMachineBlockEntity;
import com.sirolf2009.necromancy.client.model.NecroModelLayers;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/** Draws the Sewing Machine (TileEntitySewingRenderer, ModelSewing) facing whoever placed it, its needle bobbing. */
public class SewingMachineRenderer implements BlockEntityRenderer<SewingMachineBlockEntity> {
    public static final ResourceLocation TEXTURE = Necromancy.id("textures/block/sewing_machine.png");
    private final ModelPart model;
    private final ModelPart needle;

    public SewingMachineRenderer(BlockEntityRendererProvider.Context context) {
        this.model = context.bakeLayer(NecroModelLayers.SEWING_MACHINE);
        this.needle = model.getChild("needle");
    }

    @Override
    public void render(SewingMachineBlockEntity machine, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = machine.getBlockState();
        Direction facing = state.hasProperty(SewingMachineBlock.FACING) ? state.getValue(SewingMachineBlock.FACING) : Direction.SOUTH;
        float time = (machine.getLevel() != null ? machine.getLevel().getGameTime() : 0) + partialTick;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180F - facing.toYRot()));
        render(model, needle, time, poseStack, buffers, light, overlay);
        poseStack.popPose();
    }

    /** The model standing on y = 0 around the origin, its front towards +z. */
    public static void render(ModelPart model, ModelPart needle, float time, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        poseStack.pushPose();
        poseStack.scale(-1F, -1F, 1F);
        poseStack.translate(0F, -1.5F, 0F);
        needle.resetPose();
        // the needle stitches down to the bed and back up
        needle.y += 0.35F * (1F - Mth.cos(time * 0.3F));
        model.render(poseStack, buffers.getBuffer(RenderType.entityCutout(TEXTURE)), light, overlay);
        poseStack.popPose();
    }
}
