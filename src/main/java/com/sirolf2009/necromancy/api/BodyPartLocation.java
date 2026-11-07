package com.sirolf2009.necromancy.api;

import net.minecraft.util.StringRepresentable;

/** Where a body part sits on a Minion. Both arm locations use the mob's {@link PartType#ARM} item. */
public enum BodyPartLocation implements StringRepresentable {
    HEAD("head", PartType.HEAD),
    TORSO("torso", PartType.TORSO),
    ARM_LEFT("arm_left", PartType.ARM),
    ARM_RIGHT("arm_right", PartType.ARM),
    LEGS("legs", PartType.LEGS);

    private final String name;
    private final PartType type;

    BodyPartLocation(String name, PartType type) {
        this.name = name;
        this.type = type;
    }

    public PartType type() {
        return type;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
