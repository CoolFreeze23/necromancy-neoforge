package com.sirolf2009.necromancy;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Common config (config/necromancy-common.toml). */
public final class NecroConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue RARITY_NIGHT_CRAWLERS;
    public static final ModConfigSpec.IntValue RARITY_ISAACS;
    public static final ModConfigSpec.IntValue MAX_MINIONS;
    public static final ModConfigSpec.DoubleValue ORGAN_DROP_CHANCE;
    public static final ModConfigSpec.BooleanValue MINION_GRIEF_MESSAGES;
    public static final ModConfigSpec.BooleanValue CLASSIC_STATS;
    public static final ModConfigSpec.BooleanValue CREEPER_TORSO_BREAKS_BLOCKS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("spawning");
        RARITY_NIGHT_CRAWLERS = b.comment("Randomly, one in THIS many naturally spawning Zombies or Skeletons becomes a Night Crawler instead. 0 turns them off.")
                .defineInRange("rarityNightCrawlers", 30, 0, 100000);
        RARITY_ISAACS = b.comment("Randomly, one in THIS many naturally spawning Zombies or Skeletons becomes Isaac instead. 0 turns him off.")
                .defineInRange("rarityIsaacs", 30, 0, 100000);
        b.pop();
        b.push("minions");
        MAX_MINIONS = b.comment("How many living Minions one player may have at once. -1 means no limit (the old max_minion_spawn server property).")
                .defineInRange("maxMinionsPerPlayer", -1, -1, 1000);
        MINION_GRIEF_MESSAGES = b.comment("Minions talk to their master in chat (\"Your bidding?\", sit/follow replies).")
                .define("minionChat", true);
        b.pop();
        b.push("balance");
        CLASSIC_STATS = b.comment("Use the exact 1.7.1 numbers: base speed 0.1, the old Wolf head, no speed cap. Off by default (base speed 0.065, speed cap 0.35).")
                .define("classicStats", false);
        CREEPER_TORSO_BREAKS_BLOCKS = b.comment("A Minion with a Creeper torso explodes when it dies. Should that explosion break blocks (also needs mobGriefing)?")
                .define("creeperTorsoBreaksBlocks", false);
        b.pop();
        b.push("drops");
        ORGAN_DROP_CHANCE = b.comment("Multiplier on the organ drop chances (brains 1%, heart 1%, muscle 4%, lungs 1%, skin from leather in the Sewing Machine). 0 turns organ drops off.")
                .defineInRange("organDropMultiplier", 1.0, 0.0, 100.0);
        b.pop();
        SPEC = b.build();
    }

    private NecroConfig() {
    }
}
