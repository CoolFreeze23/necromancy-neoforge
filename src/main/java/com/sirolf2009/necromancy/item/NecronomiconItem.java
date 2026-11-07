package com.sirolf2009.necromancy.item;

import com.sirolf2009.necromancy.block.AltarBlock;
import com.sirolf2009.necromancy.block.AltarSideBlock;
import com.sirolf2009.necromancy.registry.NecroBlocks;
import com.sirolf2009.necromancy.registry.NecroTags;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Binds an altar: use it on planks with two cobblestone in a row beside it and the three blocks become a Summoning Altar
 * facing along the row. The book is used up, as in the original.
 */
public class NecronomiconItem extends Item {
    public NecronomiconItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(NecroTags.ALTAR_BASE)) {
            return InteractionResult.PASS;
        }
        Direction direction = findSides(level, pos, context.getHorizontalDirection());
        if (direction == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            bind(level, pos, direction);
            Player player = context.getPlayer();
            if (player != null && !player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, context.getItemInHand());
            }
            level.playSound(null, pos, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.BLOCKS, 1F, 0.8F);
            if (level instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 3; i++) {
                    BlockPos p = pos.relative(direction, i);
                    serverLevel.sendParticles(ParticleTypes.SOUL, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0.02);
                }
            }
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** The direction from the planks to the two cobblestone, preferring the way the player looks. */
    private static Direction findSides(Level level, BlockPos pos, Direction preferred) {
        if (hasSides(level, pos, preferred)) {
            return preferred;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (hasSides(level, pos, direction)) {
                return direction;
            }
        }
        return null;
    }

    private static boolean hasSides(Level level, BlockPos pos, Direction direction) {
        return level.getBlockState(pos.relative(direction)).is(NecroTags.ALTAR_SIDES)
                && level.getBlockState(pos.relative(direction, 2)).is(NecroTags.ALTAR_SIDES);
    }

    public static void bind(Level level, BlockPos pos, Direction direction) {
        BlockState base = level.getBlockState(pos);
        BlockState side1 = level.getBlockState(pos.relative(direction));
        BlockState side2 = level.getBlockState(pos.relative(direction, 2));
        level.setBlock(pos, NecroBlocks.SUMMONING_ALTAR.get().defaultBlockState().setValue(AltarBlock.FACING, direction), Block.UPDATE_ALL);
        level.setBlock(pos.relative(direction), NecroBlocks.ALTAR_SIDE.get().defaultBlockState()
                .setValue(AltarSideBlock.FACING, direction).setValue(AltarSideBlock.DISTANCE, 1), Block.UPDATE_ALL);
        level.setBlock(pos.relative(direction, 2), NecroBlocks.ALTAR_SIDE.get().defaultBlockState()
                .setValue(AltarSideBlock.FACING, direction).setValue(AltarSideBlock.DISTANCE, 2), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof com.sirolf2009.necromancy.blockentity.AltarBlockEntity altar) {
            altar.setOriginalBlocks(base, side1, side2);
        }
    }
}
