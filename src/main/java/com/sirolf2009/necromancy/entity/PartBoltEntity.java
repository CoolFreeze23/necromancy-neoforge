package com.sirolf2009.necromancy.entity;

import javax.annotation.Nullable;

import com.sirolf2009.necromancy.registry.NecroEntities;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A shot from a Minion part's ranged trait: drawn as any item (OreSpawn's laser, water, acid and ice balls), with its
 * damage, effect, fire and a small blast that never breaks blocks set by the trait.
 */
public class PartBoltEntity extends ThrowableItemProjectile {
    private float damage = 4F;
    @Nullable
    private ResourceLocation effect;
    private int effectDuration;
    private int effectAmplifier;
    private int ignite;
    private float explosion;

    public PartBoltEntity(EntityType<? extends PartBoltEntity> type, Level level) {
        super(type, level);
    }

    public PartBoltEntity(Level level, LivingEntity shooter) {
        super(NecroEntities.PART_BOLT.get(), shooter, level);
    }

    public void configure(ItemStack look, float damage, @Nullable ResourceLocation effect, int effectDuration, int effectAmplifier, int ignite, float explosion) {
        setItem(look.isEmpty() ? new ItemStack(Items.SNOWBALL) : look);
        this.damage = damage;
        this.effect = effect;
        this.effectDuration = effectDuration;
        this.effectAmplifier = effectAmplifier;
        this.ignite = ignite;
        this.explosion = Math.min(1.5F, explosion);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SNOWBALL;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.01;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > 100) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (getOwner() instanceof MinionEntity minion && minion.isFriendly(target)) {
            return false;
        }
        return super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        target.hurt(damageSources().thrown(this, getOwner()), damage);
        if (target instanceof LivingEntity living) {
            if (effect != null && effectDuration > 0) {
                BuiltInRegistries.MOB_EFFECT.getHolder(effect).ifPresent(holder ->
                        living.addEffect(new MobEffectInstance(holder, effectDuration, effectAmplifier), getEffectSource()));
            }
            if (ignite > 0 && !living.fireImmune()) {
                living.igniteForSeconds(ignite);
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            if (explosion > 0) {
                level().explode(this, getX(), getY(), getZ(), explosion, Level.ExplosionInteraction.NONE);
            }
            if (level() instanceof ServerLevel server) {
                server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, getItem()), getX(), getY(), getZ(), 8, 0.1, 0.1, 0.1, 0.05);
            }
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        if (effect != null) {
            tag.putString("Effect", effect.toString());
            tag.putInt("EffectDuration", effectDuration);
            tag.putInt("EffectAmplifier", effectAmplifier);
        }
        tag.putInt("Ignite", ignite);
        tag.putFloat("Explosion", explosion);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        effect = tag.contains("Effect") ? ResourceLocation.tryParse(tag.getString("Effect")) : null;
        effectDuration = tag.getInt("EffectDuration");
        effectAmplifier = tag.getInt("EffectAmplifier");
        ignite = tag.getInt("Ignite");
        explosion = Math.min(1.5F, tag.getFloat("Explosion"));
    }
}
