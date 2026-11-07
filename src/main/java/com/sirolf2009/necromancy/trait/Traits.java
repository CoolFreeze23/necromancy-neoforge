package com.sirolf2009.necromancy.trait;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.NecroConfig;
import com.sirolf2009.necromancy.api.BodyPartLocation;
import com.sirolf2009.necromancy.api.PartTrait;
import com.sirolf2009.necromancy.api.TraitFlag;
import com.sirolf2009.necromancy.entity.BloodTearEntity;
import com.sirolf2009.necromancy.entity.MinionEntity;
import com.sirolf2009.necromancy.entity.TearEntity;
import com.sirolf2009.necromancy.registry.NecroSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Every part trait in the mod. See docs/REVIEW.md #1, #4 and #13 for the design. */
public final class Traits {
    private Traits() {
    }

    // --- flags only ---

    public static final PartTrait CLIMBER = flag("climber", TraitFlag.CLIMBS);
    public static final PartTrait SUNBURN = flag("sunburn", TraitFlag.BURNS_IN_SUN);
    public static final PartTrait SUN_PROOF = flag("sun_proof", TraitFlag.SUN_PROOF);
    public static final PartTrait WATER_HURTS = flag("water_hurts", TraitFlag.HURT_BY_WATER);
    public static final PartTrait FIRE_PROOF = flag("fire_proof", TraitFlag.FIRE_PROOF);
    public static final PartTrait THINKER = flag("thinker", TraitFlag.THINKS);
    public static final PartTrait HEAVY = flag("heavy", TraitFlag.SINKS);

    // --- on hit ---

