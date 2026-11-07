package com.sirolf2009.necromancy.entity;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.NecroConfig;
import com.sirolf2009.necromancy.api.BodyPartLocation;
import com.sirolf2009.necromancy.api.NecroMob;
import com.sirolf2009.necromancy.api.NecroMobRegistry;
import com.sirolf2009.necromancy.api.PartStats;
import com.sirolf2009.necromancy.registry.NecroAttachments;
import com.sirolf2009.necromancy.registry.NecroItems;
import com.sirolf2009.necromancy.registry.NecroSounds;
import com.sirolf2009.necromancy.registry.NecroTags;
import com.sirolf2009.necromancy.soul.SoulData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ItemBasedSteering;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A Minion raised on the Summoning Altar from up to five body parts of different creatures. Its stats are the sum of its
 * parts (original NecroAPI modifiers), it follows and fights for its master, sits on command, can be saddled when its
 * torso allows it and steered with a Brain on a Stick, and an Isaac head makes it shoot tears.
 */
public class MinionEntity extends TamableAnimal implements RangedAttackMob, ItemSteerable, Saddleable, net.minecraft.world.entity.PlayerRideableJumping {
    private static final Map<BodyPartLocation, EntityDataAccessor<String>> DATA_PARTS = new EnumMap<>(BodyPartLocation.class);
    private static final EntityDataAccessor<Boolean> DATA_SADDLE = SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_BOOST_TIME = SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_POWERED = SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ORDER = SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STANCE = SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_KILLS = SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BOUND = SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.BOOLEAN);
    /** Kills needed for each rank (docs/REVIEW.md #12). */
    public static final int[] RANK_KILLS = { 10, 30, 75, 150, 300 };
    private static final ResourceLocation RANK_MODIFIER = Necromancy.id("rank");

    static {
        for (BodyPartLocation location : BodyPartLocation.values()) {
            DATA_PARTS.put(location, SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.STRING));
        }
    }

    private final ItemBasedSteering steering = new ItemBasedSteering(entityData, DATA_BOOST_TIME, DATA_SADDLE);
    private int attackAnimation;
    /** Client cache of the parts, rebuilt when the synced strings change. */
    private final Map<BodyPartLocation, NecroMob> partCache = new EnumMap<>(BodyPartLocation.class);
    private boolean partCacheDirty = true;

    public MinionEntity(EntityType<? extends MinionEntity> type, Level level) {
        super(type, level);
        setTame(false, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.1)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1)
                .add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        for (EntityDataAccessor<String> accessor : DATA_PARTS.values()) {
            builder.define(accessor, "");
        }
        builder.define(DATA_SADDLE, false);
        builder.define(DATA_BOOST_TIME, 0);
        builder.define(DATA_POWERED, false);
        builder.define(DATA_ORDER, MinionOrder.FOLLOW.ordinal());
        builder.define(DATA_STANCE, MinionStance.DEFENSIVE.ordinal());
        builder.define(DATA_KILLS, 0);
        builder.define(DATA_BOUND, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this) {
            @Override
            public boolean canUse() {
                return (hasFlag(com.sirolf2009.necromancy.api.TraitFlag.FLOATS) || !hasFlag(com.sirolf2009.necromancy.api.TraitFlag.SINKS)) && super.canUse();
            }
        });
        goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.PanicGoal(this, 1.3) {
            @Override
            public boolean canUse() {
                return stance() == MinionStance.PASSIVE && super.canUse();
            }
        });
        goalSelector.addGoal(4, new com.sirolf2009.necromancy.entity.goal.MinionRangedGoal(this));
        goalSelector.addGoal(4, new LeapAtTargetGoal(this, 0.4F) {
            @Override
            public boolean canUse() {
                return !hasRangedWeapon() && super.canUse();
            }
        });
        goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0, true) {
            @Override
            public boolean canUse() {
                return !hasRangedWeapon() && super.canUse();
            }
        });
        goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0, 10.0F, 2.0F) {
            @Override
            public boolean canUse() {
                return order() == MinionOrder.FOLLOW && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return order() == MinionOrder.FOLLOW && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(6, new net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(6, new com.sirolf2009.necromancy.entity.goal.MinionPatrolGoal(this));
        goalSelector.addGoal(6, new com.sirolf2009.necromancy.entity.goal.MinionWorkGoal(this));
        goalSelector.addGoal(7, new com.sirolf2009.necromancy.entity.goal.MinionForageGoal(this));
        goalSelector.addGoal(7, new TemptGoal(this, 1.0, stack -> stack.is(NecroItems.BRAIN_ON_A_STICK.get()), false));
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return order() != MinionOrder.PATROL && order() != MinionOrder.WORK && super.canUse();
            }
        });
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return stance() != MinionStance.PASSIVE && super.canUse();
            }
        });
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this) {
            @Override
            public boolean canUse() {
                return stance() != MinionStance.PASSIVE && super.canUse();
            }
        });
        targetSelector.addGoal(3, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return stance() != MinionStance.PASSIVE && super.canUse();
            }
        }.setAlertOthers());
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isOrderedTarget));
        targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false, this::isHuntedMonster));
    }

    // --- soul ---

    private SoulData soul = SoulData.UNKNOWN;

    public SoulData soul() {
        return soul;
    }

    public void setSoul(SoulData soul) {
        this.soul = soul;
    }

    // --- body parts ---

    @Nullable
    public NecroMob part(BodyPartLocation location) {
        if (partCacheDirty) {
            partCache.clear();
            for (BodyPartLocation loc : BodyPartLocation.values()) {
                NecroMob mob = NecroMobRegistry.get(ResourceLocation.tryParse(entityData.get(DATA_PARTS.get(loc))));
                if (mob != null && mob.hasPart(loc.type())) {
                    partCache.put(loc, mob);
                }
            }
            partCacheDirty = false;
        }
        return partCache.get(location);
    }

    public Map<BodyPartLocation, ResourceLocation> parts() {
        Map<BodyPartLocation, ResourceLocation> map = new EnumMap<>(BodyPartLocation.class);
        for (BodyPartLocation location : BodyPartLocation.values()) {
            NecroMob mob = part(location);
            if (mob != null) {
                map.put(location, mob.id());
            }
        }
        return map;
    }

    public void setParts(Map<BodyPartLocation, ResourceLocation> parts) {
        for (BodyPartLocation location : BodyPartLocation.values()) {
            ResourceLocation id = parts.get(location);
            entityData.set(DATA_PARTS.get(location), id == null ? "" : id.toString());
        }
        partCacheDirty = true;
        refreshDimensions();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (DATA_PARTS.containsValue(key)) {
            partCacheDirty = true;
            refreshDimensions();
        }
        if (DATA_BOOST_TIME.equals(key) && level().isClientSide) {
            steering.onSynced();
        }
        super.onSyncedDataUpdated(key);
    }

    /**
     * Applies the parts' stat modifiers (ADD_MULTIPLIED_BASE, as the original's operation 1), summed into one modifier per
     * attribute, plus each part's extra attributes. Permanent, so they are saved with the entity and in place before its
     * health is read back. Speed is capped unless classic stats are on.
     */
    public void applyPartAttributes(boolean heal) {
        double health = 0, follow = 0, knockback = 0, speed = 0, damage = 0;
        Map<Holder<Attribute>, double[]> extras = new java.util.LinkedHashMap<>();
        for (BodyPartLocation location : BodyPartLocation.values()) {
            NecroMob mob = part(location);
            if (mob == null) {
                continue;
            }
            PartStats stats = mob.stats(location);
            health += stats.health();
            follow += stats.followRange();
            knockback += stats.knockbackResistance();
            speed += stats.movementSpeed();
            damage += stats.attackDamage();
            for (NecroMob.ExtraAttribute extra : mob.extraAttributes(location.type())) {
                double[] sums = extras.computeIfAbsent(extra.attribute(), a -> new double[3]);
                sums[extra.operation().ordinal()] += extra.amount();
            }
        }
        AttributeInstance speedAttribute = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            double base = com.sirolf2009.necromancy.api.NecroBalance.baseSpeed();
            speedAttribute.setBaseValue(base);
            double cap = com.sirolf2009.necromancy.api.NecroBalance.speedCap();
            if (base * (1 + speed) > cap) {
                speed = cap / base - 1;
            }
        }
        modifier(Attributes.MAX_HEALTH, PARTS_MODIFIER, health, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(Attributes.FOLLOW_RANGE, PARTS_MODIFIER, follow, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(Attributes.KNOCKBACK_RESISTANCE, PARTS_MODIFIER, knockback, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(Attributes.MOVEMENT_SPEED, PARTS_MODIFIER, speed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(Attributes.ATTACK_DAMAGE, PARTS_MODIFIER, damage, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(Attributes.MAX_HEALTH, Necromancy.id("legendary"), soul.potency() == com.sirolf2009.necromancy.soul.SoulPotency.LEGENDARY ? 0.2 : 0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        // extra attributes: one modifier per attribute and operation; clear the old ones first
        for (var holder : net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.holders().toList()) {
            AttributeInstance instance = getAttribute(holder);
            if (instance != null) {
                for (AttributeModifier.Operation op : AttributeModifier.Operation.values()) {
                    instance.removeModifier(extraId(op));
                }
            }
        }
        extras.forEach((attribute, sums) -> {
            for (AttributeModifier.Operation op : AttributeModifier.Operation.values()) {
                double amount = sums[op.ordinal()];
                if (attribute.is(Attributes.ARMOR.unwrapKey().orElseThrow()) && op == AttributeModifier.Operation.ADD_VALUE) {
                    amount = Math.min(amount, 20);
                }
                modifier(attribute, extraId(op), amount, op);
            }
        });
        if (heal) {
            setHealth(getMaxHealth());
        } else if (getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
    }

    private static final ResourceLocation PARTS_MODIFIER = Necromancy.id("parts");

    private static ResourceLocation extraId(AttributeModifier.Operation op) {
        return Necromancy.id("parts/" + op.getSerializedName());
    }

    private void modifier(Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0) {
            instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
        }
    }

    /** Hitbox grows with the body: tall legs and torsos make tall Minions. */
    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions base = super.getDefaultDimensions(pose);
        NecroMob legs = part(BodyPartLocation.LEGS);
        NecroMob torso = part(BodyPartLocation.TORSO);
        if (legs == null && torso == null) {
            return base;
        }
        float height = (legs == null ? 0 : legs.legHeight()) + (torso == null ? 0 : torso.torsoHeight())
                + (part(BodyPartLocation.HEAD) == null ? 0 : 0.45F);
        float width = Math.max(0.6F, torso == null ? 0.6F : torso.torsoWidth());
        return EntityDimensions.scalable(width, Math.max(0.8F, height)).withEyeHeight(Math.max(0.6F, height - 0.25F));
    }

    // --- saving ---

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        CompoundTag parts = new CompoundTag();
        for (BodyPartLocation location : BodyPartLocation.values()) {
            String id = entityData.get(DATA_PARTS.get(location));
            if (!id.isEmpty()) {
                parts.putString(location.getSerializedName(), id);
            }
        }
        tag.put("Parts", parts);
        if (isPowered()) {
            tag.putBoolean("Powered", true);
        }
        SoulData.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, soul).result().ifPresent(t -> tag.put("Soul", t));
        tag.putString("Order", order().getSerializedName());
        tag.putString("Stance", stance().getSerializedName());
        tag.putInt("Kills", kills());
        tag.putBoolean("Bound", isBound());
        if (guardPost != null) {
            tag.put("GuardPost", net.minecraft.nbt.NbtUtils.writeBlockPos(guardPost));
        }
        if (workPost != null) {
            tag.put("WorkPost", net.minecraft.nbt.NbtUtils.writeBlockPos(workPost));
        }
        if (workContainer != null) {
            tag.put("WorkContainer", net.minecraft.nbt.NbtUtils.writeBlockPos(workContainer));
        }
        tag.put("Pack", pack.createTag(registryAccess()));
        if (!patrolRoute.isEmpty()) {
            net.minecraft.nbt.ListTag route = new net.minecraft.nbt.ListTag();
            patrolRoute.forEach(p -> route.add(net.minecraft.nbt.NbtUtils.writeBlockPos(p)));
            tag.put("PatrolRoute", route);
            tag.putInt("PatrolIndex", patrolIndex);
        }
        steering.addAdditionalSaveData(tag);
        if (spawnlingOf != null) {
            tag.putUUID("SpawnlingOf", spawnlingOf);
            tag.putInt("SpawnlingLife", spawnlingLife);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        Map<BodyPartLocation, ResourceLocation> map = new EnumMap<>(BodyPartLocation.class);
        CompoundTag parts = tag.getCompound("Parts");
        for (BodyPartLocation location : BodyPartLocation.values()) {
            ResourceLocation id = ResourceLocation.tryParse(parts.getString(location.getSerializedName()));
            if (id != null && NecroMobRegistry.get(id) != null) {
                map.put(location, id);
            } else if (id != null && !parts.getString(location.getSerializedName()).isEmpty()) {
                Necromancy.LOGGER.warn("Minion {} has an unknown {} part {}, dropping it", getUUID(), location.getSerializedName(), id);
            }
        }
        setParts(map);
        entityData.set(DATA_POWERED, tag.getBoolean("Powered"));
        entityData.set(DATA_KILLS, tag.getInt("Kills"));
        pack.fromTag(tag.getList("Pack", net.minecraft.nbt.Tag.TAG_COMPOUND), registryAccess());
        entityData.set(DATA_BOUND, tag.getBoolean("Bound"));
        if (tag.contains("Stance")) {
            entityData.set(DATA_STANCE, MinionStance.byName(tag.getString("Stance")).ordinal());
        }
        java.util.List<BlockPos> route = new java.util.ArrayList<>();
        for (net.minecraft.nbt.Tag t : tag.getList("PatrolRoute", net.minecraft.nbt.Tag.TAG_INT_ARRAY)) {
            if (t instanceof net.minecraft.nbt.IntArrayTag array && array.size() == 3) {
                route.add(new BlockPos(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt()));
            }
        }
        if (tag.contains("Order")) {
            MinionOrder order = MinionOrder.byName(tag.getString("Order"));
            BlockPos post = net.minecraft.nbt.NbtUtils.readBlockPos(tag, order == MinionOrder.WORK ? "WorkPost" : "GuardPost").orElse(null);
            workContainer = net.minecraft.nbt.NbtUtils.readBlockPos(tag, "WorkContainer").orElse(null);
            giveOrder(order, post, route, true);
            patrolIndex = tag.getInt("PatrolIndex");
        }
        if (tag.contains("Soul")) {
            soul = SoulData.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("Soul")).result().orElse(SoulData.UNKNOWN);
        }
        // a Minion made with /summon has parts but no saved modifiers yet: give them now (and full health if none was given)
        if (!level().isClientSide && !map.isEmpty()) {
            applyPartAttributes(!tag.contains("Health"));
        }
        steering.readAdditionalSaveData(tag);
        if (tag.hasUUID("SpawnlingOf")) {
            spawnlingOf = tag.getUUID("SpawnlingOf");
            spawnlingLife = tag.getInt("SpawnlingLife");
        }
    }

    // --- behaviour ---

    @Override
    public void tick() {
        super.tick();
        if (attackAnimation > 0) {
            attackAnimation--;
        }
        if (!level().isClientSide && tickCount % 40 == 0 && getHealth() < getMaxHealth()
                && isInFluidType(com.sirolf2009.necromancy.registry.NecroFluids.BLOOD_TYPE.value())) {
            heal(1F);
        }
        if (!level().isClientSide) {
            if (attackAnimation > 0 || getDeltaMovement().horizontalDistanceSqr() > 1.0E-5) {
                lastActiveTick = tickCount;
            }
            if (spawnlingOf != null && (--spawnlingLife <= 0 || tickCount % 20 == 0
                    && !(((ServerLevel) level()).getEntity(spawnlingOf) instanceof MinionEntity summoner && summoner.isAlive()))) {
                crumble();
                return;
            }
        }
        if (!level().isClientSide && tickCount > 20 && part(BodyPartLocation.HEAD) == null) {
            // a body without a head does not hold together (as in the original)
            hurt(damageSources().generic(), Float.MAX_VALUE);
        }
    }

    // --- orders, stance and rank ---

    @Nullable
    private BlockPos guardPost;
    @Nullable
    private BlockPos workPost;
    @Nullable
    private BlockPos workContainer;
    private final net.minecraft.world.SimpleContainer pack = new net.minecraft.world.SimpleContainer(9);
    private final java.util.List<BlockPos> patrolRoute = new java.util.ArrayList<>();
    private int patrolIndex;

    public MinionOrder order() {
        return MinionOrder.values()[Math.floorMod(entityData.get(DATA_ORDER), MinionOrder.values().length)];
    }

    public MinionStance stance() {
        return MinionStance.values()[Math.floorMod(entityData.get(DATA_STANCE), MinionStance.values().length)];
    }

    public void setStance(MinionStance stance) {
        entityData.set(DATA_STANCE, stance.ordinal());
        if (stance == MinionStance.PASSIVE) {
            setTarget(null);
        }
    }

    /** Gives an order. {@code pos} is the guard post for GUARD; PATROL uses {@code route}. */
    public void giveOrder(MinionOrder order, @Nullable BlockPos pos, java.util.List<BlockPos> route) {
        giveOrder(order, pos, route, false);
    }

    private void giveOrder(MinionOrder order, @Nullable BlockPos pos, java.util.List<BlockPos> route, boolean loading) {
        entityData.set(DATA_ORDER, order.ordinal());
        setOrderedToSit(order == MinionOrder.STAY);
        navigation.stop();
        switch (order) {
            case GUARD -> {
                guardPost = pos != null ? pos.immutable() : blockPosition();
                restrictTo(guardPost, 12);
            }
            case WORK -> {
                workPost = pos != null ? pos.immutable() : blockPosition();
                if (!loading && level().isLoaded(workPost)
                        && level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, workPost, null) != null) {
                    workContainer = workPost;
                }
                restrictTo(workPost, 10);
            }
            case PATROL -> {
                patrolRoute.clear();
                patrolRoute.addAll(route.stream().limit(4).map(BlockPos::immutable).toList());
                patrolIndex = 0;
                clearRestriction();
            }
            default -> {
                guardPost = null;
                clearRestriction();
            }
        }
    }

    @Nullable
    public BlockPos workPost() {
        return workPost;
    }

    @Nullable
    public BlockPos workContainer() {
        return workContainer;
    }

    /** What the Minion gathered on the job, until it takes it to its container. */
    public net.minecraft.world.SimpleContainer pack() {
        return pack;
    }

    public java.util.List<BlockPos> patrolRoute() {
        return patrolRoute;
    }

    public int patrolIndex() {
        return patrolIndex;
    }

    public void setPatrolIndex(int index) {
        patrolIndex = index;
    }

    @Nullable
    public BlockPos guardPost() {
        return guardPost;
    }

    @Override
    public void setOrderedToSit(boolean sitting) {
        super.setOrderedToSit(sitting);
        if (!level().isClientSide) {
            if (sitting && order() != MinionOrder.STAY) {
                entityData.set(DATA_ORDER, MinionOrder.STAY.ordinal());
            } else if (!sitting && order() == MinionOrder.STAY) {
                entityData.set(DATA_ORDER, MinionOrder.FOLLOW.ordinal());
            }
        }
    }

    /** Hostile creatures this Minion hunts: with an aggressive stance, or while guarding or on patrol. */
    private boolean isHuntedMonster(LivingEntity target) {
        if (!(target instanceof net.minecraft.world.entity.monster.Enemy) || target instanceof MinionEntity) {
            return false;
        }
        MinionStance stance = stance();
        MinionOrder order = order();
        if (stance == MinionStance.PASSIVE || stance == MinionStance.DEFENSIVE && order != MinionOrder.GUARD && order != MinionOrder.PATROL) {
            return false;
        }
        if (target.getType().is(NecroTags.MINION_IGNORES)) {
            boolean creeperSafe = hasRangedWeapon() || hasTrait(BodyPartLocation.TORSO, com.sirolf2009.necromancy.trait.Traits.VOLATILE.id());
            return creeperSafe && target instanceof net.minecraft.world.entity.monster.Creeper;
        }
        return guardPost == null || target.blockPosition().closerThan(guardPost, 16);
    }

    public int kills() {
        return entityData.get(DATA_KILLS);
    }

    public int rank() {
        int kills = kills();
        int rank = 0;
        for (int needed : RANK_KILLS) {
            if (kills >= needed) {
                rank++;
            }
        }
        return rank;
    }

    /** Raised again from Remains: keeps its kills and rank. */
    public void restoreKills(int kills) {
        entityData.set(DATA_KILLS, kills);
        applyRank();
    }

    /** Counts a kill (the Minion landed the killing blow on something with at least 10 health) and ranks up. */
    public void addKill(LivingEntity victim) {
        if (victim.getMaxHealth() < 10 || victim == getOwner() || isSpawnling()) {
            return;
        }
        int before = rank();
        entityData.set(DATA_KILLS, kills() + 1);
        if (rank() > before) {
            applyRank();
            playSound(SoundEvents.PLAYER_LEVELUP, 0.6F, 0.6F);
            if (level() instanceof ServerLevel server) {
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, getX(), getY(1.0), getZ(), 20, 0.3, 0.4, 0.3, 0.02);
            }
            if (getOwner() instanceof Player owner && NecroConfig.MINION_GRIEF_MESSAGES.get()) {
                owner.sendSystemMessage(Component.translatable("message.necromancy.minion.rank_up", getDisplayName(),
                        Component.translatable("rank.necromancy." + rank())));
            }
        }
    }

    /** +5% health and damage per rank; the fifth rank (Revenant) also gets a point of armour toughness. */
    private void applyRank() {
        int rank = rank();
        modifier(Attributes.MAX_HEALTH, RANK_MODIFIER, rank * 0.05, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        modifier(Attributes.ATTACK_DAMAGE, RANK_MODIFIER, rank * 0.05, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        modifier(Attributes.ARMOR_TOUGHNESS, RANK_MODIFIER, rank >= 5 ? 1 : 0, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public Component getDisplayName() {
        Component name = super.getDisplayName();
        return rank() >= 5 && name instanceof net.minecraft.network.chat.MutableComponent mutable
                ? mutable.withStyle(net.minecraft.ChatFormatting.DARK_RED) : name;
    }

    /** Bound to a Phylactery: it leaves Remains when it dies instead of scattering its parts. */
    public boolean isBound() {
        return entityData.get(DATA_BOUND);
    }

    public void setBound(boolean bound) {
        entityData.set(DATA_BOUND, bound);
    }

    // --- equipment ---

    public boolean hasArms() {
        return part(BodyPartLocation.ARM_LEFT) != null || part(BodyPartLocation.ARM_RIGHT) != null;
    }

    public boolean hasRangedWeapon() {
        ItemStack held = getMainHandItem();
        return hasArms() && (held.getItem() instanceof net.minecraft.world.item.BowItem
                || held.getItem() instanceof net.minecraft.world.item.TridentItem && hasArmTrait("trident_thrower"));
    }

    private boolean hasArmTrait(String name) {
        ResourceLocation id = Necromancy.id(name);
        return hasTrait(BodyPartLocation.ARM_RIGHT, id) || hasTrait(BodyPartLocation.ARM_LEFT, id);
    }

    /** Ticks between bow shots: skeleton-family arms are quick, others slow (docs/REVIEW.md #1). */
    public int rangedInterval() {
        if (getMainHandItem().getItem() instanceof net.minecraft.world.item.TridentItem) {
            return 40;
        }
        if (hasArmTrait("poison_archer")) {
            return 50;
        }
        return hasArmTrait("archer") ? 20 + random.nextInt(21) : 60;
    }

    @Override
    public void setItemSlot(net.minecraft.world.entity.EquipmentSlot slot, ItemStack stack) {
        super.setItemSlot(slot, stack);
        if (!level().isClientSide && slot != net.minecraft.world.entity.EquipmentSlot.BODY) {
            setGuaranteedDrop(slot);
        }
    }

    @Override
    public boolean canHoldItem(ItemStack stack) {
        return false;
    }

    // --- traits ---

    public void forEachTrait(java.util.function.BiConsumer<BodyPartLocation, com.sirolf2009.necromancy.api.PartTrait> action) {
        for (BodyPartLocation location : BodyPartLocation.values()) {
            NecroMob mob = part(location);
            if (mob != null) {
                for (com.sirolf2009.necromancy.api.PartTrait trait : mob.traits(location.type())) {
                    action.accept(location, trait);
                }
            }
        }
    }

    /** True if any part has the flag. */
    public boolean hasFlag(com.sirolf2009.necromancy.api.TraitFlag flag) {
        for (BodyPartLocation location : BodyPartLocation.values()) {
            if (hasFlag(location, flag)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasFlag(BodyPartLocation location, com.sirolf2009.necromancy.api.TraitFlag flag) {
        NecroMob mob = part(location);
        if (mob == null) {
            return false;
        }
        for (com.sirolf2009.necromancy.api.PartTrait trait : mob.traits(location.type())) {
            if (trait.has(flag)) {
                return true;
            }
        }
        return false;
    }

    /** Struck by lightning: a Creeper torso then explodes twice as hard and glows with the charged creeper's aura. */
    public boolean isPowered() {
        return entityData.get(DATA_POWERED);
    }

    @Override
    public void thunderHit(ServerLevel level, net.minecraft.world.entity.LightningBolt lightning) {
        super.thunderHit(level, lightning);
        if (hasTrait(BodyPartLocation.TORSO, com.sirolf2009.necromancy.trait.Traits.VOLATILE.id())) {
            entityData.set(DATA_POWERED, true);
            clearFire();
        }
    }

    public boolean hasTrait(BodyPartLocation location, ResourceLocation traitId) {
        NecroMob mob = part(location);
        return mob != null && mob.traits(location.type()).stream().anyMatch(t -> t.id().equals(traitId));
    }

    private final Map<String, Integer> cooldowns = new java.util.HashMap<>();
    private final Map<String, double[]> traitState = new java.util.HashMap<>();
    private int lastActiveTick;
    @Nullable
    private BlockPos lightPos;
    @Nullable
    private UUID spawnlingOf;
    private int spawnlingLife;
    private int pendingJump;

    /** Every trait timer ready again (tests). */
    public void clearCooldowns() {
        cooldowns.clear();
    }

    /** Starts the named cooldown now, whether or not it has run out. */
    public void setCooldown(String key, int ticks) {
        cooldowns.put(key, tickCount + ticks);
    }

    /** Scratch numbers a trait keeps on this Minion (not saved), made with the given length on first use. */
    public double[] traitState(String key, int length) {
        return traitState.computeIfAbsent(key, k -> new double[length]);
    }

    /** Ticks since the Minion last moved or swung. */
    public int idleTicks() {
        return tickCount - lastActiveTick;
    }

    /** In the open sun, where an undead mob would burn. */
    public boolean isInSunlight() {
        return isSunBurnTick();
    }

    /** Never to be harmed by this Minion's traits: itself, its master, its master's Minions and pets, and players not set as enemies. */
    public boolean isFriendly(Entity entity) {
        if (entity == this) {
            return true;
        }
        LivingEntity owner = getOwner();
        if (owner == null) {
            return entity instanceof MinionEntity other && other.getOwnerUUID() == null;
        }
        if (entity == owner) {
            return true;
        }
        if (entity instanceof Player player) {
            return !isEnemyListed(player);
        }
        return entity instanceof LivingEntity living && !wantsToAttack(living, owner);
    }

    // --- the light a glowing part keeps at its eyes ---

    @Nullable
    public BlockPos lightPos() {
        return lightPos;
    }

    public void setLightPos(@Nullable BlockPos pos) {
        lightPos = pos;
    }

    public void clearLight() {
        if (lightPos != null && !level().isClientSide && level().isLoaded(lightPos) && level().getBlockState(lightPos).is(net.minecraft.world.level.block.Blocks.LIGHT)) {
            level().setBlock(lightPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
        lightPos = null;
    }

    @Override
    public void remove(RemovalReason reason) {
        clearLight();
        super.remove(reason);
    }

    // --- spawnlings: small temporary Minions a summoning part calls up ---

    public boolean isSpawnling() {
        return spawnlingOf != null;
    }

    /** The living spawnlings this Minion called up. */
    public java.util.List<MinionEntity> spawnlings() {
        return level().getEntitiesOfClass(MinionEntity.class, getBoundingBox().inflate(32), m -> getUUID().equals(m.spawnlingOf) && m.isAlive());
    }

    /**
     * A spawnling of {@code source}: every part that creature has, the summoner's master and target, Aggressive, shrunk by
     * {@code scale}. It never drops anything or gains rank, and crumbles after {@code lifetime} ticks or when its summoner dies.
     */
    public static void spawnling(MinionEntity summoner, NecroMob source, int lifetime, double scale) {
        if (!(summoner.level() instanceof ServerLevel level)) {
            return;
        }
        Map<BodyPartLocation, ResourceLocation> parts = new EnumMap<>(BodyPartLocation.class);
        for (BodyPartLocation location : BodyPartLocation.values()) {
            if (source.hasPart(location.type())) {
                parts.put(location, source.id());
            }
        }
        if (!parts.containsKey(BodyPartLocation.HEAD)) {
            return;
        }
        MinionEntity spawnling = com.sirolf2009.necromancy.registry.NecroEntities.MINION.get().create(level);
        if (spawnling == null) {
            return;
        }
        spawnling.setParts(parts);
        double x = summoner.getX() + (summoner.getRandom().nextDouble() - 0.5) * 2;
        double z = summoner.getZ() + (summoner.getRandom().nextDouble() - 0.5) * 2;
        spawnling.moveTo(x, summoner.getY(), z, summoner.getRandom().nextFloat() * 360F, 0F);
        if (summoner.getOwnerUUID() != null) {
            spawnling.setTame(true, false);
            spawnling.setOwnerUUID(summoner.getOwnerUUID());
        }
        spawnling.setStance(MinionStance.AGGRESSIVE);
        spawnling.spawnlingOf = summoner.getUUID();
        spawnling.spawnlingLife = lifetime;
        spawnling.applyPartAttributes(true);
        spawnling.modifier(Attributes.SCALE, Necromancy.id("spawnling"), scale, AttributeModifier.Operation.ADD_VALUE);
        spawnling.setTarget(summoner.getTarget());
        level.addFreshEntity(spawnling);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, x, summoner.getY() + 0.3, z, 8, 0.2, 0.3, 0.2, 0.03);
    }

    private void crumble() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, getX(), getY(0.5), getZ(), 10, 0.25, 0.3, 0.25, 0.03);
        }
        discard();
    }

    @Override
    protected boolean shouldDropLoot() {
        return !isSpawnling() && super.shouldDropLoot();
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        if (isCoolingDown("leap_landing")) {
            return false;
        }
        return super.causeFallDamage(fallDistance, multiplier, source);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (hasFlag(com.sirolf2009.necromancy.api.TraitFlag.CACTUS_PROOF) && (source.is(net.minecraft.world.damagesource.DamageTypes.CACTUS)
                || source.is(net.minecraft.world.damagesource.DamageTypes.SWEET_BERRY_BUSH))) {
            return true;
        }
        if (hasFlag(com.sirolf2009.necromancy.api.TraitFlag.IN_WALL_PROOF) && source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) {
        if (hasFlag(BodyPartLocation.HEAD, com.sirolf2009.necromancy.api.TraitFlag.BREATHES_WATER) || hasFlag(BodyPartLocation.TORSO, com.sirolf2009.necromancy.api.TraitFlag.BREATHES_WATER)) {
            return false;
        }
        return super.canDrownInFluidType(type);
    }

    /** True (and restarts the timer) when the named cooldown has run out. Per Minion, not saved. */
    public boolean cooldown(String key, int ticks) {
        Integer until = cooldowns.get(key);
        if (until != null && until > tickCount) {
            return false;
        }
        cooldowns.put(key, tickCount + ticks);
        return true;
    }

    public boolean isCoolingDown(String key) {
        Integer until = cooldowns.get(key);
        return until != null && until > tickCount;
    }

    @Override
    public boolean onClimbable() {
        return (horizontalCollision && hasFlag(com.sirolf2009.necromancy.api.TraitFlag.CLIMBS)) || super.onClimbable();
    }

    @Override
    public boolean fireImmune() {
        return super.fireImmune() || hasFlag(BodyPartLocation.TORSO, com.sirolf2009.necromancy.api.TraitFlag.FIRE_PROOF)
                && (part(BodyPartLocation.LEGS) == null || hasFlag(BodyPartLocation.LEGS, com.sirolf2009.necromancy.api.TraitFlag.FIRE_PROOF));
    }

    @Override
    public boolean isSensitiveToWater() {
        return hasFlag(com.sirolf2009.necromancy.api.TraitFlag.HURT_BY_WATER);
    }

    private boolean burnsInSun() {
        return hasFlag(com.sirolf2009.necromancy.api.TraitFlag.BURNS_IN_SUN) && !hasFlag(BodyPartLocation.HEAD, com.sirolf2009.necromancy.api.TraitFlag.SUN_PROOF)
                && getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty();
    }

    @Override
    public void aiStep() {
        if (isAlive() && !level().isClientSide && burnsInSun() && isSunBurnTick()) {
            igniteForSeconds(8);
        }
        super.aiStep();
        com.sirolf2009.necromancy.trait.DataTraits.glide(this);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        forEachTrait((location, trait) -> trait.tick(this, location));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && !isInvulnerableTo(source)) {
            float[] box = { amount };
            forEachTrait((location, trait) -> box[0] = trait.onHurt(this, location, source, box[0]));
            amount = box[0];
            if (amount <= 0) {
                return false;
            }
            // a Legendary soul refuses to leave: the first lethal blow every ten minutes leaves it at one health
            if (amount >= getHealth() && soul.potency() == com.sirolf2009.necromancy.soul.SoulPotency.LEGENDARY
                    && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && cooldown("undying", 12000)) {
                setHealth(1F);
                addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 200, 1));
                level().broadcastEntityEvent(this, (byte) 35);
                return true;
            }
        }
        return super.hurt(source, amount);
    }

    /** A player the master told Minions to fight (an enemy, or anyone not a friend when set to aggressive). */
    public boolean isEnemyListed(LivingEntity target) {
        return isOrderedTarget(target);
    }

    private boolean isOrderedTarget(LivingEntity target) {
        if (!(target instanceof Player player) || !(getOwner() instanceof Player owner) || player == owner || player.isCreative() || player.isSpectator()) {
            return false;
        }
        NecroAttachments.NecromancerData orders = owner.getData(NecroAttachments.NECROMANCER);
        UUID id = player.getUUID();
        return orders.isEnemy(id) || orders.aggressive() && !orders.isFriend(id);
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (target instanceof MinionEntity other && other.getOwner() == owner) {
            return false;
        }
        if (target instanceof TamableAnimal tame && tame.isTame() && tame.getOwner() == owner) {
            return false;
        }
        if (target instanceof Player player && owner instanceof Player ownerPlayer) {
            if (!ownerPlayer.canHarmPlayer(player)) {
                return false;
            }
            return !owner.getData(NecroAttachments.NECROMANCER).isFriend(player.getUUID());
        }
        return !(target instanceof OwnableEntity ownable && ownable.getOwner() == owner);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        attackAnimation = 10;
        level().broadcastEntityEvent(this, (byte) 4);
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            forEachTrait((location, trait) -> trait.onAttack(this, location, living));
        }
        return hit;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            attackAnimation = 10;
        } else {
            super.handleEntityEvent(id);
        }
    }

    public int getAttackAnimation() {
        return attackAnimation;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        ItemStack held = getMainHandItem();
        if (held.getItem() instanceof net.minecraft.world.item.BowItem && hasArms()) {
            ItemStack ammo = getProjectile(held);
            net.minecraft.world.entity.projectile.AbstractArrow arrow = net.minecraft.world.entity.projectile.ProjectileUtil.getMobArrow(this, ammo, velocity, held);
            if (hasArmTrait("poison_archer") && arrow instanceof net.minecraft.world.entity.projectile.Arrow tipped) {
                tipped.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 80));
            }
            double dx = target.getX() - getX();
            double dy = target.getY(0.3333) - arrow.getY();
            double dz = target.getZ() - getZ();
            arrow.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.2, dz, 1.6F, 14 - level().getDifficulty().getId() * 4);
            arrow.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;
            playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
            level().addFreshEntity(arrow);
            return;
        }
        if (held.getItem() instanceof net.minecraft.world.item.TridentItem) {
            net.minecraft.world.entity.projectile.ThrownTrident trident = new net.minecraft.world.entity.projectile.ThrownTrident(level(), this, held.copyWithCount(1));
            double dx = target.getX() - getX();
            double dy = target.getY(0.3333) - trident.getY();
            double dz = target.getZ() - getZ();
            trident.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.2, dz, 1.6F, 14 - level().getDifficulty().getId() * 4);
            trident.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;
            playSound(SoundEvents.DROWNED_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
            level().addFreshEntity(trident);
            return;
        }
        playSound(NecroSounds.TEAR.get(), 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
        TearEntity tear = random.nextInt(5) == 0 ? new BloodTearEntity(level(), this) : new TearEntity(level(), this);
        tear.aimAt(this, target);
        level().addFreshEntity(tear);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level().isClientSide) {
            boolean mine = isOwnedBy(player);
            return mine || stack.is(Items.SADDLE) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        if (!isOwnedBy(player) && isSaddled() && getOwner() != null && getFirstPassenger() == getOwner() && canAddPassenger(player)
                && !player.isSecondaryUseActive()) {
            player.startRiding(this);
            return InteractionResult.SUCCESS;
        }
        if (!isOwnedBy(player)) {
            if (NecroConfig.MINION_GRIEF_MESSAGES.get() && getOwner() != null) {
                player.sendSystemMessage(Component.translatable("message.necromancy.minion.obey_only", getDisplayName(), getOwner().getDisplayName()));
            }
            return InteractionResult.PASS;
        }
        if (player.isSecondaryUseActive() && stack.isEmpty() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inventory, p) -> new com.sirolf2009.necromancy.menu.MinionMenu(id, inventory, this), getDisplayName()),
                    buf -> buf.writeVarInt(getId()));
            return InteractionResult.SUCCESS;
        }
        if (stack.is(NecroItems.JAR_OF_BLOOD.get()) && getHealth() < getMaxHealth()) {
            heal(8F);
            player.setItemInHand(hand, net.minecraft.world.item.ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
            playSound(SoundEvents.GENERIC_DRINK, 1F, 0.8F);
            return InteractionResult.SUCCESS;
        }
        // mend with flesh
        if (isHealingFood(stack) && getHealth() < getMaxHealth()) {
            heal(stack.is(Items.ROTTEN_FLESH) ? 2F : 6F);
            stack.consume(1, player);
            playSound(SoundEvents.GENERIC_EAT, 1F, 0.8F);
            ((ServerLevel) level()).sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, getX(), getY() + getBbHeight(), getZ(), 3, 0.3, 0.2, 0.3, 0);
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.SADDLE) && isSaddleable() && !isSaddled()) {
            equipSaddle(stack, SoundSource.NEUTRAL);
            stack.consume(1, player);
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.SHEARS) && isSaddled() && !isVehicle()) {
            steering.setSaddle(false);
            spawnAtLocation(Items.SADDLE);
            playSound(SoundEvents.SHEEP_SHEAR, 1F, 1F);
            return InteractionResult.SUCCESS;
        }
        for (BodyPartLocation location : BodyPartLocation.values()) {
            NecroMob mob = part(location);
            if (mob == null || location == BodyPartLocation.ARM_LEFT && part(BodyPartLocation.ARM_RIGHT) == mob) {
                continue;
            }
            for (com.sirolf2009.necromancy.api.PartTrait trait : mob.traits(location.type())) {
                InteractionResult result = trait.onInteract(this, location, player, hand);
                if (result.consumesAction()) {
                    return result;
                }
            }
        }
        if (stack.is(NecroItems.BONE_NEEDLE.get()) || stack.is(Items.NAME_TAG) || stack.is(Items.LEAD)) {
            return InteractionResult.PASS;
        }
        if (isSaddled() && !isVehicle() && !player.isSecondaryUseActive()) {
            player.startRiding(this);
            return InteractionResult.SUCCESS;
        }
        setOrderedToSit(!isOrderedToSit());
        jumping = false;
        navigation.stop();
        setTarget(null);
        if (NecroConfig.MINION_GRIEF_MESSAGES.get()) {
            player.displayClientMessage(Component.translatable(isOrderedToSit() ? "message.necromancy.minion.staying" : "message.necromancy.minion.following",
                    getDisplayName()), true);
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean isHealingFood(ItemStack stack) {
        return stack.is(Items.ROTTEN_FLESH) || stack.is(NecroTags.ORGANS);
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            forEachTrait((location, trait) -> trait.onDeath(this, location, source));
            if (!isSpawnling()) {
                dropRemains();
            }
        }
        if (!level().isClientSide && level().getServer() != null && !isSpawnling()) {
            UUID owner = getOwnerUUID();
            if (owner != null) {
                MinionCensus.get(level().getServer()).remove(owner);
            }
        }
        super.die(source);
    }

    /**
     * A Minion bound to a Phylactery leaves Minion Remains to raise it again. Any other Minion scatters: each part has a
     * one in four chance to drop as its item (docs/REVIEW.md #7).
     */
    protected void dropRemains() {
        if (isBound()) {
            ItemStack remains = new ItemStack(NecroItems.MINION_REMAINS.get());
            remains.set(com.sirolf2009.necromancy.registry.NecroComponents.MINION_BUILD.get(),
                    com.sirolf2009.necromancy.soul.MinionBuild.of(parts(), soul, java.util.Optional.ofNullable(getCustomName()), kills()));
            spawnAtLocation(remains);
            return;
        }
        for (BodyPartLocation location : BodyPartLocation.values()) {
            NecroMob mob = part(location);
            if (mob != null && random.nextFloat() < 0.25F) {
                ItemStack part = mob.partStack(location.type());
                if (!part.isEmpty()) {
                    spawnAtLocation(part);
                }
            }
        }
        if (level() instanceof ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, getX(), getY(0.6), getZ(), 12, 0.3, 0.4, 0.3, 0.05);
        }
    }

    @Override
    protected void dropEquipment() {
        if (isSpawnling()) {
            return;
        }
        super.dropEquipment();
        for (ItemStack stack : pack.removeAllItems()) {
            spawnAtLocation(stack);
        }
        if (isSaddled()) {
            spawnAtLocation(Items.SADDLE);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // --- sounds come from the head ---

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        NecroMob head = part(BodyPartLocation.HEAD);
        return head == null ? SoundEvents.ZOMBIE_AMBIENT : head.ambientSound();
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        NecroMob head = part(BodyPartLocation.HEAD);
        return head == null ? SoundEvents.ZOMBIE_HURT : head.hurtSound();
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        NecroMob head = part(BodyPartLocation.HEAD);
        return head == null ? SoundEvents.ZOMBIE_DEATH : head.deathSound();
    }

    // --- saddle and steering (as the pig) ---

    @Override
    public boolean isSaddleable() {
        NecroMob torso = part(BodyPartLocation.TORSO);
        return isAlive() && isTame() && torso != null && torso.isSaddleable();
    }

    @Override
    public void equipSaddle(ItemStack stack, @Nullable SoundSource source) {
        steering.setSaddle(true);
        if (source != null) {
            level().playSound(null, this, SoundEvents.PIG_SADDLE, source, 0.5F, 1.0F);
        }
    }

    @Override
    public boolean isSaddled() {
        return steering.hasSaddle();
    }

    public void removeSaddle() {
        steering.setSaddle(false);
    }

    @Override
    public boolean boost() {
        return steering.boost(getRandom());
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        if (isSaddled() && getFirstPassenger() instanceof Player player && isOwnedBy(player)
                && (mountSpec().direct() || player.isHolding(NecroItems.BRAIN_ON_A_STICK.get()))) {
            return player;
        }
        return super.getControllingPassenger();
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        setRot(player.getYRot(), player.getXRot() * 0.5F);
        yRotO = yBodyRot = yHeadRot = getYRot();
        steering.tickBoost();
        NecroMob.Mount mount = mountSpec();
        if (mount.water() && isInWater()) {
            // swim where the rider looks
            double up = -Mth.sin(player.getXRot() * Mth.DEG_TO_RAD) * 0.06 * (player.zza > 0 ? 1 : 0);
            setDeltaMovement(getDeltaMovement().add(0, up + 0.005, 0));
        }
        if (pendingJump > 0 && onGround()) {
            float strength = mount.jump() * Math.min(1F, pendingJump / 90F);
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x, 0.42 + 0.6 * strength, motion.z);
            hasImpulse = true;
            pendingJump = 0;
        }
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        if (mountSpec().direct()) {
            float forward = player.zza <= 0 ? player.zza * 0.25F : player.zza;
            return new Vec3(player.xxa * 0.5F, 0.0, forward);
        }
        return new Vec3(0.0, 0.0, 1.0);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        NecroMob.Mount mount = mountSpec();
        double speed = getAttributeValue(Attributes.MOVEMENT_SPEED) * (1 + mount.speedBonus());
        if (mount.water()) {
            speed *= isInWater() ? 1.6 : 0.3;
        }
        return (float) (mount.direct() ? speed : speed * 0.9 * steering.boostFactor());
    }

    /** How the torso rides (the pig's way unless its mount trait says otherwise). */
    public NecroMob.Mount mountSpec() {
        NecroMob torso = part(BodyPartLocation.TORSO);
        return torso == null ? NecroMob.Mount.PIG : torso.mount();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < mountSpec().seats();
    }

    @Override
    public boolean canJump() {
        return isSaddled() && mountSpec().jump() > 0;
    }

    @Override
    public void onPlayerJump(int power) {
        if (canJump() && power > 0) {
            pendingJump = power;
        }
    }

    @Override
    public void handleStartJump(int power) {
    }

    @Override
    public void handleStopJump() {
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        NecroMob torso = part(BodyPartLocation.TORSO);
        float offset = torso == null ? 0 : torso.riderOffset();
        // a second seat sits behind the first
        float back = getPassengers().indexOf(passenger) > 0 ? -Math.max(0.5F, dimensions.width() * 0.45F) : 0F;
        return new Vec3(0, dimensions.height() * 0.62F + offset, back);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        for (var direction : new net.minecraft.core.Direction[] { getMotionDirection().getClockWise(), getMotionDirection().getCounterClockWise() }) {
            BlockPos pos = blockPosition().relative(direction);
            double floor = level().getBlockFloorHeight(pos);
            if (DismountHelper.isBlockFloorValid(floor)) {
                Vec3 spot = Vec3.upFromBottomCenterOf(pos, floor);
                AABB box = passenger.getLocalBoundsForPose(Pose.STANDING).move(spot);
                if (DismountHelper.canDismountTo(level(), passenger, box)) {
                    return spot;
                }
            }
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    // --- not an animal that breeds ---

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canMate(net.minecraft.world.entity.animal.Animal other) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public String toString() {
        return String.format("Minion[%s, owner=%s, parts=%s]", getUUID(), getOwnerUUID(), parts());
    }
}
