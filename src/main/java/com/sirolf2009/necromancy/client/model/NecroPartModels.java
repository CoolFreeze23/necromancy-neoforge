package com.sirolf2009.necromancy.client.model;

import java.util.List;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.api.PartType;

import net.minecraft.client.model.geom.ModelPart;

/** Minion parts of the mod's own creatures: Isaac's head and the Night Crawler. */
public final class NecroPartModels {
    public static final net.minecraft.resources.ResourceLocation ISAAC = Necromancy.id("textures/entity/isaac/isaac.png");
    public static final net.minecraft.resources.ResourceLocation BLOOD_ISAAC = Necromancy.id("textures/entity/isaac/blood_isaac.png");
    public static final net.minecraft.resources.ResourceLocation NIGHT_CRAWLER = Necromancy.id("textures/entity/night_crawler.png");
    /** The Night Crawler is drawn 1.4 times its model's size (ModelNightCrawler.render). */
    public static final float NIGHT_CRAWLER_SCALE = 1.4F;

    private NecroPartModels() {
    }

    public static void registerAll() {
        // NecroEntityIsaac: the big head with the bloody texture and its neck sinews, which sink into the torso
        NecroMobModels.register(Necromancy.id("isaac"), b -> {
            ModelPart root = NecroModelLayers.isaacHead(4F).bakeRoot();
            b.put(PartType.HEAD, PartModel.of(BLOOD_ISAAC, root.getChild("head")).plug(0, 4, 0).animate(PartAnimator.LOOK));
        });

        // the Night Crawler split at its neck, waist and shoulders: head with jaws, hunched chest, long clawed arms, and the
        // stalk with roots it stands on as legs
        NecroMobModels.register(Necromancy.id("night_crawler"), b -> {
            ModelPart root = NecroModelLayers.nightCrawler().bakeRoot();
            b.put(PartType.HEAD, PartModel.of(NIGHT_CRAWLER, root.getChild("head"), root.getChild("neck")).plug(0, 12.3F, -4F)
                    .animate(PartAnimator.LOOK_FIRST));
            b.put(PartType.TORSO, PartModel.of(NIGHT_CRAWLER, root.getChild("upper_body"), root.getChild("mid_body")).plug(0, 19, -1)
                    .neck(0, 12.3F, -4F).shoulders(3.5F, 12, -2.5F, -3.5F, 12, -2.5F));
            b.put(PartType.ARM, PartModel.arms(NIGHT_CRAWLER, List.of(root.getChild("left_arm")), List.of(root.getChild("right_arm")))
                    .plugs(3.5F, 12, -2.5F, -3.5F, 12, -2.5F).animate(PartAnimator.ARM));
            b.put(PartType.LEGS, PartModel.of(NIGHT_CRAWLER, root.getChild("lower_body"), root.getChild("feet")).hip(0, 19, -1));
            b.scale(NIGHT_CRAWLER_SCALE);
        });
    }
}
