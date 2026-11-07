package com.sirolf2009.necromancy.soul;

import java.util.List;

import com.sirolf2009.necromancy.NecroConfig;
import com.sirolf2009.necromancy.registry.NecroFluids;
import com.sirolf2009.necromancy.registry.NecroTags;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;

/**
 * The dark décor around a Summoning Altar, counted like bookshelves around an enchanting table (docs/REVIEW.md #6).
 * Blocks within 4 horizontally and from 1 below to 3 above the altar give points; points give a tier; the tier sets how
 * many Minions the necromancer may keep.
 */
public final class Shrine {
    public static final int RADIUS = 4;
    public static final int TIER_2 = 10;
    public static final int TIER_3 = 24;

    private Shrine() {
    }

    public record State(int points, int tier) {
        /** Minions a player may keep when raising at this shrine; -1 = no limit. */
        public int minionCap() {
            if (!NecroConfig.USE_SHRINE_TIERS.get()) {
                return NecroConfig.MAX_MINIONS.get();
            }
            List<? extends Integer> caps = NecroConfig.SHRINE_TIER_CAPS.get();
            int index = Math.min(tier - 1, caps.size() - 1);
            return index < 0 ? 3 : caps.get(index);
        }
    }

    public static State scan(Level level, BlockPos altar) {
        int candles = 0;
        int soulLights = 0;
        int blood = 0;
        int other = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                for (int dy = -1; dy <= 3; dy++) {
                    pos.setWithOffset(altar, dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    if (state.is(NecroTags.FOCUS_CANDLES) && lit(state)) {
                        candles += state.hasProperty(CandleBlock.CANDLES) ? state.getValue(CandleBlock.CANDLES) : 1;
                    } else if (state.is(NecroTags.FOCUS_SOUL_LIGHTS) && lit(state)) {
                        soulLights++;
                    } else if (state.is(NecroTags.FOCUS_DRAGON)) {
                        other += 6;
                    } else if (state.is(NecroTags.FOCUS_DREAD)) {
                        other += 3;
                    } else if (state.is(NecroTags.FOCUS_HEADS)) {
                        other += 2;
                    } else {
                        FluidState fluid = state.getFluidState();
                        if (fluid.isSource() && fluid.getType().isSame(NecroFluids.BLOOD.get())) {
                            blood++;
                        }
                    }
                }
            }
        }
        int points = Math.min(candles, 8) + Math.min(soulLights, 6) + Math.min(blood, 4) + other;
        int tier = points >= TIER_3 ? 3 : points >= TIER_2 ? 2 : 1;
        return new State(points, tier);
    }

    private static boolean lit(BlockState state) {
        return !state.hasProperty(BlockStateProperties.LIT) || state.getValue(BlockStateProperties.LIT);
    }
}
