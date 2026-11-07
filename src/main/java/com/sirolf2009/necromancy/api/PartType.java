package com.sirolf2009.necromancy.api;

import net.minecraft.util.StringRepresentable;

/** The kind of body part item: one item per mob and type. */
public enum PartType implements StringRepresentable {
    HEAD("head"),
    TORSO("torso"),
    ARM("arm"),
    LEGS("legs");

    private final String name;

    PartType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
