package com.sirolf2009.necromancy.entity;

import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Necromantic Teddy: sewn from leather and wool. Use it to cycle Walking (follows you), Defending (hunts nearby
 * monsters and scares them away) and Sitting.
 */
public class TeddyEntity extends TamableAnimal {
    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(TeddyEntity.class, EntityDataSerializers.INT);

    public enum State implements StringRepresentable {
        WALKING, DEFENDING, SITTING;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public TeddyEntity(EntityType<? extends TeddyEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, State.WALKING.ordinal());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(3, new ScareMonstersGoal(this, 10.0, 7.0));
        goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.0, 8.0F, 3.0F));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 10.0F));
    }

    public State state() {
        int i = entityData.get(DATA_STATE);
        return State.values()[Math.floorMod(i, State.values().length)];
    }

    public void setState(State state) {
        entityData.set(DATA_STATE, state.ordinal());
        setOrderedToSit(state == State.SITTING);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide) {
            return InteractionResult.CONSUME;
        }
        if (!isTame()) {
            tame(player);
        } else if (!isOwnedBy(player)) {
            return InteractionResult.PASS;
        }
        State next = State.values()[(state().ordinal() + 1) % State.values().length];
        setState(next);
        navigation.stop();
        player.displayClientMessage(Component.translatable("message.necromancy.teddy." + next.getSerializedName()), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("State", state().getSerializedName());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        for (State s : State.values()) {
            if (s.getSerializedName().equals(tag.getString("State"))) {
                setState(s);
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOOL_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOOL_BREAK;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    /** Original EntityAIScareEntities: walk up to the nearest monster, and once close, send it running away from the teddy. */
    static class ScareMonstersGoal extends Goal {
        private final TeddyEntity teddy;
        private final double seekRange;
        private final double scareRange;
        @Nullable
        private PathfinderMob target;

        ScareMonstersGoal(TeddyEntity teddy, double seekRange, double scareRange) {
            this.teddy = teddy;
            this.seekRange = seekRange;
            this.scareRange = scareRange;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (teddy.state() != State.DEFENDING) {
                return false;
            }
            List<PathfinderMob> monsters = teddy.level().getEntitiesOfClass(PathfinderMob.class, teddy.getBoundingBox().inflate(seekRange),
                    mob -> mob instanceof Enemy && mob.isAlive());
            target = null;
            double best = Double.MAX_VALUE;
            for (PathfinderMob mob : monsters) {
                double d = teddy.distanceToSqr(mob);
                if (d < best) {
                    best = d;
                    target = mob;
                }
            }
            return target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && teddy.state() == State.DEFENDING && teddy.distanceToSqr(target) < seekRange * seekRange * 4;
        }

        @Override
        public void stop() {
            target = null;
        }

        @Override
        public void tick() {
            if (target == null) {
                return;
            }
            teddy.getLookControl().setLookAt(target, 30F, 30F);
            teddy.getNavigation().moveTo(target, 1.0);
            if (teddy.distanceToSqr(target) < scareRange * scareRange) {
                Vec3 away = DefaultRandomPos.getPosAway(target, 16, 7, teddy.position());
                if (away != null && teddy.distanceToSqr(away) > teddy.distanceToSqr(target)) {
                    target.getNavigation().moveTo(away.x, away.y, away.z, 1.4);
                    if (target.getTarget() == teddy.getOwner()) {
                        target.setTarget(null);
                    }
                }
            }
        }
    }
}
