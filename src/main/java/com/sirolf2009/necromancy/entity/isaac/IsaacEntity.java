package com.sirolf2009.necromancy.entity.isaac;

import com.sirolf2009.necromancy.entity.TearEntity;
import com.sirolf2009.necromancy.registry.NecroEntities;
import com.sirolf2009.necromancy.registry.NecroSounds;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.level.Level;

/**
 * Isaac, a crying child that replaces a rare Skeleton or Zombie. He shoots tears; when he dies he comes back as Blood
 * Isaac, who splits into a Head and a Body. The Head drops Isaac's Head.
 */
public class IsaacEntity extends IsaacBodyEntity implements RangedAttackMob {
    public IsaacEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void addAttackGoals() {
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 18, 40, 16F));
    }

    protected TearEntity createTear() {
        return new TearEntity(level(), this);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        playSound(NecroSounds.TEAR.get(), 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
        TearEntity tear = createTear();
        tear.aimAt(this, target);
        level().addFreshEntity(tear);
    }

    /** The entity this one turns into when it dies, or null at the end of the chain. */
    protected EntityType<? extends Mob>[] nextStages() {
        @SuppressWarnings("unchecked")
        EntityType<? extends Mob>[] next = new EntityType[] { NecroEntities.BLOOD_ISAAC.get() };
        return next;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server && !isRemoved()) {
            float yOffset = 0;
            for (EntityType<? extends Mob> type : nextStages()) {
                Mob next = type.create(server);
                if (next != null) {
                    next.moveTo(getX(), getY() + yOffset, getZ(), getYRot(), getXRot());
                    next.finalizeSpawn(server, server.getCurrentDifficultyAt(blockPosition()), MobSpawnType.CONVERSION, null);
                    if (getTarget() != null) {
                        next.setTarget(getTarget());
                    }
                    if (isPersistenceRequired()) {
                        next.setPersistenceRequired();
                    }
                    server.addFreshEntity(next);
                    yOffset += 1;
                }
            }
        }
    }
}
