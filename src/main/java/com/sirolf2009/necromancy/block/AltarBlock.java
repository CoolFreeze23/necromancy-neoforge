package com.sirolf2009.necromancy.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.sirolf2009.necromancy.blockentity.AltarBlockEntity;
import com.sirolf2009.necromancy.registry.NecroBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The heart of the Summoning Altar: the former planks block. FACING points along the row towards the two side blocks.
 * Use it to open the altar, sneak-use (or press Raise in the screen) to raise the Minion. Breaking any of the three
 * blocks unbinds the altar and gives the planks and cobblestone back.
 */
public class AltarBlock extends BaseEntityBlock {
    public static final MapCodec<AltarBlock> CODEC = simpleCodec(AltarBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);

    public AltarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AltarBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof AltarBlockEntity altar)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (player.isSecondaryUseActive()) {
                altar.tryRaise(player);
            } else {
                player.openMenu(altar, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!player.getAbilities().instabuild && level.getBlockEntity(pos) instanceof AltarBlockEntity altar) {
            Block.popResource(level, pos, new ItemStack(altar.originalBase().getBlock()));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!newState.is(this)) {
            if (level.getBlockEntity(pos) instanceof AltarBlockEntity altar) {
                Containers.dropContents(level, pos, altar);
                Direction facing = state.getValue(FACING);
                revertSide(level, pos.relative(facing, 1), altar.originalSide(1));
                revertSide(level, pos.relative(facing, 2), altar.originalSide(2));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static void revertSide(Level level, BlockPos pos, BlockState original) {
        if (level.getBlockState(pos).is(NecroBlocks.ALTAR_SIDE.get())) {
            level.setBlock(pos, original, Block.UPDATE_ALL);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** Comparators read how complete the ritual is: 0 to 15 over blood, soul, head, torso, legs and arms. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AltarBlockEntity altar ? altar.comparatorSignal() : 0;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(com.sirolf2009.necromancy.registry.NecroItems.NECRONOMICON.get());
    }
}
