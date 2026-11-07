package com.sirolf2009.necromancy.client.render;

import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.api.BodyPartLocation;
import com.sirolf2009.necromancy.block.AltarBlock;
import com.sirolf2009.necromancy.blockentity.AltarBlockEntity;
import com.sirolf2009.necromancy.client.model.NecroModelLayers;
import com.sirolf2009.necromancy.client.model.PartAnimator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Draws the whole Summoning Altar (TileEntityAltarRenderer): the pedestal on the altar block with its torch and open book,
 * the stone table across the two side blocks, and, while body parts lie in the altar, the Minion they make lying on its
 * back on the table, head towards the pedestal, gently rising and sinking.
 */
public class AltarRenderer implements BlockEntityRenderer<AltarBlockEntity> {
    public static final ResourceLocation TEXTURE = Necromancy.id("textures/block/summoning_altar.png");
    private static final float PREVIEW_SCALE = 0.75F;
    private final ModelPart altar;
    private final BookModel book;

    public AltarRenderer(BlockEntityRendererProvider.Context context) {
        this.altar = context.bakeLayer(NecroModelLayers.ALTAR);
        this.book = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    @Override
    public void render(AltarBlockEntity altarEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = altarEntity.getBlockState();
        Direction facing = state.hasProperty(AltarBlock.FACING) ? state.getValue(AltarBlock.FACING) : Direction.EAST;
        float time = (altarEntity.getLevel() != null ? altarEntity.getLevel().getGameTime() : 0) + partialTick;

        poseStack.pushPose();
        // altar frame: origin at the bottom-centre of the altar block, +x along the table towards the side blocks, y up
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(270F - facing.toYRot()));
        renderTable(poseStack, buffers, light, overlay);
        renderPedestalProps(poseStack, buffers, light, overlay, time);
        Map<BodyPartLocation, ResourceLocation> parts = altarEntity.parts();
        if (!parts.isEmpty()) {
            renderPreview(parts, poseStack, buffers, light, time);
        }
        poseStack.popPose();
    }

    /** ModelAltar in the altar frame (the model's y points down, ground at y = 24). */
    public void renderTable(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        poseStack.pushPose();
        poseStack.translate(0F, 1.5F, 0F);
        poseStack.scale(1F, -1F, -1F);
        altar.render(poseStack, buffers.getBuffer(RenderType.entityCutout(TEXTURE)), light, overlay);
        poseStack.popPose();
    }

    /** The original's torch and open book on top of the pedestal. */
    private void renderPedestalProps(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, float time) {
        float top = 1.375F;
        poseStack.pushPose();
        poseStack.translate(0.3F, top, 0.3F);
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(-0.5F, 0F, -0.5F);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.TORCH.defaultBlockState(), poseStack, buffers, light, overlay);
        poseStack.popPose();

        // open on the slanted rest like a lectern's, read from the far side of the table
        poseStack.pushPose();
        poseStack.translate(-0.05F, top + 0.12F, -0.12F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(75F));
        float flip = Mth.sin(time * 0.02F) * 0.05F;
        book.setupAnim(time, 0.1F + flip, 0.9F - flip, 1.2F);
        book.render(poseStack, EnchantTableRenderer.BOOK_LOCATION.buffer(buffers, RenderType::entitySolid), light, overlay, -1);
        poseStack.popPose();
    }

    /** The Minion the parts would make, lying face up along the table with its head at the pedestal end. */
    private void renderPreview(Map<BodyPartLocation, ResourceLocation> parts, PoseStack poseStack, MultiBufferSource buffers, int light, float time) {
        poseStack.pushPose();
        float bob = 0.1F + 0.05F * Mth.sin(time * 0.05F);
        // feet near the far end of the table, back just above its top (y = 1); three quarters size, so a biped (two
        // blocks tall) fits the table between the pedestal and the far end
        poseStack.translate(2.4F, 1.1F + bob, 0F);
        poseStack.scale(PREVIEW_SCALE, PREVIEW_SCALE, PREVIEW_SCALE);
        // entity up (+y) along -x, entity front (-z before the model flip) facing up
        poseStack.mulPose(Axis.XP.rotationDegrees(90F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90F));
        poseStack.scale(-1F, -1F, 1F);
        poseStack.translate(0F, -1.501F, 0F);
        PartAnimator.Pose pose = new PartAnimator.Pose(0F, 0F, time, 0F, 0F, 0F, false);
        MinionBodyRenderer.render(MinionBodyRenderer.of(parts), pose, poseStack, buffers, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                -1);
        poseStack.popPose();
    }

    /** The table reaches two blocks along the row and the Minion lies above it. */
    @Override
    public AABB getRenderBoundingBox(AltarBlockEntity altarEntity) {
        BlockPos pos = altarEntity.getBlockPos();
        BlockState state = altarEntity.getBlockState();
        Direction facing = state.hasProperty(AltarBlock.FACING) ? state.getValue(AltarBlock.FACING) : Direction.EAST;
        return new AABB(pos).minmax(new AABB(pos.relative(facing, 2))).expandTowards(0, 2, 0).inflate(0.5);
    }
}
