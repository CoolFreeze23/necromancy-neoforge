package com.sirolf2009.necromancy.registry;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.sirolf2009.necromancy.api.PartType;

/** Which sewn body part items exist, per mob (the original's ItemBodyPart.addParts list, with modern mob names). */
public final class BodyPartTable {
    public static final Map<String, Set<PartType>> SEWN = new LinkedHashMap<>();

    static {
        add("cow", PartType.HEAD, PartType.TORSO, PartType.ARM, PartType.LEGS);
        add("creeper", PartType.TORSO, PartType.LEGS);
        add("enderman", PartType.HEAD, PartType.TORSO, PartType.ARM, PartType.LEGS);
        add("pig", PartType.HEAD, PartType.TORSO, PartType.ARM, PartType.LEGS);
        add("zombified_piglin", PartType.HEAD, PartType.TORSO, PartType.ARM, PartType.LEGS);
        add("skeleton", PartType.TORSO, PartType.ARM, PartType.LEGS);
        add("spider", PartType.HEAD, PartType.TORSO, PartType.LEGS);
        add("zombie", PartType.TORSO, PartType.ARM, PartType.LEGS);
        add("chicken", PartType.HEAD, PartType.TORSO, PartType.ARM, PartType.LEGS);
        add("villager", PartType.HEAD, PartType.TORSO, PartType.ARM, PartType.LEGS);
        add("witch", PartType.HEAD, PartType.TORSO, PartType.ARM, PartType.LEGS);
        add("squid", PartType.HEAD, PartType.TORSO, PartType.LEGS);
        add("cave_spider", PartType.HEAD, PartType.TORSO, PartType.LEGS);
        add("sheep", PartType.HEAD, PartType.TORSO, PartType.ARM, PartType.LEGS);
        add("iron_golem", PartType.HEAD, PartType.TORSO, PartType.ARM, PartType.LEGS);
        add("wolf", PartType.HEAD);
        // modern creatures (1.8 to 1.21)
        add("husk", PartType.HEAD, PartType.TORSO, PartType.ARM, PartType.LEGS);
        add("drowned", PartType.HEAD, PartType.TORSO, PartType.ARM, PartType.LEGS);
        add("bogged", PartType.HEAD, PartType.TORSO, PartType.ARM, PartType.LEGS);
        add("wither_skeleton", PartType.TORSO, PartType.ARM, PartType.LEGS);
        add("blaze", PartType.HEAD, PartType.TORSO, PartType.LEGS);
        add("slime", PartType.HEAD, PartType.TORSO, PartType.LEGS);
        add("mooshroom", PartType.HEAD, PartType.TORSO, PartType.ARM, PartType.LEGS);
        add("snow_golem", PartType.HEAD, PartType.TORSO, PartType.ARM, PartType.LEGS);
        add("armadillo", PartType.HEAD, PartType.TORSO, PartType.LEGS);
        add("goat", PartType.HEAD);
        // the mod's own creature
        add("night_crawler", PartType.HEAD, PartType.TORSO, PartType.ARM, PartType.LEGS);
    }

    private BodyPartTable() {
    }

    public static void add(String mob, PartType... types) {
        Set<PartType> set = EnumSet.noneOf(PartType.class);
        for (PartType type : types) {
            set.add(type);
        }
        SEWN.put(mob, set);
    }

    public static String itemName(String mob, PartType type) {
        return mob + "_" + type.getSerializedName();
    }
}
