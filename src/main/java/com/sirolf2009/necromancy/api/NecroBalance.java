package com.sirolf2009.necromancy.api;

import com.sirolf2009.necromancy.NecroConfig;

/** The balance switches, read from the common config with safe defaults before it loads. */
public final class NecroBalance {
    private NecroBalance() {
    }

    /** True restores the 1.7.1 numbers exactly: base speed 0.1, original part stats, no caps. */
    public static boolean classic() {
        try {
            return NecroConfig.CLASSIC_STATS.get();
        } catch (IllegalStateException notLoaded) {
            return false;
        }
    }

    public static double baseSpeed() {
        return classic() ? 0.1 : 0.065;
    }

    public static double speedCap() {
        return classic() ? Double.MAX_VALUE : 0.35;
    }
}
