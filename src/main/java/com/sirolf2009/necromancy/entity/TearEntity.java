package com.sirolf2009.necromancy.entity;

import com.sirolf2009.necromancy.registry.NecroEntities;
import com.sirolf2009.necromancy.registry.NecroItems;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Isaac's tear: 3 damage, splashes on impact. */
public class TearEntity extends ThrowableItemProjectile {
    public TearEntity(EntityType<? extends TearEntity> type, Level level) {
        super(type, level);
    }

    public TearEntity(Level level, LivingEntity shooter) {
        this(NecroEntities.TEAR.get(), level, shooter);
    }

    protected TearEntity(EntityType<? extends TearEntity> type, Level level, LivingEntity shooter) {
        super(type, shooter, level);
        setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
    }

    protected float damage() {
        return 3F;
    }

    protected ParticleOptions impactParticle() {
        return ParticleTypes.SPLASH;
    }

    @Override
    protected Item getDefaultItem() {
        return NecroItems.TEAR.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.01;
    }

    /** Aims like the original: straight at the target's lower third, slight spread. */
    public void aimAt(LivingEntity shooter, LivingEntity target) {
        double dx = target.getX() - shooter.getX();
        double dy = target.getY(0.3333) - getY();
        double dz = target.getZ() - shooter.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        shoot(dx, dy + horizontal * 0.05, dz, 1.2F, 2.0F);
        setYRot((float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        Entity owner = getOwner();
        if (owner != null && target == owner) {
            return;
        }
        if (owner instanceof MinionEntity minion && target instanceof LivingEntity living && minion.getOwner() == living) {
            return;
        }
        target.hurt(damageSources().thrown(this, owner), damage());
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(impactParticle(), getX(), getY(), getZ(), 8, 0.1, 0.1, 0.1, 0.05);
            discard();
        }
    }
}
