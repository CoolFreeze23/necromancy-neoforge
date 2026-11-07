package com.sirolf2009.necromancy.client.model.entity;

import com.sirolf2009.necromancy.entity.isaac.IsaacHeadEntity;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** ModelIsaacHead: the severed head on the ground, turning to look at you and rocking as it rolls along. */
public class IsaacHeadModel extends HierarchicalModel<IsaacHeadEntity> {
    private final ModelPart root;
    private final ModelPart head;

    public IsaacHeadModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(IsaacHeadEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.resetPose();
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.5F;
        head.zRot = Mth.sin(limbSwing * 0.6662F) * 0.25F * limbSwingAmount;
    }
}
