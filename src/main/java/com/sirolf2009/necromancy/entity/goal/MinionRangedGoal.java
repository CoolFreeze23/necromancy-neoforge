package com.sirolf2009.necromancy.entity.goal;

import java.util.EnumSet;

import com.sirolf2009.necromancy.entity.MinionEntity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Shoots a held bow or throws a held trident at the target, keeping some distance like a skeleton. Skeleton-family arms
 * shoot every 1-2 seconds, other arms every 3 seconds (docs/REVIEW.md #1, #3).
 */
public class MinionRangedGoal extends Goal {
    private final MinionEntity minion;
    private int cooldown;
    private int seeTime;

    public MinionRangedGoal(MinionEntity minion) {
        this.minion = minion;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = minion.getTarget();
        return target != null && target.isAlive() && minion.hasRangedWeapon() && !minion.isOrderedToSit();
    }

    @Override
    public void stop() {
        seeTime = 0;
        minion.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = minion.getTarget();
        if (target == null) {
            return;
        }
        double distance = minion.distanceToSqr(target);
        boolean sees = minion.getSensing().hasLineOfSight(target);
        seeTime = sees ? seeTime + 1 : 0;
        if (distance > 144 || !sees) {
            minion.getNavigation().moveTo(target, 1.0);
        } else if (distance < 25) {
            minion.getNavigation().stop();
            minion.getMoveControl().strafe(-0.5F, 0);
        } else {
            minion.getNavigation().stop();
        }
        minion.getLookControl().setLookAt(target, 30F, 30F);
        if (--cooldown <= 0 && sees && seeTime > 5 && distance < 400) {
            minion.performRangedAttack(target, 1F);
            cooldown = minion.rangedInterval();
        }
    }
}
