package com.sirolf2009.necromancy.entity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.api.BodyPartLocation;
import com.sirolf2009.necromancy.api.NecroMob;
import com.sirolf2009.necromancy.api.NecroMobRegistry;
import com.sirolf2009.necromancy.api.PartType;
import com.sirolf2009.necromancy.registry.NecroComponents;
import com.sirolf2009.necromancy.registry.NecroItems;
import com.sirolf2009.necromancy.soul.SoulData;
import com.sirolf2009.necromancy.soul.SoulPotency;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Abomination: the necromancer's failed experiment, raised by the Rite at a tier III shrine (docs/REVIEW.md #17).
 * An ownerless Minion of heavy parts with all their traits, 300 health, a boss bar, and at 75%, 50% and 25% health it
 * tears off a part (which drops) and spawns three zombies. It leaves a Legendary soul behind.
 */
public class AbominationEntity extends MinionEntity {
    private static final float[] SHED_AT = { 0.75F, 0.5F, 0.25F };
    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.necromancy.abomination"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private int shed;

    public AbominationEntity(EntityType<? extends MinionEntity> type, Level level) {
        super(type, level);
        xpReward = 50;
    }

    public static AttributeSupplier.Builder createAbominationAttributes() {
        return MinionEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 300.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.SCALE, 1.4);
    }

    /** Heavy parts (weight 3 or more) for every slot; the head always has an ability. */
    public static Map<BodyPartLocation, ResourceLocation> randomBody(RandomSource random) {
        Map<BodyPartLocation, ResourceLocation> body = new EnumMap<>(BodyPartLocation.class);
        for (BodyPartLocation location : BodyPartLocation.values()) {
            List<NecroMob> choices = new ArrayList<>();
            for (NecroMob mob : NecroMobRegistry.all()) {
                if (mob.weight() >= 3 && mob.hasPart(location.type()) && (location != BodyPartLocation.HEAD || !mob.traits(PartType.HEAD).isEmpty())) {
                    choices.add(mob);
                }
            }
            if (!choices.isEmpty()) {
                body.put(location, choices.get(random.nextInt(choices.size())).id());
            }
        }
        return body;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(3, new com.sirolf2009.necromancy.entity.goal.MinionRangedGoal(this));
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, MinionEntity.class, true));
    }

    /** Its own parts' stats are ignored: the boss has fixed health; the parts give traits and damage (x1.5). */
    @Override
    public void applyPartAttributes(boolean heal) {
        super.applyPartAttributes(false);
        var health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.removeModifier(Necromancy.id("parts"));
        }
        var damage = getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            damage.removeModifier(Necromancy.id("abomination"));
            damage.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(Necromancy.id("abomination"), 0.5,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        if (heal) {
            setHealth(getMaxHealth());
        }
    }

    @Override
    public boolean isOwnedBy(LivingEntity entity) {
        return false;
    }

    @Override
    public net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        return net.minecraft.world.InteractionResult.PASS;
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        return true;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (shed < SHED_AT.length && getHealth() < getMaxHealth() * SHED_AT[shed]) {
            shedPart();
            shed++;
        }
    }

    /** Tears off a random part other than the head: it drops, its trait is gone, and the dead rise around it. */
    private void shedPart() {
        List<BodyPartLocation> attached = new ArrayList<>();
        for (BodyPartLocation location : BodyPartLocation.values()) {
            if (location != BodyPartLocation.HEAD && part(location) != null) {
                attached.add(location);
            }
        }
        if (!(level() instanceof ServerLevel server) || attached.isEmpty()) {
            return;
        }
        BodyPartLocation lost = attached.get(random.nextInt(attached.size()));
        NecroMob mob = part(lost);
        Map<BodyPartLocation, ResourceLocation> body = parts();
        body.remove(lost);
        setParts(body);
        applyPartAttributes(false);
        if (mob != null) {
            mob.partItem(lost.type()).ifPresent(item -> spawnAtLocation(new ItemStack(item)));
        }
        playSound(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 1.5F, 0.6F);
        server.sendParticles(ParticleTypes.DAMAGE_INDICATOR, getX(), getY(0.6), getZ(), 20, 0.6, 0.6, 0.6, 0.2);
        int zombies = 1 + server.getDifficulty().getId();
        for (int i = 0; i < zombies; i++) {
            Zombie zombie = (random.nextBoolean() ? EntityType.HUSK : EntityType.ZOMBIE).create(server);
            if (zombie != null) {
                zombie.moveTo(getX() + random.nextGaussian() * 2, getY(), getZ() + random.nextGaussian() * 2, random.nextFloat() * 360F, 0);
                zombie.finalizeSpawn(server, server.getCurrentDifficultyAt(zombie.blockPosition()), MobSpawnType.REINFORCEMENT, null);
                if (getTarget() != null) {
                    zombie.setTarget(getTarget());
                }
                server.addFreshEntity(zombie);
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        for (BodyPartLocation location : BodyPartLocation.values()) {
            NecroMob mob = part(location);
            if (mob != null) {
                mob.partItem(location.type()).ifPresent(item -> spawnAtLocation(new ItemStack(item)));
            }
        }
        ItemStack heart = new ItemStack(NecroItems.SOUL_IN_A_JAR.get());
        heart.set(NecroComponents.SOUL.get(), SoulData.of(getType(), SoulPotency.LEGENDARY));
        heart.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                Component.translatable("item.necromancy.abomination_heart").withStyle(net.minecraft.ChatFormatting.GOLD));
        spawnAtLocation(heart);
        int organs = 3 + random.nextInt(3);
        for (int i = 0; i < organs; i++) {
            var organ = switch (random.nextInt(3)) {
                case 0 -> NecroItems.BRAINS.get();
                case 1 -> NecroItems.HEART.get();
                default -> NecroItems.LUNGS.get();
            };
            spawnAtLocation(new ItemStack(organ));
        }
    }

    @Override
    protected void dropRemains() {
        // its parts drop in dropCustomDeathLoot
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Shed", shed);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        shed = tag.getInt("Shed");
    }
}
