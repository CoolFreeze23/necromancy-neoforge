package com.sirolf2009.necromancy.client.model;

import static com.sirolf2009.necromancy.client.model.VanillaPartModels.biped;
import static com.sirolf2009.necromancy.client.model.VanillaPartModels.bottomCentre;
import static com.sirolf2009.necromancy.client.model.VanillaPartModels.quadruped;
import static com.sirolf2009.necromancy.client.model.VanillaPartModels.vanilla;

import java.util.List;
import java.util.Map;

import com.mojang.math.Axis;
import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.api.PartType;

import net.minecraft.client.model.BlazeModel;
import net.minecraft.client.model.BoggedModel;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.DrownedModel;
import net.minecraft.client.model.GoatModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.SnowGolemModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Minion parts of the creatures added after 1.7 (REVIEW #4). Same conventions as {@link VanillaPartModels}. */
public final class ModernPartModels {
    private ModernPartModels() {
    }

    public static void registerAll() {
        biped("husk", vanilla("textures/entity/zombie/husk.png"), () -> LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0), 64, 64),
                true, PartAnimator.ZOMBIE_ARM);

        NecroMobModels.register(Necromancy.id("drowned"), b -> {
            ModelPart root = DrownedModel.createBodyLayer(CubeDeformation.NONE).bakeRoot();
            ModelPart outer = DrownedModel.createBodyLayer(new CubeDeformation(0.25F)).bakeRoot();
            biped(b, root, vanilla("textures/entity/zombie/drowned.png"), true, PartAnimator.ZOMBIE_ARM);
            b.layer(PartLayer.mesh(vanilla("textures/entity/zombie/drowned_outer_layer.png"), RenderType::entityCutoutNoCull, root, outer,
                    "head", "hat", "body", "left_arm", "right_arm", "left_leg", "right_leg"));
        });

        NecroMobModels.register(Necromancy.id("bogged"), b -> {
            ModelPart root = BoggedModel.createBodyLayer().bakeRoot();
            ModelPart outer = LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0.2F), 0F), 64, 32).bakeRoot();
            // the mushrooms are children of the head
            biped(b, root, vanilla("textures/entity/skeleton/bogged.png"), false, PartAnimator.ARM);
            b.layer(PartLayer.mesh(vanilla("textures/entity/skeleton/bogged_overlay.png"), RenderType::entityCutoutNoCull, root, outer,
                    "head", "body", "left_arm", "right_arm", "left_leg", "right_leg"));
        });

        // vanilla draws it 1.2 times bigger; the Minion grows by the parts' scale attributes instead
        biped("wither_skeleton", vanilla("textures/entity/skeleton/wither_skeleton.png"), SkeletonModel::createBodyLayer, false, PartAnimator.ARM);

        NecroMobModels.register(Necromancy.id("blaze"), b -> {
            ModelPart root = BlazeModel.createBodyLayer().bakeRoot();
            ResourceLocation tex = vanilla("textures/entity/blaze.png");
            ModelPart[] upper = new ModelPart[8];
            for (int i = 0; i < 8; i++) {
                upper[i] = root.getChild("part" + i);
            }
            // head: the face; torso: the two upper rings of rods turning around it; legs: the lowest ring, which hovers like
            // vanilla's (the blaze floats five pixels above the ground)
            b.put(PartType.HEAD, PartModel.of(tex, root.getChild("head")).plug(0, 4, 0).animate(PartAnimator.LOOK));
            b.put(PartType.TORSO, PartModel.of(tex, upper).plug(0, 10, 0).neck(0, 4, 0).shoulders(6, 1, 0, -6, 1, 0)
                    .animate(PartAnimator.blazeRods(new float[] { 4, 9, -2, 0.25F, -0.1F, 0F, 0F, 2F },
                            new float[] { 4, 7, 2, 0.25F, 0.03F, Mth.PI / 4F, 8F, 2F })));
            b.put(PartType.LEGS, PartModel.of(tex, root.getChild("part8"), root.getChild("part9"), root.getChild("part10"), root.getChild("part11"))
                    .hip(0, 10, 0).animate(PartAnimator.blazeRods(new float[] { 4, 5, 11, 0.5F, -0.05F, 0.47123894F, 12F, 1.5F })));
            b.fullBright();
        });

        NecroMobModels.register(Necromancy.id("slime"), b -> {
            ModelPart root = slime().bakeRoot();
            ResourceLocation tex = vanilla("textures/entity/slime/slime.png");
            PartLayer jelly = PartLayer.mesh(tex, RenderType::entityTranslucent, Map.of(root.getChild("head_core"), root.getChild("head_jelly"),
                    root.getChild("torso_core"), root.getChild("torso_jelly"), root.getChild("legs_core"), root.getChild("legs_jelly")));
            // a stack of three slimes, smallest on top: the head is a small slime with its face, the torso and the legs are
            // bigger ones (cubes 8, 10 and 12 wide); the jelly is see-through like vanilla's outer layer
            b.put(PartType.HEAD, PartModel.of(tex, root.getChild("head_core"), root.getChild("right_eye"), root.getChild("left_eye"),
                    root.getChild("mouth")).plug(0, 24, 0).animate(PartAnimator.LOOK).layer(jelly));
            b.put(PartType.TORSO, PartModel.of(tex, root.getChild("torso_core")).plug(0, 12, 0).neck(0, 2, 0).shoulders(6, 4, 0, -6, 4, 0)
                    .animate(PartAnimator.WOBBLE).layer(jelly));
            b.put(PartType.LEGS, PartModel.of(tex, root.getChild("legs_core")).hip(0, 12, 0).animate(PartAnimator.WOBBLE).layer(jelly));
        });

        NecroMobModels.register(Necromancy.id("mooshroom"), b -> {
            quadruped(b, CowModel.createBodyLayer().bakeRoot(), vanilla("textures/entity/cow/red_mooshroom.png"), 12F, 7F, -6F, 4F);
            // the red mushrooms of MushroomCowMushroomLayer: two on the back, one on the head
            b.get(PartType.TORSO).layer(PartLayer.block(Blocks.RED_MUSHROOM::defaultBlockState, (parts, poseStack) -> {
                poseStack.translate(0.2F, -0.35F, 0.5F);
                poseStack.mulPose(Axis.YP.rotationDegrees(-48F));
                poseStack.scale(-1F, -1F, 1F);
                poseStack.translate(-0.5F, -0.5F, -0.5F);
            })).layer(PartLayer.block(Blocks.RED_MUSHROOM::defaultBlockState, (parts, poseStack) -> {
                poseStack.translate(0.2F, -0.35F, 0.5F);
                poseStack.mulPose(Axis.YP.rotationDegrees(42F));
                poseStack.translate(0.1F, 0F, -0.6F);
                poseStack.mulPose(Axis.YP.rotationDegrees(-48F));
                poseStack.scale(-1F, -1F, 1F);
                poseStack.translate(-0.5F, -0.5F, -0.5F);
            }));
            b.get(PartType.HEAD).layer(PartLayer.block(Blocks.RED_MUSHROOM::defaultBlockState, (parts, poseStack) -> {
                parts.get(0).translateAndRotate(poseStack);
                poseStack.translate(0F, -0.7F, -0.2F);
                poseStack.mulPose(Axis.YP.rotationDegrees(-78F));
                poseStack.scale(-1F, -1F, 1F);
                poseStack.translate(-0.5F, -0.5F, -0.5F);
            }));
        });

        NecroMobModels.register(Necromancy.id("snow_golem"), b -> {
            ModelPart root = SnowGolemModel.createBodyLayer().bakeRoot();
            ResourceLocation tex = vanilla("textures/entity/snow_golem.png");
            // cubes shrunk by 0.5 on each side: head y -3.5..3.5, upper body 3.5..12.5, lower body 12.5..23.5
            b.put(PartType.HEAD, PartModel.of(tex, root.getChild("head")).plug(0, 3.5F, 0).animate(PartAnimator.LOOK)
                    .layer(PartLayer.item(() -> new ItemStack(Blocks.CARVED_PUMPKIN), (parts, poseStack) -> {
                        // SnowGolemHeadLayer
                        parts.get(0).translateAndRotate(poseStack);
                        poseStack.translate(0F, -0.34375F, 0F);
                        poseStack.mulPose(Axis.YP.rotationDegrees(180F));
                        poseStack.scale(0.625F, -0.625F, -0.625F);
                    })).iconBox(-5F, -6.5F, -5F, 5F, 3.5F, 5F));
            b.put(PartType.TORSO, PartModel.of(tex, root.getChild("upper_body")).plug(0, 12.5F, 0).neck(0, 3.5F, 0).shoulders(5, 6, 1, -5, 6, -1));
            b.put(PartType.ARM, PartModel.arms(tex, List.of(root.getChild("left_arm")), List.of(root.getChild("right_arm")))
                    .plugs(5, 6, 1, -5, 6, -1).animate(PartAnimator.ARM));
            b.put(PartType.LEGS, PartModel.of(tex, root.getChild("lower_body")).hip(0, 12.5F, 0));
        });

        NecroMobModels.register(Necromancy.id("armadillo"), b -> {
            ModelPart root = armadillo().bakeRoot();
            ResourceLocation tex = vanilla("textures/entity/armadillo.png");
            ModelPart head = root.getChild("head");
            float[] neck = bottomCentre(head);
            b.put(PartType.HEAD, PartModel.of(tex, head).plug(neck).animate(PartAnimator.LOOK));
            // shell y 14..22 over legs 21..24
            b.put(PartType.TORSO, PartModel.of(tex, root.getChild("body")).plug(0, 21, 0).neck(neck[0], neck[1], neck[2]).shoulders(5, 16, -3, -5, 16, -3));
            b.put(PartType.LEGS, PartModel.of(tex, root.getChild("right_hind_leg"), root.getChild("left_hind_leg"), root.getChild("left_front_leg"),
                    root.getChild("right_front_leg")).hip(0, 21, 0).animate(PartAnimator.WALK_PAIR));
        });

        NecroMobModels.register(Necromancy.id("goat"), b -> {
            ModelPart head = GoatModel.createBodyLayer().bakeRoot().getChild("head");
            // the bottom of the skull (the "nose" box, tilted down); the beard hangs below it in front of the neck
            b.put(PartType.HEAD, PartModel.of(vanilla("textures/entity/goat/goat.png"), head).plug(0.5F, 14.3F, -10F).animate(PartAnimator.LOOK));
        });
    }

    /**
     * Slime parts pivoted at their bottom-centres so they can turn and wobble. The bigger cubes use the small slime's
     * texture scaled up (texture scale 1.25 and 1.5) so the pixels stay square.
     */
    static LayerDefinition slime() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartPose head = PartPose.offset(0F, 24F, 0F);
        root.addOrReplaceChild("head_core", CubeListBuilder.create().texOffs(0, 16).addBox(-3F, -7F, -3F, 6F, 6F, 6F), head);
        root.addOrReplaceChild("right_eye", CubeListBuilder.create().texOffs(32, 0).addBox(-3.25F, -6F, -3.5F, 2F, 2F, 2F), head);
        root.addOrReplaceChild("left_eye", CubeListBuilder.create().texOffs(32, 4).addBox(1.25F, -6F, -3.5F, 2F, 2F, 2F), head);
        root.addOrReplaceChild("mouth", CubeListBuilder.create().texOffs(32, 8).addBox(0F, -3F, -3.5F, 1F, 1F, 1F), head);
        root.addOrReplaceChild("head_jelly", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F), head);
        PartPose waist = PartPose.offset(0F, 12F, 0F);
        root.addOrReplaceChild("torso_core", CubeListBuilder.create().texOffs(0, 20).addBox(-3.75F, -8.75F, -3.75F, 7.5F, 7.5F, 7.5F,
                CubeDeformation.NONE, 1.25F, 1.25F), waist);
        root.addOrReplaceChild("torso_jelly", CubeListBuilder.create().texOffs(0, 0).addBox(-5F, -10F, -5F, 10F, 10F, 10F, CubeDeformation.NONE, 1.25F, 1.25F),
                waist);
        PartPose ground = PartPose.offset(0F, 24F, 0F);
        root.addOrReplaceChild("legs_core", CubeListBuilder.create().texOffs(0, 24).addBox(-4.5F, -10.5F, -4.5F, 9F, 9F, 9F, CubeDeformation.NONE, 1.5F, 1.5F),
                ground);
        root.addOrReplaceChild("legs_jelly", CubeListBuilder.create().texOffs(0, 0).addBox(-6F, -12F, -6F, 12F, 12F, 12F, CubeDeformation.NONE, 1.5F, 1.5F),
                ground);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** ArmadilloModel with the head moved out of the shell (vanilla makes it a child of the body) and without the rolled-up ball. */
    static LayerDefinition armadillo() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 20).addBox(-4F, -7F, -10F, 8F, 8F, 12F, new CubeDeformation(0.3F))
                .texOffs(0, 40).addBox(-4F, -7F, -10F, 8F, 8F, 12F), PartPose.offset(0F, 21F, 4F));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(44, 53).addBox(-0.5F, -0.0865F, 0.0933F, 1F, 6F, 1F),
                PartPose.offsetAndRotation(0F, -3F, 1F, 0.5061F, 0F, 0F));
        // vanilla: body (0, 21, 4) + head (0, -2, -11)
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 19F, -7F));
        head.addOrReplaceChild("head_cube", CubeListBuilder.create().texOffs(43, 15).addBox(-1.5F, -1F, -1F, 3F, 5F, 2F),
                PartPose.offsetAndRotation(0F, 0F, 0F, -0.3927F, 0F, 0F));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create(), PartPose.offset(-1F, -1F, 0F));
        rightEar.addOrReplaceChild("right_ear_cube", CubeListBuilder.create().texOffs(43, 10).addBox(-2F, -3F, 0F, 2F, 5F, 0F),
                PartPose.offsetAndRotation(-0.5F, 0F, -0.6F, 0.1886F, -0.3864F, -0.0718F));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create(), PartPose.offset(1F, -2F, 0F));
        leftEar.addOrReplaceChild("left_ear_cube", CubeListBuilder.create().texOffs(47, 10).addBox(0F, -3F, 0F, 2F, 5F, 0F),
                PartPose.offsetAndRotation(0.5F, 1F, -0.6F, 0.1886F, 0.3864F, 0.0718F));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(51, 31).addBox(-1F, 0F, -1F, 2F, 3F, 2F), PartPose.offset(-2F, 21F, 4F));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(42, 31).addBox(-1F, 0F, -1F, 2F, 3F, 2F), PartPose.offset(2F, 21F, 4F));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(51, 43).addBox(-1F, 0F, -1F, 2F, 3F, 2F), PartPose.offset(-2F, 21F, -4F));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(42, 43).addBox(-1F, 0F, -1F, 2F, 3F, 2F), PartPose.offset(2F, 21F, -4F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
