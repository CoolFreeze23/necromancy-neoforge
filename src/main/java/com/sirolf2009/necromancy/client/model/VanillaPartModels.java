package com.sirolf2009.necromancy.client.model;

import java.util.List;
import java.util.function.Supplier;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.api.PartType;

import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.EndermanModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.IronGolemModel;
import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.PiglinModel;
import net.minecraft.client.model.SheepFurModel;
import net.minecraft.client.model.SheepModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.WitchModel;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * Minion parts for the vanilla creatures, cut from the vanilla model layer definitions (baked here, not taken from the
 * entity model set, so model-swapping resource packs cannot break them).
 *
 * <p>How to add a mob: bake its layer, pick the children for each part, and give the points where parts meet in that
 * model's own coordinates (y down, ground at 24): the head's plug is the bottom-centre of the head, a torso's plug is its
 * waist and its neck is where its own head's plug sits, an arm's plug is its pivot (the shoulder), the legs' hip is where
 * its own torso's waist sits. Same-mob bodies then line up exactly like the vanilla mob. Shoulders sit one unit inside
 * the outer edge of the arm's inner side (a biped's arm pivot), so any arm hangs flush against any torso.
 *
 * <p>The modern creatures are in {@link ModernPartModels}, the mod's own in {@link NecroPartModels}.
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
        // vanilla draws the cave spider at 0.7; its legs also shrink the whole Minion by 15 %, so 0.8 here ends near 0.7
        NecroMobModels.register(Necromancy.id("cave_spider"), b -> {
            spider(b, SpiderModel.createSpiderBodyLayer().bakeRoot(), vanilla("textures/entity/spider/cave_spider.png"));
            b.scale(0.8F);
        });

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

        NecroMobModels.register(Necromancy.id("enderman"), b -> {
            ModelPart root = EndermanModel.createBodyLayer().bakeRoot();
            ResourceLocation tex = vanilla("textures/entity/enderman/enderman.png");
            b.put(PartType.HEAD, PartModel.of(tex, root.getChild("head"), root.getChild("hat")).plug(0, -13, 0).animate(PartAnimator.LOOK)
                    .layer(PartLayer.eyes(vanilla("textures/entity/enderman/enderman_eyes.png"))));
            b.put(PartType.TORSO, PartModel.of(tex, root.getChild("body")).plug(0, -2, 0).neck(0, -13, 0).shoulders(5, -12, 0, -5, -12, 0));
            b.put(PartType.ARM, PartModel.arms(tex, List.of(root.getChild("left_arm")), List.of(root.getChild("right_arm")))
                    .plugs(5, -12, 0, -5, -12, 0).animate(PartAnimator.ARM));
            b.put(PartType.LEGS, PartModel.of(tex, root.getChild("right_leg"), root.getChild("left_leg")).hip(0, -2, 0).animate(PartAnimator.WALK_PAIR));
        });

        NecroMobModels.register(Necromancy.id("iron_golem"), b -> {
            ModelPart root = IronGolemModel.createBodyLayer().bakeRoot();
            ModelPart arms = ironGolemArms().bakeRoot();
            ResourceLocation tex = vanilla("textures/entity/iron_golem/iron_golem.png");
            // head box: x -4..4, y -19..-9, z -7.5..0.5 (with the nose in front)
            b.put(PartType.HEAD, PartModel.of(tex, root.getChild("head")).plug(0, -9, -3.5F).animate(PartAnimator.LOOK));
            // chest y -9..3 then the waist down to 8.5; the legs start at 8
            b.put(PartType.TORSO, PartModel.of(tex, root.getChild("body")).plug(0, 8, 0).neck(0, -9, -3.5F).shoulders(10, -7, 0, -10, -7, 0));
            b.put(PartType.ARM, PartModel.arms(tex, List.of(arms.getChild("left_arm")), List.of(arms.getChild("right_arm")))
                    .plugs(10, -7, 0, -10, -7, 0).animate(PartAnimator.GOLEM_ARM));
            b.put(PartType.LEGS, PartModel.of(tex, root.getChild("right_leg"), root.getChild("left_leg")).hip(0, 8, 0).animate(PartAnimator.WALK_PAIR));
        });

        NecroMobModels.register(Necromancy.id("pig"), b -> quadruped(b, PigModel.createBodyLayer(CubeDeformation.NONE).bakeRoot(),
                vanilla("textures/entity/pig/pig.png"), 18F, 7F, -5F, 3F));

        NecroMobModels.register(Necromancy.id("sheep"), b -> {
            ModelPart root = SheepModel.createBodyLayer().bakeRoot();
            ModelPart fur = SheepFurModel.createFurLayer().bakeRoot();
            quadruped(b, root, vanilla("textures/entity/sheep/sheep.png"), 12F, 7F, -5F, 3F);
            b.layer(PartLayer.mesh(vanilla("textures/entity/sheep/sheep_fur.png"), RenderType::entityCutoutNoCull, root, fur,
                    "head", "body", "right_hind_leg", "left_hind_leg", "right_front_leg", "left_front_leg"));
        });

        NecroMobModels.register(Necromancy.id("zombified_piglin"), b -> {
            ModelPart root = LayerDefinition.create(PiglinModel.createMesh(CubeDeformation.NONE), 64, 64).bakeRoot();
            ResourceLocation tex = vanilla("textures/entity/piglin/zombified_piglin.png");
            // the ears are children of the head, so they turn with it
            b.put(PartType.HEAD, PartModel.of(tex, root.getChild("head")).plug(0, 0, 0).animate(PartAnimator.LOOK));
            b.put(PartType.TORSO, PartModel.of(tex, root.getChild("body"), root.getChild("jacket")).plug(0, 12, 0).neck(0, 0, 0)
                    .shoulders(5, 2, 0, -5, 2, 0));
            b.put(PartType.ARM, PartModel.arms(tex, List.of(root.getChild("left_arm"), root.getChild("left_sleeve")),
                    List.of(root.getChild("right_arm"), root.getChild("right_sleeve"))).plugs(5, 2, 0, -5, 2, 0).animate(PartAnimator.ZOMBIE_ARM));
            // legs first, then the trousers, so WALK_PAIR swings each leg with its own trouser leg
            b.put(PartType.LEGS, PartModel.of(tex, root.getChild("right_leg"), root.getChild("left_leg"), root.getChild("right_pants"),
                    root.getChild("left_pants")).hip(0, 12, 0).animate(PartAnimator.WALK_PAIR));
        });

        NecroMobModels.register(Necromancy.id("squid"), b -> {
            ModelPart root = squid().bakeRoot();
            ResourceLocation tex = vanilla("textures/entity/squid/squid.png");
            // the squid stands upright on its tentacles: the head is the top half of its body (the eyes), the torso the bottom
            // half (the mouth underneath), the tentacles are the legs
            b.put(PartType.HEAD, PartModel.of(tex, root.getChild("head")).plug(0, 4, 0).animate(PartAnimator.LOOK));
            b.put(PartType.TORSO, PartModel.of(tex, root.getChild("body")).plug(0, 12, 0).neck(0, 4, 0).shoulders(7, 6, 0, -7, 6, 0));
            ModelPart[] tentacles = new ModelPart[8];
            for (int i = 0; i < 8; i++) {
                tentacles[i] = root.getChild("tentacle" + i);
            }
            b.put(PartType.LEGS, PartModel.of(tex, tentacles).hip(0, 12, 0).animate(PartAnimator.TENTACLES));
        });

        NecroMobModels.register(Necromancy.id("villager"), b -> {
            ModelPart root = LayerDefinition.create(VillagerModel.createBodyModel(), 64, 64).bakeRoot();
            villagerLike(b, root, villagerArms(64).bakeRoot(), vanilla("textures/entity/villager/villager.png"));
            // the plains clothes over the bare villager, like vanilla's type layer
            b.layer(PartLayer.retexture(vanilla("textures/entity/villager/type/plains.png"), RenderType::entityCutoutNoCull));
        });

        NecroMobModels.register(Necromancy.id("witch"), b -> villagerLike(b, WitchModel.createBodyLayer().bakeRoot(), villagerArms(128).bakeRoot(),
                vanilla("textures/entity/witch.png")));

        NecroMobModels.register(Necromancy.id("wolf"), b -> {
            ModelPart root = LayerDefinition.create(WolfModel.createMeshDefinition(CubeDeformation.NONE), 64, 32).bakeRoot();
            ModelPart head = root.getChild("head");
            b.put(PartType.HEAD, PartModel.of(vanilla("textures/entity/wolf/wolf.png"), head).plug(bottomCentre(head)).animate(PartAnimator.LOOK));
        });

        ModernPartModels.registerAll();
    }

    // --- body plans ---

    /** A humanoid (zombie, skeleton, husk, drowned, ...) from a HumanoidModel-shaped layer. */
    public static void biped(String mob, ResourceLocation texture, Supplier<LayerDefinition> layer, boolean hatLayer, PartAnimator arms) {
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
        float[] neck = bottomCentre(head);
        b.put(PartType.HEAD, PartModel.of(texture, head).plug(neck).animate(PartAnimator.LOOK));
        b.put(PartType.TORSO, PartModel.of(texture, root.getChild("body")).plug(0, legTop, hindZ).neck(neck[0], neck[1], neck[2])
                .shoulders(legX, legTop, frontZ, -legX, legTop, frontZ));
        b.put(PartType.ARM, PartModel.arms(texture, List.of(root.getChild("left_front_leg")), List.of(root.getChild("right_front_leg")))
                .plugs(legX, legTop, frontZ, -legX, legTop, frontZ).animate(PartAnimator.FRONT_LEG));
        b.put(PartType.LEGS, PartModel.of(texture, root.getChild("right_hind_leg"), root.getChild("left_hind_leg")).hip(0, legTop, hindZ)
                .animate(PartAnimator.WALK_PAIR));
    }

    /** Spider body plan (also cave spider, drawn smaller). */
    public static void spider(NecroMobModels.Builder b, ModelPart root, ResourceLocation texture) {
        b.put(PartType.HEAD, PartModel.of(texture, root.getChild("head")).plug(0, 19, -7).animate(PartAnimator.LOOK)
                .layer(PartLayer.eyes(vanilla("textures/entity/spider_eyes.png"))));
        b.put(PartType.TORSO, PartModel.of(texture, root.getChild("body0"), root.getChild("body1")).plug(0, 15, 0).neck(0, 19, -7)
                .shoulders(4, 15, -1, -4, 15, -1));
        b.put(PartType.LEGS, PartModel.of(texture, root.getChild("right_hind_leg"), root.getChild("left_hind_leg"),
                root.getChild("right_middle_hind_leg"), root.getChild("left_middle_hind_leg"),
                root.getChild("right_middle_front_leg"), root.getChild("left_middle_front_leg"),
                root.getChild("right_front_leg"), root.getChild("left_front_leg")).hip(0, 15, 0).animate(PartAnimator.SCUTTLE));
    }

    /**
     * Villager and witch: VillagerModel heads (with hats and noses as children), robes over the legs, and the folded arms
     * split down the middle into a left and a right arm (both together look exactly like vanilla's).
     */
    static void villagerLike(NecroMobModels.Builder b, ModelPart root, ModelPart arms, ResourceLocation texture) {
        b.put(PartType.HEAD, PartModel.of(texture, root.getChild("head")).plug(0, 0, 0).animate(PartAnimator.LOOK));
        b.put(PartType.TORSO, PartModel.of(texture, root.getChild("body")).plug(0, 12, 0).neck(0, 0, 0).shoulders(5, 3, -1, -5, 3, -1));
        b.put(PartType.ARM, PartModel.arms(texture, List.of(arms.getChild("left_arm")), List.of(arms.getChild("right_arm")))
                .plugs(5, 3, -1, -5, 3, -1).animate(PartAnimator.CROSSED_ARMS));
        b.put(PartType.LEGS, PartModel.of(texture, root.getChild("right_leg"), root.getChild("left_leg")).hip(0, 12, 0).animate(PartAnimator.WALK_PAIR));
    }

    /** VillagerModel's "arms" cut in two: each half is one sleeve and half of the folded hands, posed like vanilla's. */
    static LayerDefinition villagerArms(int textureHeight) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartPose pose = PartPose.offsetAndRotation(0F, 3F, -1F, -0.75F, 0F, 0F);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(44, 22).mirror().addBox(4F, -2F, -2F, 4F, 8F, 4F).mirror(false)
                .texOffs(44, 38).addBox(0F, 2F, -2F, 4F, 4F, 4F), pose);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(44, 22).addBox(-8F, -2F, -2F, 4F, 8F, 4F)
                .texOffs(40, 38).addBox(-4F, 2F, -2F, 4F, 4F, 4F), pose);
        return LayerDefinition.create(mesh, 64, textureHeight);
    }

    /** IronGolemModel's arms pivoted at the shoulders (vanilla pivots both at the middle of the chest). */
    static LayerDefinition ironGolemArms() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(60, 21).addBox(-3F, -2.5F, -3F, 4F, 30F, 6F), PartPose.offset(-10F, -7F, 0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(60, 58).addBox(-1F, -2.5F, -3F, 4F, 30F, 6F), PartPose.offset(10F, -7F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    /**
     * SquidModel's body cut into a top half (head: the dome and the eyes) and a bottom half (torso: the mouth underneath,
     * capped with the dome's top so a smaller head does not show a hole), on eight shorter tentacles standing like legs.
     */
    static LayerDefinition squid() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation skin = new CubeDeformation(0.02F);
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-6F, -8F, -6F, 12F, 8F, 12F, skin), PartPose.offset(0F, 4F, 0F));
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 8).addBox(-6F, -8F, -6F, 12F, 8F, 12F, java.util.EnumSet.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST))
                .texOffs(0, 0).addBox(-6F, -8F, -6F, 12F, 0F, 12F, java.util.EnumSet.of(Direction.DOWN))
                .texOffs(0, 0).addBox(-6F, 0F, -6F, 12F, 0F, 12F, java.util.EnumSet.of(Direction.UP)), PartPose.offset(0F, 12F, 0F));
        CubeListBuilder tentacle = CubeListBuilder.create().texOffs(48, 0).addBox(-1F, 0F, -1F, 2F, 12F, 2F);
        for (int k = 0; k < 8; k++) {
            float angle = k * Mth.TWO_PI / 8F;
            root.addOrReplaceChild("tentacle" + k, tentacle, PartPose.offsetAndRotation(Mth.cos(angle) * 4.5F, 12F, Mth.sin(angle) * 4.5F, 0.12F,
                    -k * Mth.TWO_PI / 8F + Mth.HALF_PI, 0F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    // --- helpers ---

    /** The bottom-centre of a part's cubes as baked (x, y, z), where a head plugs into its neck. */
    public static float[] bottomCentre(ModelPart part) {
        part.resetPose();
        AABB box = PartModel.boundsOf(List.of(part));
        return new float[] { (float) (box.minX + box.maxX) / 2F, (float) box.maxY, (float) (box.minZ + box.maxZ) / 2F };
    }

    public static ResourceLocation vanilla(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }
}