    public static final PartTrait VENOM = onHit("venom", 1.0F, (m, t) -> t.addEffect(new MobEffectInstance(MobEffects.POISON, 140, 0), m));
    public static final PartTrait HUNGER_BITE = onHit("hunger_bite", 0.15F, (m, t) -> t.addEffect(new MobEffectInstance(MobEffects.HUNGER, 140, 0), m));
    public static final PartTrait HUSK_BITE = onHit("husk_bite", 1.0F, (m, t) -> t.addEffect(new MobEffectInstance(MobEffects.HUNGER, 140, 0), m));
    public static final PartTrait WITHERING = onHit("withering", 1.0F, (m, t) -> t.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 0), m));
    public static final PartTrait SHADOW_TOUCH = onHit("shadow_touch", 1.0F, (m, t) -> t.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), m));

    /** Iron Golem arms throw what they hit into the air. */
    public static final PartTrait GOLEM_TOSS = new Simple("golem_toss") {
        @Override
        public void onAttack(MinionEntity minion, BodyPartLocation location, LivingEntity target) {
            if (location == BodyPartLocation.ARM_RIGHT || !minion.hasTrait(BodyPartLocation.ARM_RIGHT, id())) {
                target.setDeltaMovement(target.getDeltaMovement().add(0, 0.4, 0));
                minion.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1F, 1F);
            }
        }
    };

    /** Wolf head: +1 damage for each other wolf-headed Minion of the same master within 8 blocks, up to +3. */
    public static final PartTrait PACK_HUNTER = new Simple("pack_hunter") {
        @Override
        public void onAttack(MinionEntity minion, BodyPartLocation location, LivingEntity target) {
            List<MinionEntity> pack = minion.level().getEntitiesOfClass(MinionEntity.class, minion.getBoundingBox().inflate(8),
                    other -> other != minion && other.getOwnerUUID() != null && other.getOwnerUUID().equals(minion.getOwnerUUID())
                            && other.hasTrait(BodyPartLocation.HEAD, id()));
            int bonus = Math.min(3, pack.size());
            if (bonus > 0) {
                target.invulnerableTime = 0;
                target.hurt(minion.damageSources().mobAttack(minion), bonus);
            }
        }
    };

    /** Zombified Piglin arms: +2 damage while holding a golden sword or axe (per Minion). */
    public static final PartTrait GOLD_FIGHTER = new Simple("gold_fighter") {
        @Override
        public void onAttack(MinionEntity minion, BodyPartLocation location, LivingEntity target) {
            ItemStack held = minion.getMainHandItem();
            boolean gold = held.is(Items.GOLDEN_SWORD) || held.is(Items.GOLDEN_AXE);
            if (gold && (location == BodyPartLocation.ARM_RIGHT || !minion.hasTrait(BodyPartLocation.ARM_RIGHT, id()))) {
                target.invulnerableTime = 0;
                target.hurt(minion.damageSources().mobAttack(minion), 2F);
            }
        }
    };

    /** Goat head: a charge. The first hit after a cooldown deals half again and knocks the target far back. */
    public static final PartTrait RAM = new Simple("ram") {
        @Override
        public void onAttack(MinionEntity minion, BodyPartLocation location, LivingEntity target) {
            if (minion.cooldown("ram", 100)) {
                Vec3 push = target.position().subtract(minion.position()).normalize().scale(1.2);
                target.push(push.x, 0.35, push.z);
                target.invulnerableTime = 0;
                target.hurt(minion.damageSources().mobAttack(minion), (float) minion.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 0.5F);
                minion.playSound(SoundEvents.GOAT_RAM_IMPACT, 1F, 1F);
            }
        }
    };

    // --- when hurt ---

    /** Squid torso: 25% chance to blind the attacker for 2 seconds. */
    public static final PartTrait INK_CLOUD = new Simple("ink_cloud") {
        @Override
        public float onHurt(MinionEntity minion, BodyPartLocation location, DamageSource source, float amount) {
            if (source.getEntity() instanceof LivingEntity attacker && attacker != minion && minion.getRandom().nextFloat() < 0.25F) {
                attacker.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), minion);
                if (minion.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.SQUID_INK, minion.getX(), minion.getY(0.6), minion.getZ(), 20, 0.4, 0.4, 0.4, 0.05);
                }
            }
            return amount;
        }
    };

    /** Slime torso: 30% chance to bounce a melee attacker away. */
    public static final PartTrait BOUNCY = new Simple("bouncy") {
        @Override
        public float onHurt(MinionEntity minion, BodyPartLocation location, DamageSource source, float amount) {
            if (source.getDirectEntity() instanceof LivingEntity attacker && attacker != minion && minion.getRandom().nextFloat() < 0.3F) {
                attacker.knockback(1.0, minion.getX() - attacker.getX(), minion.getZ() - attacker.getZ());
                minion.playSound(SoundEvents.SLIME_JUMP, 1F, 1F);
            }
            return amount;
        }
    };

    /** Armadillo torso: below 30% health it rolls up (Resistance III for 5 seconds), once a minute. */
    public static final PartTrait ROLL_UP = new Simple("roll_up") {
        @Override
        public float onHurt(MinionEntity minion, BodyPartLocation location, DamageSource source, float amount) {
            if (minion.getHealth() - amount < minion.getMaxHealth() * 0.3F && minion.cooldown("roll_up", 1200)) {
                minion.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 2));
                minion.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4));
                minion.playSound(SoundEvents.ARMADILLO_ROLL, 1F, 1F);
                return amount * 0.5F;
            }
            return amount;
        }
    };

    // --- on death ---

    /** Creeper torso: explodes when the Minion dies. Breaks blocks only with mobGriefing and the config switch. */
    public static final PartTrait VOLATILE = new Simple("volatile") {
        @Override
        public void onDeath(MinionEntity minion, BodyPartLocation location, DamageSource source) {
            Level level = minion.level();
            boolean griefing = NecroConfig.CREEPER_TORSO_BREAKS_BLOCKS.get() && level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
            float power = minion.isPowered() ? 4F : 2F;
            level.explode(minion, minion.getX(), minion.getY(), minion.getZ(), power,
                    griefing ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
        }
    };

    // --- every tick ---

    /** Isaac's head: a tear at the target every 3 seconds, one in five a blood tear (original values). */
    public static final PartTrait TEARS = new Simple("tears") {
        @Override
        public void tick(MinionEntity minion, BodyPartLocation location) {
            LivingEntity target = minion.getTarget();
            if (target != null && target.isAlive() && !minion.isOrderedToSit() && minion.distanceToSqr(target) < 256
                    && minion.getSensing().hasLineOfSight(target) && minion.cooldown("tears", 60)) {
                minion.playSound(NecroSounds.TEAR.get(), 1.0F, 1.0F / (minion.getRandom().nextFloat() * 0.4F + 0.8F));
                TearEntity tear = minion.getRandom().nextInt(5) == 0 ? new BloodTearEntity(minion.level(), minion) : new TearEntity(minion.level(), minion);
                tear.aimAt(minion, target);
                minion.level().addFreshEntity(tear);
            }
        }
    };

    /** Enderman head: blinks next to a target more than 6 blocks away, every 5 seconds. */
    public static final PartTrait BLINK = new Simple("blink") {
        @Override
        public void tick(MinionEntity minion, BodyPartLocation location) {
            LivingEntity target = minion.getTarget();
            if (target == null || !target.isAlive() || minion.isOrderedToSit() || minion.distanceToSqr(target) < 36 || minion.isCoolingDown("blink")) {
                return;
            }
            for (int i = 0; i < 16; i++) {
                double x = target.getX() + (minion.getRandom().nextDouble() - 0.5) * 4;
                double z = target.getZ() + (minion.getRandom().nextDouble() - 0.5) * 4;
                double y = target.getY() + minion.getRandom().nextInt(3) - 1;
                Vec3 from = minion.position();
                if (minion.randomTeleport(x, y, z, true)) {
                    minion.cooldown("blink", 100);
                    minion.level().playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1F, 1F);
                    minion.playSound(SoundEvents.ENDERMAN_TELEPORT, 1F, 1F);
                    return;
                }
            }
            minion.cooldown("blink", 40);
        }
    };

    /** Blaze head: three small fireballs at a target 4 to 16 blocks away, every 5 seconds. */
    public static final PartTrait FIREBALLS = new Simple("fireballs") {
        @Override
        public void tick(MinionEntity minion, BodyPartLocation location) {
            LivingEntity target = minion.getTarget();
            if (target == null || !target.isAlive() || minion.isOrderedToSit()) {
                return;
            }
            double d = minion.distanceToSqr(target);
            if (d < 16 || d > 256 || !minion.getSensing().hasLineOfSight(target) || !minion.cooldown("fireballs", 100)) {
                return;
            }
            double spread = Math.sqrt(Math.sqrt(d)) * 0.5;
            for (int i = 0; i < 3; i++) {
                Vec3 dir = new Vec3(minion.getRandom().triangle(target.getX() - minion.getX(), 2.297 * spread), target.getY(0.5) - minion.getY(0.5),
                        minion.getRandom().triangle(target.getZ() - minion.getZ(), 2.297 * spread));
                SmallFireball fireball = new SmallFireball(minion.level(), minion, dir.normalize());
                fireball.setPos(fireball.getX(), minion.getY(0.5) + 0.5, fireball.getZ());
                minion.level().addFreshEntity(fireball);
            }
            minion.level().levelEvent(null, 1018, minion.blockPosition(), 0);
        }
    };

    /** Snow Golem head: a snowball every second at targets within 10 blocks (pulls aggro, hurts blazes). */
    public static final PartTrait SNOWBALLS = new Simple("snowballs") {
        @Override
        public void tick(MinionEntity minion, BodyPartLocation location) {
            LivingEntity target = minion.getTarget();
            if (target == null || !target.isAlive() || minion.isOrderedToSit() || minion.distanceToSqr(target) > 100
                    || !minion.getSensing().hasLineOfSight(target) || !minion.cooldown("snowballs", 20)) {
                return;
            }
            Snowball snowball = new Snowball(minion.level(), minion);
            double dx = target.getX() - minion.getX();
            double dy = target.getEyeY() - 1.1 - snowball.getY();
            double dz = target.getZ() - minion.getZ();
            snowball.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.2, dz, 1.6F, 12F);
            minion.playSound(SoundEvents.SNOW_GOLEM_SHOOT, 1F, 0.4F / (minion.getRandom().nextFloat() * 0.4F + 0.8F));
            minion.level().addFreshEntity(snowball);
        }
    };

    /** Snow Golem body: melts in hot biomes. */
    public static final PartTrait MELTS = new Simple("melts") {
        @Override
        public void tick(MinionEntity minion, BodyPartLocation location) {
            if (minion.tickCount % 20 == 0 && minion.level().getBiome(minion.blockPosition()).value().getBaseTemperature() > 1.0F) {
                minion.hurt(minion.damageSources().onFire(), 1F);
            }
        }

        @Override
        public boolean has(TraitFlag flag) {
            return flag == TraitFlag.HURT_BY_WATER;
        }
    };

    /** Witch arms: a splash potion every 6 seconds (Slowness far away, else Weakness or Poison). Never Harming. */
    public static final PartTrait POTION_THROWER = new Simple("potion_thrower") {
        @Override
        public void tick(MinionEntity minion, BodyPartLocation location) {
            LivingEntity target = minion.getTarget();
            if (target == null || !target.isAlive() || minion.isOrderedToSit() || minion.distanceToSqr(target) > 256
                    || !minion.getSensing().hasLineOfSight(target) || !minion.cooldown("witch_potion", 120)) {
                return;
            }
            double distance = Math.sqrt(minion.distanceToSqr(target));
            var potion = distance > 8 ? Potions.SLOWNESS : minion.getRandom().nextBoolean() ? Potions.WEAKNESS : Potions.POISON;
            ThrownPotion thrown = new ThrownPotion(minion.level(), minion);
            thrown.setItem(PotionContents.createItemStack(Items.SPLASH_POTION, potion));
            thrown.setXRot(thrown.getXRot() + 20F);
            Vec3 delta = target.getDeltaMovement();
            double dx = target.getX() + delta.x - minion.getX();
            double dy = target.getEyeY() - 1.1 - minion.getY();
            double dz = target.getZ() + delta.z - minion.getZ();
            thrown.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.2, dz, 0.75F, 8F);
            minion.level().playSound(null, minion.getX(), minion.getY(), minion.getZ(), SoundEvents.WITCH_THROW, SoundSource.HOSTILE, 1F,
                    0.8F + minion.getRandom().nextFloat() * 0.4F);
            minion.level().addFreshEntity(thrown);
        }
    };

    /** Witch head: below half health, drinks a potion of Harming, which heals the undead. Once every 30 seconds. */
    public static final PartTrait DARK_BREW = new Simple("dark_brew") {
        @Override
        public void tick(MinionEntity minion, BodyPartLocation location) {
            if (minion.getHealth() < minion.getMaxHealth() * 0.5F && minion.cooldown("dark_brew", 600)) {
                minion.heal(minion.getMaxHealth() * 0.25F);
                minion.playSound(SoundEvents.WITCH_DRINK, 1F, 0.8F);
                if (minion.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.WITCH, minion.getX(), minion.getY(1.0), minion.getZ(), 10, 0.3, 0.3, 0.3, 0);
                }
            }
        }
    };

    /** Chicken torso: lays an egg every 5 to 10 minutes. */
    public static final PartTrait LAYS_EGGS = new Simple("lays_eggs") {
        @Override
        public void tick(MinionEntity minion, BodyPartLocation location) {
            if (!minion.isCoolingDown("egg_started")) {
                minion.cooldown("egg_started", Integer.MAX_VALUE / 2);
                minion.cooldown("egg", 6000 + minion.getRandom().nextInt(6000));
            } else if (minion.cooldown("egg", 6000 + minion.getRandom().nextInt(6000))) {
                minion.playSound(SoundEvents.CHICKEN_EGG, 1F, (minion.getRandom().nextFloat() - minion.getRandom().nextFloat()) * 0.2F + 1F);
                minion.spawnAtLocation(Items.EGG);
            }
        }
    };

    // --- interactions ---

    /** Cow torso: milk it with a bucket, once a minute. */
    public static final PartTrait MILKABLE = interact("milkable", Items.BUCKET, 1200, (m, p, stack) -> {
        p.playSound(SoundEvents.COW_MILK, 1F, 1F);
        return ItemUtils.createFilledResult(stack, p, new ItemStack(Items.MILK_BUCKET));
    });

    /** Mooshroom torso: a bowl gives mushroom stew, every 5 minutes. */
    public static final PartTrait STEW = interact("stew", Items.BOWL, 6000, (m, p, stack) -> {
        p.playSound(SoundEvents.MOOSHROOM_MILK, 1F, 1F);
        return ItemUtils.createFilledResult(stack, p, new ItemStack(Items.MUSHROOM_STEW));
    });

    /** Sheep torso: shear it for 1 to 3 wool; it grows back in 2 minutes. */
    public static final PartTrait WOOLLY = interact("woolly", Items.SHEARS, 2400, (m, p, stack) -> {
        m.playSound(SoundEvents.SHEEP_SHEAR, 1F, 1F);
        int count = 1 + m.getRandom().nextInt(3);
        for (int i = 0; i < count; i++) {
            ItemEntity wool = m.spawnAtLocation(Items.WHITE_WOOL, 1);
            if (wool != null) {
                wool.setDeltaMovement(wool.getDeltaMovement().add((m.getRandom().nextFloat() - m.getRandom().nextFloat()) * 0.1F,
                        m.getRandom().nextFloat() * 0.05F, (m.getRandom().nextFloat() - m.getRandom().nextFloat()) * 0.1F));
            }
        }
        stack.hurtAndBreak(1, p, p.getUsedItemHand() == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND);
        return stack;
    });

    // --- helpers ---

    public static PartTrait flag(String name, TraitFlag... flags) {
        Set<TraitFlag> set = flags.length == 0 ? EnumSet.noneOf(TraitFlag.class) : EnumSet.of(flags[0], flags);
        return new Simple(name) {
            @Override
            public boolean has(TraitFlag flag) {
                return set.contains(flag);
            }
        };
    }

    public static PartTrait onHit(String name, float chance, java.util.function.BiConsumer<MinionEntity, LivingEntity> effect) {
        return new Simple(name) {
            @Override
            public void onAttack(MinionEntity minion, BodyPartLocation location, LivingEntity target) {
                if (minion.getRandom().nextFloat() < chance) {
                    effect.accept(minion, target);
                }
            }
        };
    }

    @FunctionalInterface
    public interface Use {
        ItemStack apply(MinionEntity minion, Player player, ItemStack stack);
    }

    public static PartTrait interact(String name, net.minecraft.world.item.Item item, int cooldown, Use use) {
        return new Simple(name) {
            @Override
            public InteractionResult onInteract(MinionEntity minion, BodyPartLocation location, Player player, InteractionHand hand) {
                ItemStack stack = player.getItemInHand(hand);
                if (!stack.is(item)) {
                    return InteractionResult.PASS;
                }
                if (!minion.cooldown(name, cooldown)) {
                    return InteractionResult.PASS;
                }
                player.setItemInHand(hand, use.apply(minion, player, stack));
                return InteractionResult.SUCCESS;
            }
        };
    }

    /** A trait with an id under the mod's namespace. */
    public static class Simple implements PartTrait {
        private final ResourceLocation id;

        public Simple(String name) {
            this.id = Necromancy.id(name);
        }

        @Override
        public ResourceLocation id() {
            return id;
        }
    }
}
