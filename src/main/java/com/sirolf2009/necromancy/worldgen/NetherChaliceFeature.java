package com.sirolf2009.necromancy.worldgen;

import java.util.ArrayList;
import java.util.List;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.registry.NecroBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The Nether Chalice (sirolf2009's WorldGenNetherChalice): a nether brick cup of blood on a stem of iron bars, standing in
 * a lava ocean. The cup and the stem are the 1.7 shape block for block. New: a foot on the lava with a small loot chest,
 * a pillar under the stem down to the lava floor, and the whole 7 x 7 footprint kept inside the chunk being decorated (the
 * old generator multiplied the chunk index by a random offset and wrote far outside the chunk).
 */
public class NetherChaliceFeature extends Feature<NoneFeatureConfiguration> {
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, Necromancy.id("chests/nether_chalice"));

    private static final int SIZE = 7;
    private static final int HEIGHT = 23;
    /** How far below the placement height to look for the lava surface. */
    private static final int SEARCH_DOWN = 16;
    private static final int MAX_PILLAR = 48;

    public NetherChaliceFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos at = context.origin();
        // keep the footprint inside this chunk
        int chunkX = SectionPos.sectionToBlockCoord(SectionPos.blockToSectionCoord(at.getX()));
        int chunkZ = SectionPos.sectionToBlockCoord(SectionPos.blockToSectionCoord(at.getZ()));
        int x = chunkX + Math.min(at.getX() - chunkX, 16 - SIZE);
        int z = chunkZ + Math.min(at.getZ() - chunkZ, 16 - SIZE);
        BlockPos origin = findSurface(level, new BlockPos(x, at.getY(), z));
        if (origin == null || !fits(level, origin)) {
            return false;
        }
        build(level, origin, context.random());
        return true;
    }

    /** The topmost lava source with air above it, at most SEARCH_DOWN below the start, at the stem's centre. */
    private static BlockPos findSurface(WorldGenLevel level, BlockPos start) {
        BlockPos.MutableBlockPos pos = start.offset(3, 0, 3).mutable();
        for (int i = 0; i <= SEARCH_DOWN && pos.getY() > level.getMinBuildHeight(); i++, pos.move(Direction.DOWN)) {
            if (isLavaSource(level, pos) && level.getBlockState(pos.above()).isAir()) {
                return pos.offset(-3, 0, -3).immutable();
            }
        }
        return null;
    }

    /** The 5 x 5 under the foot is open lava and the whole height above it is air. */
    private static boolean fits(WorldGenLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = 1; dx <= 5; dx++) {
            for (int dz = 1; dz <= 5; dz++) {
                if (!isLavaSource(level, pos.setWithOffset(origin, dx, 0, dz))) {
                    return false;
                }
            }
        }
        if (origin.getY() + HEIGHT >= level.getMaxBuildHeight()) {
            return false;
        }
        for (int dx = 0; dx < SIZE; dx++) {
            for (int dz = 0; dz < SIZE; dz++) {
                for (int dy = 1; dy < HEIGHT; dy++) {
                    if (!level.getBlockState(pos.setWithOffset(origin, dx, dy, dz)).isAir()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean isLavaSource(WorldGenLevel level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        return fluid.is(Fluids.LAVA) && fluid.isSource() && level.getBlockState(pos).is(Blocks.LAVA);
    }

    private static void build(WorldGenLevel level, BlockPos o, RandomSource random) {
        Builder b = new Builder(level, o);
        BlockState brick = Blocks.NETHER_BRICKS.defaultBlockState();
        BlockState fence = Blocks.NETHER_BRICK_FENCE.defaultBlockState();
        BlockState bars = Blocks.IRON_BARS.defaultBlockState();
        BlockState blood = NecroBlocks.BLOOD.get().defaultBlockState();
        BlockState torch = Blocks.REDSTONE_TORCH.defaultBlockState();

        // pillar under the stem, down to the floor of the lava ocean
        for (int dy = -1; dy >= -MAX_PILLAR; dy--) {
            BlockState below = level.getBlockState(o.offset(3, dy, 3));
            if (!below.is(Blocks.LAVA) && !below.isAir() || o.getY() + dy <= level.getMinBuildHeight()) {
                break;
            }
            b.fill(2, dy, 2, 4, dy, 4, brick);
        }
        // the foot, floating on the lava, with a rim of stairs
        b.fill(1, 0, 1, 5, 0, 5, brick);
        for (int i = 2; i <= 4; i++) {
            b.set(0, 0, i, stairs(Direction.EAST, Half.BOTTOM));
            b.set(6, 0, i, stairs(Direction.WEST, Half.BOTTOM));
            b.set(i, 0, 0, stairs(Direction.SOUTH, Half.BOTTOM));
            b.set(i, 0, 6, stairs(Direction.NORTH, Half.BOTTOM));
        }
        // the stem: a ring of iron bars around a column of blood
        for (int dy = 1; dy <= 17; dy++) {
            for (int dx = 2; dx <= 4; dx++) {
                for (int dz = 2; dz <= 4; dz++) {
                    b.set(dx, dy, dz, dx == 3 && dz == 3 ? blood : bars);
                }
            }
        }
        b.set(3, 18, 3, blood);
        // y + 18: the plate under the cup
        for (int i = 2; i <= 4; i++) {
            b.set(1, 18, i, stairs(Direction.EAST, Half.TOP));
            b.set(5, 18, i, stairs(Direction.WEST, Half.TOP));
            b.set(i, 18, 1, stairs(Direction.SOUTH, Half.TOP));
            b.set(i, 18, 5, stairs(Direction.NORTH, Half.TOP));
        }
        for (int dx = 2; dx <= 4; dx++) {
            for (int dz = 2; dz <= 4; dz++) {
                if (dx != 3 || dz != 3) {
                    b.set(dx, 18, dz, brick);
                }
            }
        }
        b.set(1, 18, 1, fence);
        b.set(1, 18, 5, fence);
        b.set(5, 18, 1, fence);
        b.set(5, 18, 5, fence);
        // y + 19: the cup's lower ring, 3 x 3 of blood inside
        for (int i = 2; i <= 4; i++) {
            b.set(0, 19, i, stairs(Direction.EAST, Half.TOP));
            b.set(6, 19, i, stairs(Direction.WEST, Half.TOP));
            b.set(i, 19, 0, stairs(Direction.SOUTH, Half.TOP));
            b.set(i, 19, 6, stairs(Direction.NORTH, Half.TOP));
        }
        for (int i = 1; i <= 5; i++) {
            b.set(1, 19, i, brick);
            b.set(5, 19, i, brick);
            b.set(i, 19, 1, brick);
            b.set(i, 19, 5, brick);
        }
        b.fill(2, 19, 2, 4, 19, 4, blood);
        for (int[] c : new int[][] {{0, 1}, {0, 5}, {1, 0}, {1, 6}, {5, 0}, {5, 6}, {6, 1}, {6, 5}}) {
            b.set(c[0], 19, c[1], fence);
            b.set(c[0], 20, c[1], fence);
            b.set(c[0], 21, c[1], fence);
            b.set(c[0], 22, c[1], torch);
        }
        // y + 20: the brim, a rounded 5 x 5 of blood
        for (int i = 2; i <= 4; i++) {
            b.set(0, 20, i, brick);
            b.set(6, 20, i, brick);
            b.set(i, 20, 0, brick);
            b.set(i, 20, 6, brick);
        }
        b.set(1, 20, 1, brick);
        b.set(1, 20, 5, brick);
        b.set(5, 20, 1, brick);
        b.set(5, 20, 5, brick);
        for (int dx = 1; dx <= 5; dx++) {
            for (int dz = 1; dz <= 5; dz++) {
                boolean corner = (dx == 1 || dx == 5) && (dz == 1 || dz == 5);
                if (!corner) {
                    b.set(dx, 20, dz, blood);
                }
            }
        }
        // y + 21: a railing of fences round the brim
        for (int i = 1; i <= 5; i++) {
            b.set(0, 21, i, fence);
            b.set(6, 21, i, fence);
            b.set(i, 21, 0, fence);
            b.set(i, 21, 6, fence);
        }
        b.set(1, 21, 1, fence);
        b.set(1, 21, 5, fence);
        b.set(5, 21, 1, fence);
        b.set(5, 21, 5, fence);
        // the offering chest on the foot
        BlockPos chest = o.offset(3, 1, 1);
        b.set(3, 1, 1, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));
        RandomizableContainer.setBlockEntityLootTable(level, random, chest, LOOT);
        b.connect();
    }

    private static BlockState stairs(Direction facing, Half half) {
        return Blocks.NETHER_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, half);
    }

    /** Places blocks relative to the chalice origin, then lets fences, bars and stairs join up with their neighbours. */
    private static final class Builder {
        private final WorldGenLevel level;
        private final BlockPos origin;
        private final List<BlockPos> shaped = new ArrayList<>();

        Builder(WorldGenLevel level, BlockPos origin) {
            this.level = level;
            this.origin = origin;
        }

        void set(int dx, int dy, int dz, BlockState state) {
            BlockPos pos = origin.offset(dx, dy, dz);
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            if (state.getFluidState().isEmpty() && !state.is(Blocks.NETHER_BRICKS)) {
                shaped.add(pos);
            }
        }

        void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
            for (int dx = x0; dx <= x1; dx++) {
                for (int dy = y0; dy <= y1; dy++) {
                    for (int dz = z0; dz <= z1; dz++) {
                        set(dx, dy, dz, state);
                    }
                }
            }
        }

        void connect() {
            for (BlockPos pos : shaped) {
                BlockState state = level.getBlockState(pos);
                BlockState updated = Block.updateFromNeighbourShapes(state, level, pos);
                if (updated != state && !updated.isAir()) {
                    level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
                }
            }
        }
    }
}
