package com.sirolf2009.necromancy.entity;

import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/** What a Minion is told to do (docs/REVIEW.md #2). Given with the Bone Scepter or /minion. */
public enum MinionOrder implements StringRepresentable {
    /** Follow the master, teleporting when left far behind (the default). */
    FOLLOW,
    /** Stay put (the original sit). */
    STAY,
    /** Hold a post: stays within 12 blocks of it and attacks hostile creatures that come close. */
    GUARD,
    /** Walk a route of up to four points, fighting what it meets. */
    PATROL;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component displayName() {
        return Component.translatable("order.necromancy." + getSerializedName());
    }

    public MinionOrder next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static MinionOrder byName(String name) {
        for (MinionOrder order : values()) {
            if (order.getSerializedName().equals(name)) {
                return order;
            }
        }
        return FOLLOW;
    }
}
