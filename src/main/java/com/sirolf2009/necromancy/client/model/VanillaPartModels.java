package com.sirolf2009.necromancy.client.model;

import java.util.List;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.api.PartType;

import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Minion parts for the vanilla creatures, cut from the vanilla model layer definitions (baked here, not taken from the
 * entity model set, so model-swapping resource packs cannot break them).
 *
 * <p>How to add a mob: bake its layer, pick the children for each part, and give the points where parts meet in that
 * model's own coordinates (y down, ground at 24): the head's plug is the bottom-centre of the head, a torso's plug is its
 * waist and its neck is where its own head's plug sits, an arm's plug is its pivot (the shoulder), the legs' hip is where
 * its own torso's waist sits. Same-mob bodies then line up exactly like the vanilla mob.
 */
public final class VanillaPartModels {
    private VanillaPartModels() {
    }

    public static void registerAll() {
        biped("zombie", vanilla("textures/entity/zombie/zombie.png"), () -> LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0), 64, 64),
                true, PartAnimator.ZOMBIE_ARM);
        biped("skeleton", vanilla("textures/entity/skeleton/skeleton.png"), SkeletonModel::createBodyLayer, false, PartAnimator.ARM);

        NecroMobModels.register(Necromancy.id("cow"), b -> {
            ModelPart root = CowModel.createBodyLayer().bakeRoot();
            ResourceLocation tex = vanilla("textures/entity/cow/cow.png");
            quadruped(b, root, tex, 12F, 7F, -6F, 4F);
        });

        NecroMobModels.register(Necromancy.id("creeper"), b -> {
            ModelPart root = CreeperModel.createBodyLayer(CubeDeformation.NONE).bakeRoot();
            ResourceLocation tex = vanilla("textures/entity/creeper/creeper.png");
            b.put(PartType.HEAD, PartModel.of(tex, root.getChild("head")).plug(0, 6, 0).animate(PartAnimator.LOOK));
            b.put(PartType.TORSO, PartModel.of(tex, root.getChild("body")).plug(0, 18, 0).neck(0, 6, 0).shoulders(5, 8, 0, -5, 8, 0));
            b.put(PartType.LEGS, PartModel.of(tex, root.getChild("right_front_leg"), root.getChild("left_front_leg"),
                    root.getChild("left_hind_leg"), root.getChild("right_hind_leg")).hip(0, 18, 0).animate(PartAnimator.WALK_PAIR));
        });

        NecroMobModels.register(Necromancy.id("spider"), b -> spider(b, SpiderModel.createSpiderBodyLayer().bakeRoot(), vanilla("textures/entity/spider/spider.png")));

        NecroMobModels.register(Necromancy.id("chicken"), b -> {
            ModelPart root = ChickenModel.createBodyLayer().bakeRoot();
            ResourceLocation tex = vanilla("textures/entity/chicken.png");
            b.put(PartType.HEAD, PartModel.of(tex, root.getChild("head"), root.getChild("beak"), root.getChild("red_thing")).plug(0, 15, -4.5F)
                    .animate(PartAnimator.LOOK));
            b.put(PartType.TORSO, PartModel.of(tex, root.getChild("body")).plug(0, 19, 0).neck(0, 15, -4.5F).shoulders(4, 13, 0, -4, 13, 0));
            b.put(PartType.ARM, PartModel.arms(tex, List.of(root.getChild("left_wing")), List.of(root.getChild("right_wing")))
                    .plugs(4, 13, 0, -4, 13, 0).animate(PartAnimator.FRONT_LEG));
            b.put(PartType.LEGS, PartModel.of(tex, root.getChild("right_leg"), root.getChild("left_leg")).hip(0, 19, 0).animate(PartAnimator.WALK_PAIR));
        });
    }

    // --- body plans ---

    /** A humanoid (zombie, skeleton, husk, drowned, ...) from a HumanoidModel-shaped layer. */
    public static void biped(String mob, ResourceLocation texture, java.util.function.Supplier<LayerDefinition> layer, boolean hatLayer, PartAnimator arms) {
        NecroMobModels.register(Necromancy.id(mob), b -> {
            ModelPart root = layer.get().bakeRoot();
            biped(b, root, texture, hatLayer, arms);
        });
    }

    public static void biped(NecroMobModels.Builder b, ModelPart root, ResourceLocation texture, boolean hatLayer, PartAnimator arms) {
        ModelPart head = root.getChild("head");
        b.put(PartType.HEAD, (hatLayer && root.hasChild("hat") ? PartModel.of(texture, head, root.getChild("hat")) : PartModel.of(texture, head))
                .plug(0, 0, 0).animate(PartAnimator.LOOK));
        b.put(PartType.TORSO, PartModel.of(texture, root.getChild("body")).plug(0, 12, 0).neck(0, 0, 0).shoulders(5, 2, 0, -5, 2, 0));
        b.put(PartType.ARM, PartModel.arms(texture, List.of(root.getChild("left_arm")), List.of(root.getChild("right_arm")))
                .plugs(5, 2, 0, -5, 2, 0).animate(arms));
        b.put(PartType.LEGS, PartModel.of(texture, root.getChild("right_leg"), root.getChild("left_leg")).hip(0, 12, 0).animate(PartAnimator.WALK_PAIR));
    }

    /**
     * A four-legged animal from a QuadrupedModel-shaped layer: the hind legs are the legs, the front legs are the arms.
     *
     * @param legTop   y of the top of the legs
     * @param hindZ    z of the hind legs (the torso's waist sits above them)
     * @param frontZ   z of the front legs (the shoulders)
     * @param legX     x of the legs from the middle
     */
    public static void quadruped(NecroMobModels.Builder b, ModelPart root, ResourceLocation texture, float legTop, float hindZ, float frontZ, float legX) {
        ModelPart head = root.getChild("head");
        // head plug: bottom-centre of the head's first cube, in model space
        var cube = head.getRandomCube(net.minecraft.util.RandomSource.create(0));
        float headBottomY = head.y + cube.maxY;
        float headMidZ = head.z + (cube.minZ + cube.maxZ) / 2F;
        b.put(PartType.HEAD, PartModel.of(texture, head).plug(0, headBottomY, headMidZ).animate(PartAnimator.LOOK));
        b.put(PartType.TORSO, PartModel.of(texture, root.getChild("body")).plug(0, legTop, hindZ).neck(0, headBottomY, headMidZ)
                .shoulders(legX, legTop, frontZ, -legX, legTop, frontZ));
        b.put(PartType.ARM, PartModel.arms(texture, List.of(root.getChild("left_front_leg")), List.of(root.getChild("right_front_leg")))
                .plugs(legX, legTop, frontZ, -legX, legTop, frontZ).animate(PartAnimator.FRONT_LEG));
        b.put(PartType.LEGS, PartModel.of(texture, root.getChild("right_hind_leg"), root.getChild("left_hind_leg")).hip(0, legTop, hindZ)
                .animate(PartAnimator.WALK_PAIR));
    }

    /** Spider body plan (also cave spider, drawn smaller). */
    public static void spider(NecroMobModels.Builder b, ModelPart root, ResourceLocation texture) {
        b.put(PartType.HEAD, PartModel.of(texture, root.getChild("head")).plug(0, 19, -7).animate(PartAnimator.LOOK));
        b.put(PartType.TORSO, PartModel.of(texture, root.getChild("body0"), root.getChild("body1")).plug(0, 15, 0).neck(0, 19, -7)
                .shoulders(4, 15, -1, -4, 15, -1));
        b.put(PartType.LEGS, PartModel.of(texture, root.getChild("right_hind_leg"), root.getChild("left_hind_leg"),
                root.getChild("right_middle_hind_leg"), root.getChild("left_middle_hind_leg"),
                root.getChild("right_middle_front_leg"), root.getChild("left_middle_front_leg"),
                root.getChild("right_front_leg"), root.getChild("left_front_leg")).hip(0, 15, 0).animate(PartAnimator.SCUTTLE));
    }

    public static ResourceLocation vanilla(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }
}
