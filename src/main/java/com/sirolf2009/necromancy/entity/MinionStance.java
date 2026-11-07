package com.sirolf2009.necromancy.entity;

import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/** How eager a Minion is to fight (docs/REVIEW.md #2). */
public enum MinionStance implements StringRepresentable {
    /** Never attacks; runs when hurt. */
    PASSIVE,
    /** Fights what attacks its master or itself, and what its master attacks (the original behaviour). */
    DEFENSIVE,
    /** Also hunts hostile creatures within its follow range. */
    AGGRESSIVE;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component displayName() {
        return Component.translatable("stance.necromancy." + getSerializedName());
    }

    public MinionStance next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static MinionStance byName(String name) {
        for (MinionStance stance : values()) {
            if (stance.getSerializedName().equals(name)) {
                return stance;
            }
        }
        return DEFENSIVE;
    }
}
