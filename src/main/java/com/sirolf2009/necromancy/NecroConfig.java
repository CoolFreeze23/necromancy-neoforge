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
    public static final ModConfigSpec.BooleanValue USE_SHRINE_TIERS;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends Integer>> SHRINE_TIER_CAPS;
    public static final ModConfigSpec.BooleanValue SOUL_POTENCY;
    public static final ModConfigSpec.BooleanValue CREEPER_TORSO_BREAKS_BLOCKS;
    public static final ModConfigSpec.DoubleValue ORESPAWN_SOUL_HEALTH;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("spawning");
        RARITY_NIGHT_CRAWLERS = b.comment("Randomly, one in THIS many naturally spawning Zombies or Skeletons becomes a Night Crawler instead. 0 turns them off.")
                .defineInRange("rarityNightCrawlers", 30, 0, 100000);
        RARITY_ISAACS = b.comment("Randomly, one in THIS many naturally spawning Zombies or Skeletons becomes Isaac instead. 0 turns him off.")
                .defineInRange("rarityIsaacs", 30, 0, 100000);
        b.pop();
        b.push("minions");
        USE_SHRINE_TIERS = b.comment("The décor around the altar (candles, soul lights, blood, mob heads) sets how many Minions a player may keep.",
                "Off: maxMinionsPerPlayer applies instead.")
                .define("useShrineTiers", true);
        SHRINE_TIER_CAPS = b.comment("Minions allowed at shrine tier I, II and III.")
                .defineList("shrineTierCaps", java.util.List.of(3, 5, 8), () -> 3, o -> o instanceof Integer i && i >= -1);
        MAX_MINIONS = b.comment("With useShrineTiers off: how many living Minions one player may have at once. -1 means no limit (the old max_minion_spawn server property).")
                .defineInRange("maxMinionsPerPlayer", -1, -1, 1000);
        SOUL_POTENCY = b.comment("A soul can only animate a body whose parts weigh up to its potency (from the reaped creature's health). Off: any soul animates any body.")
                .define("soulPotency", true);
        MINION_GRIEF_MESSAGES = b.comment("Minions talk to their master in chat (\"Your bidding?\", sit/follow replies).")
                .define("minionChat", true);
        b.pop();
        b.push("balance");
        CLASSIC_STATS = b.comment("Use the exact 1.7.1 numbers: base speed 0.1, the old Wolf head, no speed cap. Off by default (base speed 0.065, speed cap 0.35).")
                .define("classicStats", false);
        CREEPER_TORSO_BREAKS_BLOCKS = b.comment("A Minion with a Creeper torso explodes when it dies. Should that explosion break blocks (also needs mobGriefing)?")
                .define("creeperTorsoBreaksBlocks", false);
        b.pop();
        b.push("compat");
        ORESPAWN_SOUL_HEALTH = b.comment("OreSpawn creatures have far more health than vanilla ones. Their health counts this much when a soul's potency is read from it (1 = as is).")
                .defineInRange("oreSpawnSoulHealthFactor", 0.5, 0.05, 1.0);
        b.pop();
        b.push("drops");
        ORGAN_DROP_CHANCE = b.comment("Multiplier on the organ drop chances (brains, heart, lungs 2%, muscle 5%, skin 15%, more from organ-rich creatures, +1% per Looting or Reaping level). 0 turns organ drops off.")
                .defineInRange("organDropMultiplier", 1.0, 0.0, 100.0);
        b.pop();
        SPEC = b.build();
    }

    private NecroConfig() {
    }
}
