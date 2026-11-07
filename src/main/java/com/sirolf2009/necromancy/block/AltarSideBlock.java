package com.sirolf2009.necromancy.block;

import com.mojang.serialization.MapCodec;
import com.sirolf2009.necromancy.blockentity.AltarBlockEntity;
import com.sirolf2009.necromancy.registry.NecroBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** One of the two stone blocks of a Summoning Altar (the original "Altar Building Block"). DISTANCE is 1 or 2 from the altar. */
public class AltarSideBlock extends Block {
    public static final MapCodec<AltarSideBlock> CODEC = simpleCodec(AltarSideBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty DISTANCE = IntegerProperty.create("distance", 1, 2);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);

    public AltarSideBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(DISTANCE, 1));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, DISTANCE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Drawn by the altar's renderer, which draws the whole three-block table. */
    @Override
    protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.INVISIBLE;
    }

    public static BlockPos altarPos(BlockState state, BlockPos pos) {
        return pos.relative(state.getValue(FACING).getOpposite(), state.getValue(DISTANCE));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos altarPos = altarPos(state, pos);
        if (!(level.getBlockEntity(altarPos) instanceof AltarBlockEntity altar)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (player.isSecondaryUseActive()) {
                altar.tryRaise(player);
            } else {
                player.openMenu(altar, altarPos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!player.getAbilities().instabuild && level.getBlockEntity(altarPos(state, pos)) instanceof AltarBlockEntity altar) {
            Block.popResource(level, pos, new ItemStack(altar.originalSide(state.getValue(DISTANCE)).getBlock()));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!newState.is(this)) {
            BlockPos altarPos = altarPos(state, pos);
            if (level.getBlockState(altarPos).is(NecroBlocks.SUMMONING_ALTAR.get()) && level.getBlockEntity(altarPos) instanceof AltarBlockEntity altar) {
                level.setBlock(altarPos, altar.originalBase(), Block.UPDATE_ALL);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
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
