package com.sirolf2009.necromancy.client.model.entity;

import com.sirolf2009.necromancy.entity.TeddyEntity;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** ModelTeddy: front paws held out like a hug, raised to swat; hind paws toddle, and stretch out in front when sitting. */
public class TeddyModel extends HierarchicalModel<TeddyEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart belly;
    private final ModelPart pawFrontRight;
    private final ModelPart pawFrontLeft;
    private final ModelPart pawBackRight;
    private final ModelPart pawBackLeft;

    public TeddyModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.belly = root.getChild("belly");
        this.pawFrontRight = root.getChild("paw_front_right");
        this.pawFrontLeft = root.getChild("paw_front_left");
        this.pawBackRight = root.getChild("paw_back_right");
        this.pawBackLeft = root.getChild("paw_back_left");
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(TeddyEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.5F;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.5F;
        // the original's arm swing (its onGround was the swing progress)
        float swing = Mth.sin(attackTime * Mth.PI);
        float lift = Mth.sin((1.0F - (1.0F - attackTime) * (1.0F - attackTime)) * Mth.PI);
        pawFrontRight.yRot = -(0.1F - swing * 0.6F);
        pawFrontLeft.yRot = 0.1F - swing * 0.6F;
        pawFrontRight.xRot = -Mth.HALF_PI - (swing * 1.2F - lift * 0.4F);
        pawFrontLeft.xRot = -Mth.HALF_PI - (swing * 1.2F - lift * 0.4F);
        pawFrontRight.zRot = Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        pawFrontLeft.zRot = -(Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F);
        pawFrontRight.xRot += Mth.sin(ageInTicks * 0.067F) * 0.05F;
        pawFrontLeft.xRot -= Mth.sin(ageInTicks * 0.067F) * 0.05F;
        if (entity.isInSittingPose()) {
            // sat down: hind paws stretched out in front, everything else two pixels lower
            pawBackRight.xRot = -Mth.HALF_PI;
            pawBackLeft.xRot = -Mth.HALF_PI;
            pawBackRight.y += 1F;
            pawBackLeft.y += 1F;
            pawBackRight.z += 2F;
            pawBackLeft.z += 2F;
            for (ModelPart part : new ModelPart[] { head, belly, pawFrontRight, pawFrontLeft }) {
                part.y += 2F;
            }
        } else {
            pawBackRight.xRot = Mth.cos(limbSwing) * 0.5F * limbSwingAmount;
            pawBackLeft.xRot = Mth.cos(limbSwing + Mth.PI) * 0.5F * limbSwingAmount;
        }
    }
}
