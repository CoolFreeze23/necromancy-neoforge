package com.sirolf2009.necromancy.entity;

import com.sirolf2009.necromancy.registry.NecroEntities;
import com.sirolf2009.necromancy.registry.NecroItems;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/** A tear of blood: 6 damage, red mist on impact. */
public class BloodTearEntity extends TearEntity {
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.7F, 0.0F, 0.0F), 1.0F);

    public BloodTearEntity(EntityType<? extends BloodTearEntity> type, Level level) {
        super(type, level);
    }

    public BloodTearEntity(Level level, LivingEntity shooter) {
        super(NecroEntities.BLOOD_TEAR.get(), level, shooter);
    }

    @Override
    protected float damage() {
        return 6F;
    }

    @Override
    protected ParticleOptions impactParticle() {
        return BLOOD;
    }

    @Override
    protected Item getDefaultItem() {
        return NecroItems.BLOOD_TEAR.get();
    }
}
