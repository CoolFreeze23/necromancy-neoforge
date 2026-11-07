package com.sirolf2009.necromancy.entity.goal;

import java.util.EnumSet;
import java.util.List;

import com.sirolf2009.necromancy.entity.MinionEntity;
import com.sirolf2009.necromancy.entity.MinionOrder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

/** Walks the patrol route point by point, waiting a moment at each one. */
public class MinionPatrolGoal extends Goal {
    private final MinionEntity minion;
    private int pause;

    public MinionPatrolGoal(MinionEntity minion) {
        this.minion = minion;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return minion.order() == MinionOrder.PATROL && minion.patrolRoute().size() >= 2 && minion.getTarget() == null && !minion.isOrderedToSit();
    }

    @Override
    public void tick() {
        List<BlockPos> route = minion.patrolRoute();
        if (route.isEmpty()) {
            return;
        }
        BlockPos point = route.get(Math.floorMod(minion.patrolIndex(), route.size()));
        if (minion.distanceToSqr(point.getX() + 0.5, point.getY(), point.getZ() + 0.5) < 4) {
            if (pause++ > 40) {
                pause = 0;
                minion.setPatrolIndex(minion.patrolIndex() + 1);
            }
        } else if (minion.getNavigation().isDone() || minion.tickCount % 40 == 0) {
            minion.getNavigation().moveTo(point.getX() + 0.5, point.getY(), point.getZ() + 0.5, 0.8);
        }
    }
}
