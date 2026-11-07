package com.sirolf2009.necromancy.entity.goal;

import java.util.Comparator;
import java.util.EnumSet;

import javax.annotation.Nullable;

import com.sirolf2009.necromancy.entity.MinionEntity;
import com.sirolf2009.necromancy.entity.MinionOrder;
import com.sirolf2009.necromancy.trait.DataTraits;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Grazing: a Minion with an eating forager trait (a leaf monster, a caterpillar) eats the blocks it is made for to heal,
 * when it is hurt and idle (or at its post under the Work order, if the trait asks for that). Also the block search and
 * the safety checks every forager shares with the Forager job of {@link MinionWorkGoal}.
 */
public class MinionForageGoal extends Goal {
    private final MinionEntity minion;
    @Nullable
    private BlockPos target;
    private int cooldown;

    public MinionForageGoal(MinionEntity minion) {
        this.minion = minion;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        DataTraits.Forager forager = MinionWorkGoal.forager(minion, true);
        if (forager == null || minion.getTarget() != null || minion.isOrderedToSit() || minion.isVehicle()
                || !minion.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return false;
        }
        boolean wants = forager.needsWork ? minion.order() == MinionOrder.WORK && minion.workPost() != null : minion.getHealth() < minion.getMaxHealth();
        if (!wants || --cooldown > 0) {
            return false;
        }
        cooldown = 40;
        target = find((ServerLevel) minion.level(), minion, forager.needsWork ? minion.workPost() : minion.blockPosition(), forager);
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && minion.getTarget() == null && !minion.isOrderedToSit();
    }

    @Override
    public void stop() {
        target = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        DataTraits.Forager forager = MinionWorkGoal.forager(minion, true);
        if (forager == null || target == null || !(minion.level() instanceof ServerLevel level)) {
            target = null;
            return;
        }
        BlockState state = level.getBlockState(target);
        if (!state.is(forager.blocks)) {
            target = null;
            return;
        }
        minion.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        if (minion.distanceToSqr(target.getX() + 0.5, target.getY(), target.getZ() + 0.5) > 6.25) {
            if (minion.tickCount % 10 == 0) {
                minion.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.9);
            }
            return;
        }
        if (--cooldown > 0) {
            return;
        }
        cooldown = forager.interval;
        if (mayBreak(level, minion, target, state)) {
            minion.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            level.levelEvent(2001, target, Block.getId(state));
            Block replace = forager.replaceWith == null ? Blocks.AIR : BuiltInRegistries.BLOCK.get(forager.replaceWith);
            level.setBlock(target, replace.defaultBlockState(), Block.UPDATE_ALL);
            minion.heal((float) forager.heal);
            minion.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT, 1F, 0.8F);
        }
        target = null;
    }

    /** The nearest block of the forager's kind within its radius of the centre that may be broken. */
    @Nullable
    static BlockPos find(ServerLevel level, MinionEntity minion, BlockPos centre, DataTraits.Forager forager) {
        int r = forager.radius;
        return BlockPos.betweenClosedStream(centre.offset(-r, -3, -r), centre.offset(r, 3, r))
                .filter(p -> level.getBlockState(p).is(forager.blocks) && mayBreak(level, minion, p, level.getBlockState(p)))
                .map(BlockPos::immutable)
                .min(Comparator.comparingDouble(p -> p.distSqr(minion.blockPosition())))
                .orElse(null);
    }

    /** Never next to a bed, a container or the altar; never where a claim (a break event) says no; never unbreakable blocks. */
    static boolean mayBreak(ServerLevel level, MinionEntity minion, BlockPos pos, BlockState state) {
        if (state.getDestroySpeed(level, pos) < 0 || level.getBlockEntity(pos) != null) {
            return false;
        }
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-3, -3, -3), pos.offset(3, 3, 3))) {
            BlockState s = level.getBlockState(near);
            if (s.is(BlockTags.BEDS) || level.getBlockEntity(near) instanceof net.minecraft.world.Container
                    || s.getBlock() instanceof com.sirolf2009.necromancy.block.AltarBlock) {
                return false;
            }
        }
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, FakePlayerFactory.getMinecraft(level));
        return !NeoForge.EVENT_BUS.post(event).isCanceled();
    }
}
