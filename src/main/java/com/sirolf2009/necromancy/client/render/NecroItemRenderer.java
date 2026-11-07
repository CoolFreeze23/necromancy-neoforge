package com.sirolf2009.necromancy.client.render;

import java.util.List;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.api.NecroMobRegistry;
import com.sirolf2009.necromancy.api.PartType;
import com.sirolf2009.necromancy.client.model.NecroMobModels;
import com.sirolf2009.necromancy.client.model.NecroModelLayers;
import com.sirolf2009.necromancy.client.model.PartModel;
import com.sirolf2009.necromancy.registry.NecroItems;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * 3D items (the original's IItemRenderers): the Sewing Machine block item, the two scythes, and the body parts of the
 * creatures that have no sprite, which show the part itself framed in the slot like a mob head.
 */
public class NecroItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final ResourceLocation BLOOD_SCYTHE_TEXTURE = Necromancy.id("textures/item/blood_scythe_model.png");
    public static final ResourceLocation BONE_SCYTHE_TEXTURE = Necromancy.id("textures/item/bone_scythe_model.png");
    /** The mobs whose part items are drawn here (the others have sprites). */
    public static final Set<String> MODEL_PART_MOBS = Set.of("husk", "drowned", "bogged", "wither_skeleton", "blaze", "slime", "mooshroom", "snow_golem",
            "armadillo", "goat", "night_crawler");

    private static NecroItemRenderer instance;

    private ModelPart sewingMachine;
    private ModelPart bloodScythe;
    private ModelPart boneScythe;

    private NecroItemRenderer(Minecraft minecraft) {
        super(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
    }

    public static NecroItemRenderer get() {
        if (instance == null) {
            instance = new NecroItemRenderer(Minecraft.getInstance());
        }
        return instance;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
    }

    private void bake() {
        if (sewingMachine == null) {
            sewingMachine = NecroModelLayers.sewingMachine().bakeRoot();
            bloodScythe = NecroModelLayers.bloodScythe().bakeRoot();
            boneScythe = NecroModelLayers.boneScythe().bakeRoot();
        }
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        bake();
        if (stack.is(NecroItems.SEWING_MACHINE.get())) {
            // like the placed block: standing on the bottom of the item cube, front towards the viewer's left in the GUI
            poseStack.pushPose();
            poseStack.translate(0.5F, 0F, 0.5F);
            SewingMachineRenderer.render(sewingMachine, sewingMachine.getChild("needle"), Util.getMillis() / 50F, poseStack, buffers, light, overlay);
            poseStack.popPose();
        } else if (stack.is(NecroItems.BLOOD_SCYTHE.get())) {
            renderScythe(bloodScythe, BLOOD_SCYTHE_TEXTURE, poseStack, buffers, light, overlay);
        } else if (stack.is(NecroItems.BONE_SCYTHE.get())) {
            renderScythe(boneScythe, BONE_SCYTHE_TEXTURE, poseStack, buffers, light, overlay);
        } else {
            NecroMobRegistry.partOf(stack).ifPresent(ref -> {
                PartModel part = NecroMobModels.get(ref.mob().id(), ref.type());
                if (part != null) {
                    renderPart(part, ref.type() == PartType.ARM, poseStack, buffers, light, overlay);
                }
            });
        }
    }

    /**
     * The scythe laid in the item's x/y plane like a sword sprite: the handle running diagonally up to the right, the blade
     * at the top hooking down to the right (so the handheld display transforms, which tip a sword forward, hold the blade
     * edge up; the GUI view turns it over), and the grip in the middle of the item cube. Two blocks long, like the original.
     */
    private static void renderScythe(ModelPart scythe, ResourceLocation texture, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-90F));
        poseStack.scale(1F, -1F, -1F);
        // the model's handle runs from y = -8 (blade) down to y = 24; its grip (y = 13) goes to the centre
        poseStack.translate(-0.5F / 16F, -13F / 16F, -1F / 16F);
        scythe.render(poseStack, buffers.getBuffer(RenderType.entityCutout(texture)), light, overlay);
        poseStack.popPose();
    }

    /** A Minion part in its baked pose, fitted into the item cube and centred, its front towards -z like a block's north. */
    public static void renderPart(PartModel part, boolean right, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        AABB box = part.bounds(right);
        double size = Math.max(box.getXsize(), Math.max(box.getYsize(), box.getZsize()));
        if (size <= 0) {
            return;
        }
        float fit = (float) (16D / size);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(fit, fit, fit);
        poseStack.scale(-1F, -1F, 1F);
        var centre = box.getCenter();
        poseStack.translate(-centre.x / 16D, -centre.y / 16D, -centre.z / 16D);
        MinionBodyRenderer.renderPart(part, right, poseStack, buffers, light, overlay, -1);
        poseStack.popPose();
    }

    /** Ids of the items this renderer draws, for the client extensions. */
    public static List<net.minecraft.world.item.Item> items() {
        List<net.minecraft.world.item.Item> items = new java.util.ArrayList<>();
        items.add(NecroItems.SEWING_MACHINE.get());
        items.add(NecroItems.BLOOD_SCYTHE.get());
        items.add(NecroItems.BONE_SCYTHE.get());
        NecroItems.BODY_PARTS.forEach((mob, parts) -> {
            if (MODEL_PART_MOBS.contains(mob)) {
                parts.values().forEach(item -> items.add(item.get()));
            }
        });
        return items;
    }
}
