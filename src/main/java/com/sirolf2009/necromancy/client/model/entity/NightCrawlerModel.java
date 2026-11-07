package com.sirolf2009.necromancy.client.model.entity;

import com.sirolf2009.necromancy.entity.NightCrawlerEntity;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * ModelNightCrawler, animated: the head turns and rears back when it strikes, the long arms drag and sway as it shuffles
 * and rise to claw, the hunched chest heaves, the roots under it creep.
 */
public class NightCrawlerModel extends HierarchicalModel<NightCrawlerEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart upperBody;
    private final ModelPart midBody;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart feet;

    public NightCrawlerModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.upperBody = root.getChild("upper_body");
        this.midBody = root.getChild("mid_body");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.feet = root.getChild("feet");
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(NightCrawlerEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        float breath = Mth.sin(ageInTicks * 0.08F);
        upperBody.xRot += breath * 0.04F;
        midBody.xRot += breath * 0.02F;
        head.y += breath * 0.2F;
        float swing = Mth.cos(limbSwing * 0.6662F) * 0.8F * limbSwingAmount;
        leftArm.xRot = swing + Mth.sin(ageInTicks * 0.067F) * 0.05F;
        rightArm.xRot = -swing - Mth.sin(ageInTicks * 0.067F) * 0.05F;
        leftArm.zRot = -(Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F);
        rightArm.zRot = Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        if (attackTime > 0F) {
            float strike = Mth.sin(attackTime * Mth.PI);
            leftArm.xRot = -2.2F * strike;
            rightArm.xRot = -2.2F * strike;
            head.xRot -= 0.4F * strike;
        }
        feet.yRot = Mth.sin(limbSwing * 0.6662F) * 0.15F * limbSwingAmount + Mth.sin(ageInTicks * 0.05F) * 0.05F;
    }
}
