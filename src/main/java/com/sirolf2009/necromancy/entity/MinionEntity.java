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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
public class MinionEntity extends TamableAnimal implements RangedAttackMob, ItemSteerable, Saddleable {
    private static final Map<BodyPartLocation, EntityDataAccessor<String>> DATA_PARTS = new EnumMap<>(BodyPartLocation.class);
    private static final EntityDataAccessor<Boolean> DATA_SADDLE = SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_BOOST_TIME = SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_POWERED = SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.BOOLEAN);

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
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this) {
            @Override
            public boolean canUse() {
                return !hasFlag(com.sirolf2009.necromancy.api.TraitFlag.SINKS) && super.canUse();
            }
        });
        goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(4, new LeapAtTargetGoal(this, 0.4F));
        goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0, 10.0F, 2.0F));
        goalSelector.addGoal(7, new TemptGoal(this, 1.0, stack -> stack.is(NecroItems.BRAIN_ON_A_STICK.get()), false));
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isOrderedTarget));
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
        steering.addAdditionalSaveData(tag);
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
        steering.readAdditionalSaveData(tag);
    }

    // --- behaviour ---

    @Override
    public void tick() {
        super.tick();
        if (attackAnimation > 0) {
            attackAnimation--;
        }
        if (!level().isClientSide && tickCount > 20 && part(BodyPartLocation.HEAD) == null) {
            // a body without a head does not hold together (as in the original)
            hurt(damageSources().generic(), Float.MAX_VALUE);
        }
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
                && hasFlag(BodyPartLocation.LEGS, com.sirolf2009.necromancy.api.TraitFlag.FIRE_PROOF);
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
        }
        return super.hurt(source, amount);
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
        if (!isOwnedBy(player)) {
            if (NecroConfig.MINION_GRIEF_MESSAGES.get() && getOwner() != null) {
                player.sendSystemMessage(Component.translatable("message.necromancy.minion.obey_only", getDisplayName(), getOwner().getDisplayName()));
            }
            return InteractionResult.PASS;
        }
        // mend with flesh
        if (isHealingFood(stack) && getHealth() < getMaxHealth()) {
            heal(stack.is(Items.ROTTEN_FLESH) ? 4F : 8F);
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
        }
        if (!level().isClientSide && level().getServer() != null) {
            UUID owner = getOwnerUUID();
            if (owner != null) {
                MinionCensus.get(level().getServer()).remove(owner);
            }
        }
        super.die(source);
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
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

    @Override
    public boolean boost() {
        return steering.boost(getRandom());
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        if (isSaddled() && getFirstPassenger() instanceof Player player && isOwnedBy(player)
                && (player.isHolding(NecroItems.BRAIN_ON_A_STICK.get()))) {
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
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        return new Vec3(0.0, 0.0, 1.0);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) (getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.9 * steering.boostFactor());
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        NecroMob torso = part(BodyPartLocation.TORSO);
        float offset = torso == null ? 0 : torso.riderOffset();
        return new Vec3(0, dimensions.height() * 0.62F + offset, 0);
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
