package com.sirolf2009.necromancy.trait;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.NecroConfig;
import com.sirolf2009.necromancy.api.BodyPartLocation;
import com.sirolf2009.necromancy.api.NecroMob;
import com.sirolf2009.necromancy.api.NecroMobRegistry;
import com.sirolf2009.necromancy.api.PartTrait;
import com.sirolf2009.necromancy.api.PartType;
import com.sirolf2009.necromancy.api.TraitFlag;
import com.sirolf2009.necromancy.entity.BloodTearEntity;
import com.sirolf2009.necromancy.entity.MinionEntity;
import com.sirolf2009.necromancy.entity.PartBoltEntity;
import com.sirolf2009.necromancy.entity.TearEntity;
import com.sirolf2009.necromancy.registry.NecroSounds;
import com.sirolf2009.necromancy.world.PlacedBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Part traits described in data: the parameterised trait types of docs/orespawn_parts_spec.json ({@code trait_types}),
 * used by the creatures of other mods. Each JSON trait {@code {"type": ..., params}} becomes one {@link PartTrait}; a
 * {@code flag} entry becomes one flag trait per flag. Tooltips are built from the parameters.
 */
public final class DataTraits {
    private DataTraits() {
    }

    // new flags (the existing ones are in Traits)
    public static final PartTrait GILLS = Traits.flag("gills", TraitFlag.BREATHES_WATER);
    public static final PartTrait CACTUS_PROOF = Traits.flag("cactus_proof", TraitFlag.CACTUS_PROOF);
    public static final PartTrait BURROWER = Traits.flag("burrower", TraitFlag.IN_WALL_PROOF);
    public static final PartTrait REAPER = Traits.flag("reaper", TraitFlag.REAPS);
    public static final PartTrait FLOATS = Traits.flag("floats", TraitFlag.FLOATS);

    private static PartTrait flagTrait(String name) {
        return switch (name) {
            case "climbs" -> Traits.CLIMBER;
            case "burns_in_sun" -> Traits.SUNBURN;
            case "hurt_by_water" -> Traits.WATER_HURTS;
            case "fire_proof" -> Traits.FIRE_PROOF;
            case "sun_proof" -> Traits.SUN_PROOF;
            case "thinks" -> Traits.THINKER;
            case "sinks" -> Traits.HEAVY;
            case "breathes_water" -> GILLS;
            case "cactus_proof" -> CACTUS_PROOF;
            case "in_wall_proof" -> BURROWER;
            case "reaps" -> REAPER;
            case "floats" -> FLOATS;
            default -> throw new IllegalArgumentException("Unknown trait flag '" + name + "'");
        };
    }

    /** The traits of one part from its JSON list. {@code mob} and {@code type} name the cooldowns and modifiers. */
    public static List<PartTrait> parse(ResourceLocation mob, PartType type, JsonArray traits) {
        List<PartTrait> out = new ArrayList<>();
        int index = 0;
        for (JsonElement element : traits) {
            JsonObject t = element.getAsJsonObject();
            String kind = t.get("type").getAsString();
            String key = mob.getPath() + "." + type.getSerializedName() + "." + index++;
            if (kind.equals("flag")) {
                for (JsonElement flag : t.getAsJsonArray("flags")) {
                    out.add(flagTrait(flag.getAsString()));
                }
                continue;
            }
            out.add(create(kind, key, mob, type, t));
        }
        return out;
    }

    private static PartTrait create(String kind, String key, ResourceLocation mob, PartType type, JsonObject t) {
        return switch (kind) {
            case "on_hit_effect" -> new OnHitEffect(key, t);
            case "on_hit_ignite" -> new OnHitIgnite(key, t);
            case "on_hit_toss" -> new OnHitToss(key, t);
            case "on_hit_disarm" -> new OnHitDisarm(key, t);
            case "on_hit_block" -> new OnHitBlock(key, t);
            case "lifesteal" -> new Lifesteal(key, t);
            case "bane" -> new Bane(key, t);
            case "charge" -> new Charge(key, t);
            case "pack" -> new Pack(key, t);
            case "ranged" -> new Ranged(key, t);
            case "leap" -> new Leap(key, t);
            case "teleport" -> new Teleport(key, t);
            case "on_hurt_effect" -> new OnHurtEffect(key, t);
            case "on_hurt_reflect" -> new OnHurtReflect(key, t);
            case "on_hurt_knockback" -> new OnHurtKnockback(key, t);
            case "damage_filter" -> new DamageFilter(key, t);
            case "last_stand" -> new LastStand(key, t);
            case "regen" -> new Regen(key, t);
            case "on_death_explode" -> new OnDeathExplode(key, t);
            case "on_death_cloud" -> new OnDeathCloud(key, t);
            case "summon" -> new Summon(key, t);
            case "aura" -> new Aura(key, t);
            case "pull" -> new Pull(key, t);
            case "produce" -> new Produce(key, t);
            case "interact_yield" -> new InteractYield(key, t);
            case "forager" -> new Forager(key, t);
            case "mount" -> new MountTrait(key, t);
            case "glide" -> new Glide(key, t);
            case "environment" -> new Environment(key, mob, type, t);
            case "emits_light" -> new EmitsLight(key, t);
            case "transfuse" -> new Transfuse(key, t);
            default -> throw new IllegalArgumentException("Unknown trait type '" + kind + "' for " + mob);
        };
    }

    /** The mount settings of a torso's {@code mount} trait. */
    public static NecroMob.Mount mount(JsonObject t) {
        return new NecroMob.Mount(Math.max(1, Math.min(2, i(t, "seats", 1))), "water".equals(s(t, "medium", "land")),
                "saddle_direct".equals(s(t, "steering", "saddle_direct")), (float) d(t, "jump", 0), (float) d(t, "speed_bonus", 0));
    }

    // ------------------------------------------------------------------ the base

    /** One data trait: its type names the tooltip, its key the cooldowns. */
    public abstract static class Base implements PartTrait {
        protected final String type;
        protected final String key;
        private final ResourceLocation id;

        protected Base(String type, String key) {
            this.type = type;
            this.key = key;
            this.id = Necromancy.id(type);
        }

        @Override
        public ResourceLocation id() {
            return id;
        }

        protected String lang() {
            return "trait.necromancy." + type;
        }

        @Override
        public Component name() {
            return Component.translatable(lang());
        }

        @Override
        public Component description() {
            return Component.translatable(lang() + ".desc");
        }

        /** Arm traits run once per Minion: on the right arm, or on the left when the right arm lacks this trait. */
        protected boolean once(MinionEntity minion, BodyPartLocation location) {
            if (location != BodyPartLocation.ARM_LEFT) {
                return true;
            }
            NecroMob right = minion.part(BodyPartLocation.ARM_RIGHT);
            return right == null || !right.traits(PartType.ARM).contains(this);
        }
    }

    // ------------------------------------------------------------------ on hit

    static final class OnHitEffect extends Base {
        final ResourceLocation effect;
        final int duration, amplifier, cooldown;
        final float chance;

        OnHitEffect(String key, JsonObject t) {
            super("on_hit_effect", key);
            effect = rl(t, "effect");
            duration = i(t, "duration", 100);
            amplifier = i(t, "amplifier", 0);
            chance = (float) d(t, "chance", 1);
            cooldown = i(t, "cooldown", 0);
        }

        @Override
        public void onAttack(MinionEntity minion, BodyPartLocation location, LivingEntity target) {
            if (once(minion, location) && minion.getRandom().nextFloat() < chance && (cooldown == 0 || minion.cooldown(key, cooldown))) {
                Holder<MobEffect> holder = effect(effect);
                if (holder != null) {
                    target.addEffect(new MobEffectInstance(holder, duration, amplifier), minion);
                }
            }
        }

        @Override
        public Component name() {
            return Component.translatable(lang(), effectName(effect, amplifier));
        }

        @Override
        public Component description() {
            return chance < 1 ? Component.translatable(lang() + ".desc.chance", pct(chance), secs(duration))
                    : Component.translatable(lang() + ".desc", secs(duration));
        }
    }

    static final class OnHitIgnite extends Base {
        final int seconds;
        final float chance;

        OnHitIgnite(String key, JsonObject t) {
            super("on_hit_ignite", key);
            seconds = i(t, "seconds", 4);
            chance = (float) d(t, "chance", 1);
        }

        @Override
        public void onAttack(MinionEntity minion, BodyPartLocation location, LivingEntity target) {
            if (once(minion, location) && !target.fireImmune() && !target.isInWaterRainOrBubble() && minion.getRandom().nextFloat() < chance) {
                target.igniteForSeconds(seconds);
            }
        }

        @Override
        public Component description() {
            return chance < 1 ? Component.translatable(lang() + ".desc.chance", pct(chance), seconds)
                    : Component.translatable(lang() + ".desc", seconds);
        }
    }

    static final class OnHitToss extends Base {
        final double horizontal, vertical;
        final float chance;
        final int cooldown;

        OnHitToss(String key, JsonObject t) {
            super("on_hit_toss", key);
            horizontal = d(t, "horizontal", 0);
            vertical = d(t, "vertical", 0.4);
            chance = (float) d(t, "chance", 1);
            cooldown = i(t, "cooldown", 0);
        }

        @Override
        public void onAttack(MinionEntity minion, BodyPartLocation location, LivingEntity target) {
            if (!once(minion, location) || minion.getRandom().nextFloat() >= chance || cooldown > 0 && !minion.cooldown(key, cooldown)) {
                return;
            }
            Vec3 dir = new Vec3(target.getX() - minion.getX(), 0, target.getZ() - minion.getZ());
            dir = dir.lengthSqr() < 1.0E-4 ? Vec3.ZERO : dir.normalize();
            double resist = 1 - Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
            double up = vertical * (target instanceof Player ? 2 : 1);
            target.push(dir.x * horizontal * resist, up * resist, dir.z * horizontal * resist);
            target.hurtMarked = true;
            NecroMob head = minion.part(BodyPartLocation.HEAD);
            SoundEvent sound = head == null ? null : head.hurtSound();
            minion.playSound(sound != null ? sound : SoundEvents.IRON_GOLEM_ATTACK, 1F, 0.8F);
        }

        @Override
        public Component description() {
            return Component.translatable(chance < 1 ? lang() + ".desc.chance" : lang() + ".desc", pct(chance));
        }
    }

    static final class OnHitDisarm extends Base {
        final List<EquipmentSlot> slots = new ArrayList<>();
        final float chance;
        final boolean toPack;

        OnHitDisarm(String key, JsonObject t) {
            super("on_hit_disarm", key);
            for (JsonElement e : t.getAsJsonArray("slots")) {
                slots.add(EquipmentSlot.byName(e.getAsString()));
            }
            chance = (float) d(t, "chance", 0.1);
            toPack = b(t, "to_pack", false);
        }

        @Override
        public void onAttack(MinionEntity minion, BodyPartLocation location, LivingEntity target) {
            if (!once(minion, location) || !(target instanceof Mob mob) || mob.hasCustomName() || minion.getRandom().nextFloat() >= chance) {
                return;
            }
            if (mob instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
                return;
            }
            for (EquipmentSlot slot : slots) {
                ItemStack stack = mob.getItemBySlot(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                ItemStack taken = stack.copy();
                if (taken.isDamageableItem()) {
                    taken.setDamageValue(Math.min(taken.getMaxDamage() - 1, taken.getDamageValue() + Math.max(1, taken.getMaxDamage() / 20)));
                }
                mob.setItemSlot(slot, ItemStack.EMPTY);
                mob.setDropChance(slot, 0F);
                if (toPack && minion.pack().canAddItem(taken)) {
                    minion.pack().addItem(taken);
                } else {
                    ItemEntity item = new ItemEntity(minion.level(), mob.getX(), mob.getY(), mob.getZ(), taken);
                    item.setNoPickUpDelay();
                    minion.level().addFreshEntity(item);
                }
                minion.playSound(SoundEvents.ITEM_FRAME_REMOVE_ITEM, 1F, 0.8F);
                return;
            }
        }

        @Override
        public Component description() {
            return Component.translatable(lang() + ".desc", pct(chance));
        }
    }

    static final class OnHitBlock extends Base {
        final ResourceLocation block;
        final float chance;
        final int lifetime, cooldown;

        OnHitBlock(String key, JsonObject t) {
            super("on_hit_block", key);
            block = rl(t, "block");
            chance = (float) d(t, "chance", 0.2);
            lifetime = i(t, "lifetime", 100);
            cooldown = i(t, "cooldown", 100);
        }

        @Override
        public void onAttack(MinionEntity minion, BodyPartLocation location, LivingEntity target) {
            if (!once(minion, location) || !(minion.level() instanceof ServerLevel level) || !block.equals(ResourceLocation.withDefaultNamespace("cobweb"))
                    || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) || minion.getRandom().nextFloat() >= chance || !minion.cooldown(key, cooldown)) {
                return;
            }
            BlockPos pos = target.blockPosition();
            BlockState state = level.getBlockState(pos);
            if ((state.isAir() || state.canBeReplaced()) && state.getFluidState().isEmpty() && level.getBlockEntity(pos) == null) {
                PlacedBlocks.place(level, pos, Blocks.COBWEB.defaultBlockState(), lifetime);
            }
        }

        @Override
        public Component description() {
            return Component.translatable(lang() + ".desc", pct(chance), secs(lifetime));
        }
    }

    static final class Lifesteal extends Base {
        final double fraction, max;

        Lifesteal(String key, JsonObject t) {
            super("lifesteal", key);
            fraction = d(t, "fraction", 0.1);
            max = d(t, "max_per_hit", 2);
        }

        @Override
        public void onAttack(MinionEntity minion, BodyPartLocation location, LivingEntity target) {
            double damage = minion.getAttributeValue(Attributes.ATTACK_DAMAGE);
            if (location.type() == PartType.ARM) {
                damage /= 2; // each arm heals for its share of the hit
            }
            minion.heal((float) Math.min(max, fraction * damage));
        }

        @Override
        public Component description() {
            return Component.translatable(lang() + ".desc", pct(fraction), num(max));
        }
    }

    static final class Bane extends Base {
        final String target;
        @Nullable
        final TagKey<EntityType<?>> tag;
        final double multiplier;

        Bane(String key, JsonObject t) {
            super("bane", key);
            target = s(t, "target", "large");
            tag = target.startsWith("#") ? TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(target.substring(1))) : null;
            multiplier = d(t, "multiplier", 1.5);
        }

        boolean matches(LivingEntity victim) {
            if (tag != null) {
                return victim.getType().is(tag);
            }
            return switch (target) {
                case "airborne" -> !victim.onGround() && !victim.isInWater();
                case "small" -> victim.getBbWidth() < 0.6F;
                case "large" -> victim.getBbWidth() * victim.getBbHeight() > 4F;
                case "wounded" -> victim.getHealth() < victim.getMaxHealth() * 0.5F;
                default -> false;
            };
        }

        @Override
        public void onAttack(MinionEntity minion, BodyPartLocation location, LivingEntity victim) {
            if (once(minion, location) && victim.isAlive() && matches(victim)) {
                extraHit(minion, victim, (float) ((multiplier - 1) * minion.getAttributeValue(Attributes.ATTACK_DAMAGE)));
            }
        }

        String label() {
            return tag != null ? tag.location().getPath().replace('/', '.') : target;
        }

        @Override
        public Component name() {
            return Component.translatable(lang(), Component.translatable(lang() + ".target." + label()));
        }

        @Override
        public Component description() {
            return Component.translatable(lang() + ".desc", num(multiplier));
        }
    }

    static final class Charge extends Base {
        final double minRun, multiplier, knockback;
        final int window, cooldown;

        Charge(String key, JsonObject t) {
            super("charge", key);
            minRun = d(t, "min_run", 4);
            window = Math.max(1, Math.min(200, i(t, "window", 40)));
            multiplier = d(t, "multiplier", 1.5);
            knockback = d(t, "knockback", 1.2);
            cooldown = i(t, "cooldown", 100);
        }

        @Override
        public void tick(MinionEntity minion, BodyPartLocation location) {
            if (minRun <= 0 || !once(minion, location)) {
                return;
            }
            // a ring of the distances to the target over the last window ticks
            double[] ring = minion.traitState(key, window + 2);
            LivingEntity target = minion.getTarget();
            if (target == null || !target.isAlive()) {
                ring[1] = 0;
                return;
            }
            int at = (int) ring[0];
            ring[2 + at] = minion.distanceTo(target);
            ring[0] = (at + 1) % window;
            ring[1] = Math.min(window, ring[1] + 1);
        }

        boolean charged(MinionEntity minion, LivingEntity target) {
            if (minRun <= 0) {
                return !minion.isCoolingDown(key + ".hit");
            }
            double[] ring = minion.traitState(key, window + 2);
            int filled = (int) ring[1];
            if (filled < 2) {
                return false;
            }
            int newest = Math.floorMod((int) ring[0] - 1, window);
            int oldest = filled < window ? 0 : (int) ring[0];
            return ring[2 + oldest] - ring[2 + newest] >= minRun;
        }

        @Override
        public void onAttack(MinionEntity minion, BodyPartLocation location, LivingEntity target) {
            if (!once(minion, location)) {
                return;
            }
            boolean ready = charged(minion, target) && !minion.isCoolingDown(key);
            minion.setCooldown(key + ".hit", cooldown);
            if (ready && target.isAlive()) {
                minion.setCooldown(key, cooldown);
                extraHit(minion, target, (float) ((multiplier - 1) * minion.getAttributeValue(Attributes.ATTACK_DAMAGE)));
                Vec3 push = new Vec3(target.getX() - minion.getX(), 0, target.getZ() - minion.getZ());
                if (push.lengthSqr() > 1.0E-4) {
                    push = push.normalize().scale(knockback);
                    target.push(push.x, 0.35, push.z);
                    target.hurtMarked = true;
                }
                minion.playSound(SoundEvents.GOAT_RAM_IMPACT, 1F, 1F);
            }
        }

        @Override
        public Component description() {
            return minRun <= 0 ? Component.translatable(lang() + ".desc.ambush", num(multiplier), secs(cooldown))
                    : Component.translatable(lang() + ".desc", num(minRun), num(multiplier));
        }
    }

    static final class Pack extends Base {
        final double perAlly, max, radius;

        Pack(String key, JsonObject t) {
            super("pack", key);
            perAlly = d(t, "per_ally", 1);
            max = d(t, "max", 3);
            radius = d(t, "radius", 8);
        }

        @Override
        public void onAttack(MinionEntity minion, BodyPartLocation location, LivingEntity target) {
            if (!once(minion, location) || minion.getOwnerUUID() == null) {
                return;
            }
            List<MinionEntity> pack = minion.level().getEntitiesOfClass(MinionEntity.class, minion.getBoundingBox().inflate(radius),
                    other -> other != minion && minion.getOwnerUUID().equals(other.getOwnerUUID()) && holds(other, location));
            double bonus = Math.min(max, perAlly * pack.size());
            if (bonus > 0 && target.isAlive()) {
                extraHit(minion, target, (float) bonus);
            }
        }

        private static boolean holds(MinionEntity other, BodyPartLocation location) {
            NecroMob mob = other.part(location);
            return mob != null && mob.traits(location.type()).stream().anyMatch(t -> t instanceof Pack);
        }

        @Override
        public Component description() {
            return Component.translatable(lang() + ".desc", num(perAlly), num(max));
        }
    }

    // ------------------------------------------------------------------ every tick: ranged, leap, teleport

    static final class Ranged extends Base {
        final String projectile;
        final int cooldown, count, effectDuration, effectAmplifier, ignite;
        final double range, minRange, spread, velocity, damage, explosion;
        @Nullable
        final ResourceLocation item, effect;
        final boolean needsLos;

        Ranged(String key, JsonObject t) {
            super("ranged", key);
            projectile = s(t, "projectile", "necromancy:part_bolt");
            cooldown = Math.max(5, i(t, "cooldown", 60));
            range = d(t, "range", 16);
            minRange = d(t, "min_range", 0);
            count = Math.max(1, Math.min(8, i(t, "count", 1)));
            spread = d(t, "spread", 0);
            velocity = d(t, "velocity", 1.6);
            damage = d(t, "damage", 4);
            item = t.has("item") && !t.get("item").isJsonNull() ? rl(t, "item") : null;
            effect = t.has("effect") && !t.get("effect").isJsonNull() ? rl(t, "effect") : null;
            effectDuration = i(t, "effect_duration", 0);
            effectAmplifier = i(t, "effect_amplifier", 0);
            ignite = i(t, "ignite", 0);
            explosion = Math.min(1.5, d(t, "explosion", 0));
            needsLos = b(t, "needs_los", true);
        }

        @Override
        public void tick(MinionEntity minion, BodyPartLocation location) {
            LivingEntity target = combatTarget(minion);
            if (target == null || !once(minion, location)) {
                return;
            }
            double distance = minion.distanceTo(target);
            if (distance > range || distance < minRange || needsLos && !minion.getSensing().hasLineOfSight(target) || !minion.cooldown(key, cooldown)) {
                return;
            }
            double y = switch (location.type()) {
                case HEAD -> minion.getEyeY() - 0.1;
                case ARM -> minion.getY(0.75);
                default -> minion.getY(0.5);
            };
            Level level = minion.level();
            for (int n = 0; n < count; n++) {
                Projectile shot = switch (projectile) {
                    case "minecraft:small_fireball" -> {
                        Vec3 dir = new Vec3(target.getX() - minion.getX(), target.getEyeY() - 0.3 - y, target.getZ() - minion.getZ()).normalize();
                        SmallFireball fireball = new SmallFireball(level, minion, aim(minion, dir));
                        fireball.setPos(minion.getX(), y, minion.getZ());
                        yield fireball;
                    }
                    case "minecraft:snowball" -> new Snowball(level, minion);
                    case "necromancy:tear" -> minion.getRandom().nextInt(5) == 0 ? new BloodTearEntity(level, minion) : new TearEntity(level, minion);
                    default -> {
                        PartBoltEntity bolt = new PartBoltEntity(level, minion);
                        bolt.configure(item == null ? new ItemStack(Items.SNOWBALL) : new ItemStack(BuiltInRegistries.ITEM.get(item)), (float) damage,
                                effect, effectDuration, effectAmplifier, ignite, (float) explosion);
                        yield bolt;
                    }
                };
                if (!(shot instanceof SmallFireball)) {
                    shot.setPos(minion.getX(), y, minion.getZ());
                    double dx = target.getX() - minion.getX();
                    double dy = target.getEyeY() - 0.3 - y;
                    double dz = target.getZ() - minion.getZ();
                    double drop = shot instanceof PartBoltEntity ? 0.05 : 0.2;
                    shot.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * drop, dz, (float) velocity, (float) (spread / 0.0172275));
                }
                level.addFreshEntity(shot);
            }
            if (projectile.equals("minecraft:small_fireball")) {
                level.levelEvent(null, 1018, minion.blockPosition(), 0);
            } else if (projectile.equals("necromancy:tear")) {
                minion.playSound(NecroSounds.TEAR.get(), 1F, 1F);
            } else {
                minion.playSound(SoundEvents.SNOW_GOLEM_SHOOT, 1F, 0.6F + minion.getRandom().nextFloat() * 0.4F);
            }
        }

        private Vec3 aim(MinionEntity minion, Vec3 dir) {
            if (spread <= 0) {
                return dir;
            }
            return dir.add(minion.getRandom().triangle(0, spread), minion.getRandom().triangle(0, spread), minion.getRandom().triangle(0, spread)).normalize();
        }

        @Override
        public Component name() {
            Component what = switch (projectile) {
                case "minecraft:small_fireball" -> Component.translatable(Items.FIRE_CHARGE.getDescriptionId());
                case "minecraft:snowball" -> Component.translatable(Items.SNOWBALL.getDescriptionId());
                case "necromancy:tear" -> Component.translatable("entity.necromancy.tear");
                default -> item == null ? Component.translatable("entity.necromancy.part_bolt") : Component.translatable(BuiltInRegistries.ITEM.get(item).getDescriptionId());
            };
            return count > 1 ? Component.translatable(lang() + ".volley", what, count) : Component.translatable(lang(), what);
        }

        @Override
        public Component description() {
            MutableComponent text = Component.translatable(lang() + ".desc", secs(cooldown), num(range));
            if (effect != null) {
                text.append(" ").append(Component.translatable(lang() + ".effect", effectName(effect, effectAmplifier)));
            }
            if (ignite > 0) {
                text.append(" ").append(Component.translatable(lang() + ".ignite"));
            }
            return text;
        }
    }

    static final class Leap extends Base {
        final double minRange, maxRange, horizontal, vertical;
        final int cooldown;

        Leap(String key, JsonObject t) {
            super("leap", key);
            minRange = d(t, "min_range", 3);
            maxRange = d(t, "max_range", 8);
            horizontal = d(t, "horizontal", 0.8);
            vertical = d(t, "vertical", 0.4);
            cooldown = i(t, "cooldown", 80);
        }

        @Override
        public void tick(MinionEntity minion, BodyPartLocation location) {
            LivingEntity target = combatTarget(minion);
            if (target == null || !once(minion, location) || !minion.onGround() || minion.isVehicle()) {
                return;
            }
            double distance = minion.distanceTo(target);
            if (distance < minRange || distance > maxRange || !minion.cooldown(key, cooldown)) {
                return;
            }
            Vec3 dir = new Vec3(target.getX() - minion.getX(), 0, target.getZ() - minion.getZ()).normalize();
            minion.setDeltaMovement(dir.x * horizontal, vertical, dir.z * horizontal);
            minion.hasImpulse = true;
            minion.setCooldown("leap_landing", 40);
        }

        @Override
        public Component description() {
            return Component.translatable(lang() + ".desc", num(minRange), num(maxRange));
        }
    }

    static final class Teleport extends Base {
        final String mode;
        final int cooldown;
        final float chance;
        final double minDistance, radius;

        Teleport(String key, JsonObject t) {
            super("teleport", key);
            mode = s(t, "mode", "to_target");
            cooldown = i(t, "cooldown", 100);
            chance = (float) d(t, "chance", 1);
            minDistance = d(t, "min_distance", 0);
            radius = d(t, "radius", 8);
        }

        @Override
        public void tick(MinionEntity minion, BodyPartLocation location) {
            if (!mode.equals("to_target") || !once(minion, location)) {
                return;
            }
            LivingEntity target = combatTarget(minion);
            if (target == null || minion.isVehicle() || minion.distanceTo(target) < Math.max(2, minDistance) || minion.isCoolingDown(key)) {
                return;
            }
            for (int i = 0; i < 16; i++) {
                double x = target.getX() + (minion.getRandom().nextDouble() - 0.5) * 4;
                double z = target.getZ() + (minion.getRandom().nextDouble() - 0.5) * 4;
                double y = target.getY() + minion.getRandom().nextInt(3) - 1;
                Vec3 from = minion.position();
                if (minion.randomTeleport(x, y, z, true)) {
                    minion.setCooldown(key, cooldown);
                    teleported(minion, from);
                    return;
                }
            }
            minion.setCooldown(key, 40);
        }

        @Override
        public float onHurt(MinionEntity minion, BodyPartLocation location, DamageSource source, float amount) {
            if (!mode.equals("away_when_hurt") || !once(minion, location) || minion.isVehicle()) {
                return amount;
            }
            boolean ranged = source.is(DamageTypeTags.IS_PROJECTILE) || source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC);
            if (!ranged || minion.getRandom().nextFloat() >= chance || minion.isCoolingDown(key)) {
                return amount;
            }
            Vec3 from = minion.position();
            if (randomTeleport(minion, minion, radius)) {
                minion.setCooldown(key, cooldown);
                teleported(minion, from);
                return 0F;
            }
            return amount;
        }

        @Override
        public void onAttack(MinionEntity minion, BodyPartLocation location, LivingEntity target) {
            if (!mode.equals("victim") || !once(minion, location) || !target.isAlive() || isBoss(target)) {
                return;
            }
            boolean allowed = target instanceof Mob || target instanceof Player player && minion.isEnemyListed(player);
            if (allowed && minion.getRandom().nextFloat() < chance && minion.cooldown(key, cooldown)) {
                Vec3 from = target.position();
                if (randomTeleport(target, minion, radius)) {
                    minion.level().playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1F, 1F);
                    target.playSound(SoundEvents.ENDERMAN_TELEPORT, 1F, 1F);
                }
            }
        }

        private static boolean randomTeleport(LivingEntity who, MinionEntity minion, double radius) {
            for (int i = 0; i < 16; i++) {
                double x = who.getX() + (minion.getRandom().nextDouble() - 0.5) * 2 * radius;
                double y = Mth.clamp(who.getY() + (minion.getRandom().nextInt((int) Math.max(2, radius)) - radius / 2), who.level().getMinBuildHeight(),
                        who.level().getMaxBuildHeight() - 1);
                double z = who.getZ() + (minion.getRandom().nextDouble() - 0.5) * 2 * radius;
                if (who.randomTeleport(x, y, z, true)) {
                    return true;
                }
            }
            return false;
        }

        private static void teleported(MinionEntity minion, Vec3 from) {
            minion.level().playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1F, 1F);
            minion.playSound(SoundEvents.ENDERMAN_TELEPORT, 1F, 1F);
        }

        @Override
        protected String lang() {
            return super.lang() + "." + mode;
        }

        @Override
        public Component description() {
            return switch (mode) {
                case "to_target" -> Component.translatable(lang() + ".desc", num(Math.max(2, minDistance)), secs(cooldown));
                case "away_when_hurt" -> Component.translatable(lang() + ".desc", pct(chance));
                default -> Component.translatable(lang() + ".desc", pct(chance), num(radius));
            };
        }
    }

    // ------------------------------------------------------------------ when hurt

    static final class OnHurtEffect extends Base {
        final ResourceLocation effect;
        final int duration, amplifier;
        final float chance;
        final boolean meleeOnly;

        OnHurtEffect(String key, JsonObject t) {
            super("on_hurt_effect", key);
            effect = rl(t, "effect");
            duration = i(t, "duration", 60);
            amplifier = i(t, "amplifier", 0);
            chance = (float) d(t, "chance", 1);
            meleeOnly = b(t, "melee_only", true);
        }

        @Override
        public float onHurt(MinionEntity minion, BodyPartLocation location, DamageSource source, float amount) {
            LivingEntity attacker = attacker(minion, source, meleeOnly);
            if (attacker != null && once(minion, location) && minion.getRandom().nextFloat() < chance) {
                Holder<MobEffect> holder = effect(effect);
                if (holder != null) {
                    attacker.addEffect(new MobEffectInstance(holder, duration, amplifier), minion);
                    if (minion.level() instanceof ServerLevel level) {
                        ParticleOptions particle = effect.getPath().equals("blindness") ? ParticleTypes.SQUID_INK
                                : effect.getPath().equals("nausea") ? ParticleTypes.SNEEZE : ParticleTypes.WITCH;
                        level.sendParticles(particle, minion.getX(), minion.getY(0.6), minion.getZ(), 20, 0.4, 0.4, 0.4, 0.05);
                    }
                }
            }
            return amount;
        }

        @Override
        public Component name() {
            return Component.translatable(lang(), effectName(effect, amplifier));
        }

        @Override
        public Component description() {
            return Component.translatable(lang() + ".desc", pct(chance), secs(duration));
        }
    }

    static final class OnHurtReflect extends Base {
        final double fraction, flat, max;
        final int igniteSeconds;
        final boolean meleeOnly;

        OnHurtReflect(String key, JsonObject t) {
            super("on_hurt_reflect", key);
            fraction = d(t, "fraction", 0);
            flat = d(t, "flat", 0);
            max = d(t, "max", 4);
            igniteSeconds = i(t, "ignite_seconds", 0);
            meleeOnly = b(t, "melee_only", true);
        }

        @Override
        public float onHurt(MinionEntity minion, BodyPartLocation location, DamageSource source, float amount) {
            if (source.is(net.minecraft.world.damagesource.DamageTypes.THORNS) || !once(minion, location)) {
                return amount;
            }
            LivingEntity attacker = attacker(minion, source, meleeOnly);
            if (attacker != null) {
                float back = (float) Math.min(max, flat + fraction * amount);
                if (back > 0) {
                    attacker.hurt(minion.damageSources().thorns(minion), back);
                }
                if (igniteSeconds > 0 && !attacker.fireImmune()) {
                    attacker.igniteForSeconds(igniteSeconds);
                }
            }
            return amount;
        }

        @Override
        public Component name() {
            return Component.translatable(igniteSeconds > 0 ? lang() + ".burning" : lang());
        }

        @Override
        public Component description() {
            if (fraction <= 0 && flat <= 0) {
                return Component.translatable(lang() + ".desc.fire", igniteSeconds);
            }
            MutableComponent text = fraction > 0 ? Component.translatable(lang() + ".desc.fraction", pct(fraction), num(max))
                    : Component.translatable(lang() + ".desc", num(Math.min(max, flat)));
            if (igniteSeconds > 0) {
                text.append(" ").append(Component.translatable(lang() + ".ignite", igniteSeconds));
            }
            return text;
        }
    }

    static final class OnHurtKnockback extends Base {
        final double strength;
        final float chance;

        OnHurtKnockback(String key, JsonObject t) {
            super("on_hurt_knockback", key);
            strength = d(t, "strength", 1);
            chance = (float) d(t, "chance", 0.3);
        }

        @Override
        public float onHurt(MinionEntity minion, BodyPartLocation location, DamageSource source, float amount) {
            LivingEntity attacker = attacker(minion, source, true);
            if (attacker != null && once(minion, location) && minion.getRandom().nextFloat() < chance) {
                attacker.knockback(strength, minion.getX() - attacker.getX(), minion.getZ() - attacker.getZ());
                attacker.hurtMarked = true;
                minion.playSound(SoundEvents.SLIME_JUMP, 1F, 1F);
            }
            return amount;
        }

        @Override
        public Component description() {
            return Component.translatable(lang() + ".desc", pct(chance));
        }
    }

    static final class DamageFilter extends Base {
        final List<String> types = new ArrayList<>();
        final double multiplier;
        final String condition;

        DamageFilter(String key, JsonObject t) {
            super("damage_filter", key);
            JsonElement list = t.get("types");
            if (list.isJsonArray()) {
                list.getAsJsonArray().forEach(e -> types.add(e.getAsString()));
            } else {
                types.add(list.getAsString());
            }
            double m = d(t, "multiplier", 0.5);
            multiplier = m == 0 ? 0 : Math.max(0.1, m);
            condition = s(t, "condition", "always");
        }

        boolean matches(DamageSource source) {
            for (String type : types) {
                if (type.equals("*")) {
                    return true;
                }
                if (type.startsWith("#") ? source.is(TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse(type.substring(1))))
                        : source.is(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse(type)))) {
                    return true;
                }
            }
            return false;
        }

        boolean holds(MinionEntity minion) {
            return switch (condition) {
                case "idle" -> minion.idleTicks() >= 40;
                case "in_water" -> minion.isInWater();
                case "below_half" -> minion.getHealth() < minion.getMaxHealth() * 0.5F;
                default -> true;
            };
        }

        @Override
        public float onHurt(MinionEntity minion, BodyPartLocation location, DamageSource source, float amount) {
            if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !once(minion, location) || !matches(source) || !holds(minion)) {
                return amount;
            }
            return (float) (amount * multiplier);
        }

        @Override
        public Component name() {
            String which = types.size() == 1 ? types.get(0) : "*";
            String label = which.equals("*") ? "all" : (which.startsWith("#") ? which.substring(1) : which).replace(':', '.').replace('/', '.');
            String verb = multiplier == 0 ? "immune" : multiplier < 1 ? "resists" : "weak";
            return Component.translatable(lang() + "." + verb, Component.translatable(lang() + ".type." + label));
        }

        @Override
        public Component description() {
            if (condition.equals("always")) {
                return multiplier == 0 ? Component.translatable(lang() + ".desc.immune.always") : Component.translatable(lang() + ".desc.always", pct(multiplier));
            }
            Component when = Component.translatable(lang() + ".when." + condition);
            return multiplier == 0 ? Component.translatable(lang() + ".desc.immune", when)
                    : Component.translatable(lang() + ".desc", pct(multiplier), when);
        }
    }

    static final class LastStand extends Base {
        final double threshold, healFraction, damageMultiplier;
        final int cooldown;
        final List<MobEffectInstanceSpec> effects = new ArrayList<>();

        LastStand(String key, JsonObject t) {
            super("last_stand", key);
            threshold = d(t, "threshold", 0.3);
            cooldown = i(t, "cooldown", 1200);
            healFraction = d(t, "heal_fraction", 0);
            damageMultiplier = d(t, "damage_multiplier", 1);
            if (t.has("effects")) {
                for (JsonElement e : t.getAsJsonArray("effects")) {
                    JsonObject o = e.getAsJsonObject();
                    effects.add(new MobEffectInstanceSpec(rl(o, "effect"), i(o, "duration", 100), i(o, "amplifier", 0)));
                }
            }
        }

        @Override
        public float onHurt(MinionEntity minion, BodyPartLocation location, DamageSource source, float amount) {
            if (!once(minion, location) || minion.getHealth() - amount >= minion.getMaxHealth() * threshold || !minion.cooldown(key, cooldown)) {
                return amount;
            }
            for (MobEffectInstanceSpec spec : effects) {
                Holder<MobEffect> holder = effect(spec.effect());
                if (holder != null) {
                    minion.addEffect(new MobEffectInstance(holder, spec.duration(), spec.amplifier()));
                }
            }
            if (healFraction > 0) {
                minion.heal((float) (minion.getMaxHealth() * healFraction));
            }
            minion.playSound(SoundEvents.TOTEM_USE, 0.4F, 1.4F);
            return (float) (amount * damageMultiplier);
        }

        @Override
        public Component description() {
            return Component.translatable(lang() + ".desc", pct(threshold), secs(cooldown));
        }
    }

    record MobEffectInstanceSpec(ResourceLocation effect, int duration, int amplifier) {
    }

    // ------------------------------------------------------------------ every tick: upkeep

    static final class Regen extends Base {
        final double amount;
        final int interval;
        final String condition;

        Regen(String key, JsonObject t) {
            super("regen", key);
            amount = d(t, "amount", 1);
            interval = Math.max(1, i(t, "interval", 40));
            condition = s(t, "condition", "always");
        }

        @Override
        public void tick(MinionEntity minion, BodyPartLocation location) {
            if (minion.tickCount % interval != 0 || minion.getHealth() >= minion.getMaxHealth() || !once(minion, location)) {
                return;
            }
            BlockPos pos = minion.blockPosition();
            Level level = minion.level();
            boolean holds = switch (condition) {
                case "in_water" -> minion.isInWater();
                case "in_rain" -> level.isRainingAt(pos) || level.isRainingAt(pos.above((int) minion.getBbHeight()));
                case "in_dark" -> level.getMaxLocalRawBrightness(pos) < 8;
                case "in_sun" -> level.isDay() && level.canSeeSky(pos);
                case "out_of_combat" -> minion.getTarget() == null && minion.tickCount - minion.getLastHurtByMobTimestamp() > 100;
                default -> true;
            };
            if (holds) {
                minion.heal((float) amount);
            }
        }

        @Override
        public Component description() {
            return Component.translatable(lang() + ".desc", num(amount), secs(interval), Component.translatable(lang() + ".when." + condition));
        }
    }

    static final class OnDeathExplode extends Base {
        final float power;
        final boolean fire, breaksBlocks;

        OnDeathExplode(String key, JsonObject t) {
            super("on_death_explode", key);
            power = (float) d(t, "power", 2);
            fire = b(t, "fire", false);
            breaksBlocks = b(t, "breaks_blocks", false);
        }

        @Override
        public void onDeath(MinionEntity minion, BodyPartLocation location, DamageSource source) {
            if (!once(minion, location)) {
                return;
            }
            Level level = minion.level();
            boolean griefing = breaksBlocks && NecroConfig.CREEPER_TORSO_BREAKS_BLOCKS.get() && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
            float strength = minion.isPowered() ? Math.min(4F, power * 2) : power;
            level.explode(minion, null, null, minion.getX(), minion.getY(), minion.getZ(), strength, fire && griefing,
                    griefing ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
        }

        @Override
        public Component description() {
            return Component.translatable(fire ? lang() + ".desc.fire" : lang() + ".desc", num(power));
        }
    }

    static final class OnDeathCloud extends Base {
        final ResourceLocation effect;
        final int duration, amplifier, cloudDuration;
        final float radius;

        OnDeathCloud(String key, JsonObject t) {
            super("on_death_cloud", key);
            effect = rl(t, "effect");
            duration = i(t, "duration", 100);
            amplifier = i(t, "amplifier", 0);
            radius = (float) d(t, "radius", 3);
            cloudDuration = Math.max(20, i(t, "cloud_duration", 200));
        }

        @Override
        public void onDeath(MinionEntity minion, BodyPartLocation location, DamageSource source) {
            Holder<MobEffect> holder = effect(effect);
            if (holder == null || !once(minion, location)) {
                return;
            }
            AreaEffectCloud cloud = new AreaEffectCloud(minion.level(), minion.getX(), minion.getY(), minion.getZ());
            cloud.setOwner(minion);
            cloud.setRadius(radius);
            cloud.setRadiusOnUse(-0.25F);
            cloud.setWaitTime(10);
            cloud.setDuration(cloudDuration);
            cloud.setRadiusPerTick(-radius / cloudDuration);
            cloud.addEffect(new MobEffectInstance(holder, duration, amplifier));
            minion.level().addFreshEntity(cloud);
        }

        @Override
        public Component description() {
            return Component.translatable(lang() + ".desc", effectName(effect, amplifier), num(radius));
        }
    }

    static final class Summon extends Base {
        final String entity;
        final int count, cooldown, maxAlive, lifetime;
        final String trigger;
        final double scale;

        Summon(String key, JsonObject t) {
            super("summon", key);
            entity = s(t, "entity", "spawnling:worm_small");
            count = Math.max(1, Math.min(6, i(t, "count", 1)));
            cooldown = i(t, "cooldown", 400);
            maxAlive = Math.max(1, Math.min(8, i(t, "max_alive", 2)));
            lifetime = Math.max(20, i(t, "lifetime", 600));
            trigger = s(t, "trigger", "combat");
            scale = d(t, "scale", -0.5);
        }

        @Override
        public void tick(MinionEntity minion, BodyPartLocation location) {
            if (trigger.equals("combat") && combatTarget(minion) != null && once(minion, location) && minion.cooldown(key, cooldown)) {
                summon(minion);
            }
        }

        @Override
        public float onHurt(MinionEntity minion, BodyPartLocation location, DamageSource source, float amount) {
            if (trigger.equals("hurt") && once(minion, location) && minion.cooldown(key, Math.max(20, cooldown))) {
                summon(minion);
            }
            return amount;
        }

        @Override
        public void onDeath(MinionEntity minion, BodyPartLocation location, DamageSource source) {
            if (trigger.equals("death") && once(minion, location)) {
                summon(minion);
            }
        }

        void summon(MinionEntity minion) {
            if (minion.isSpawnling() || !(minion.level() instanceof ServerLevel level)) {
                return;
            }
            if (!entity.startsWith("spawnling:")) {
                EntityType.byString(entity).ifPresent(type -> {
                    for (int n = 0; n < count; n++) {
                        Entity spawned = type.create(level);
                        if (spawned != null) {
                            spawned.moveTo(minion.getX(), minion.getY(0.6), minion.getZ(), minion.getRandom().nextFloat() * 360F, 0F);
                            level.addFreshEntity(spawned);
                        }
                    }
                });
                return;
            }
            NecroMob source = NecroMobRegistry.get(ResourceLocation.fromNamespaceAndPath(spawnlingNamespace(minion), entity.substring("spawnling:".length())));
            if (source == null) {
                return;
            }
            int alive = minion.spawnlings().size();
            for (int n = 0; n < count && alive < maxAlive; n++, alive++) {
                MinionEntity.spawnling(minion, source, lifetime, scale);
            }
        }

        /** Spawnlings come from the same mod as the summoning part's creature. */
        private String spawnlingNamespace(MinionEntity minion) {
            for (BodyPartLocation location : BodyPartLocation.values()) {
                NecroMob mob = minion.part(location);
                if (mob != null && mob.traits(location.type()).contains(this)) {
                    return mob.id().getNamespace();
                }
            }
            return Necromancy.MOD_ID;
        }

        Component what() {
            if (entity.startsWith("spawnling:")) {
                return Component.translatable("necromob.orespawn." + entity.substring("spawnling:".length()));
            }
            return EntityType.byString(entity).map(EntityType::getDescription).orElse(Component.literal(entity));
        }

        @Override
        public Component name() {
            return Component.translatable(lang(), what());
        }

        @Override
        public Component description() {
            return switch (trigger) {
                case "death" -> Component.translatable(lang() + ".desc.death", count, what());
                case "hurt" -> Component.translatable(lang() + ".desc.hurt", maxAlive, secs(lifetime));
                default -> Component.translatable(lang() + ".desc", maxAlive, secs(lifetime));
            };
        }
    }

    static final class Aura extends Base {
        final ResourceLocation effect;
        final int amplifier, duration, interval;
        final double radius;
        final String targets;
        final boolean needsLos;

        Aura(String key, JsonObject t) {
            super("aura", key);
            effect = rl(t, "effect");
            amplifier = i(t, "amplifier", 0);
            duration = i(t, "duration", 60);
            radius = d(t, "radius", 6);
            interval = Math.max(10, i(t, "interval", 100));
            targets = s(t, "targets", "enemies");
            needsLos = b(t, "needs_los", false);
        }

        @Override
        public void tick(MinionEntity minion, BodyPartLocation location) {
            if (minion.tickCount % interval != 0 || !once(minion, location) || minion.isSpawnling() && !targets.equals("enemies")) {
                return;
            }
            Holder<MobEffect> holder = effect(effect);
            if (holder == null) {
                return;
            }
            List<LivingEntity> victims = switch (targets) {
                case "owner" -> minion.getOwner() != null && minion.getOwner().distanceTo(minion) <= radius ? List.of(minion.getOwner()) : List.of();
                case "allies" -> minion.level().getEntitiesOfClass(LivingEntity.class, minion.getBoundingBox().inflate(radius),
                        e -> e != minion && minion.isFriendly(e));
                default -> minion.stance() == com.sirolf2009.necromancy.entity.MinionStance.PASSIVE ? List.of()
                        : minion.level().getEntitiesOfClass(LivingEntity.class, minion.getBoundingBox().inflate(radius),
                                e -> e != minion && e.isAlive() && !isBoss(e) && !minion.isFriendly(e)
                                        && (e instanceof Enemy || e == minion.getTarget() || e instanceof Player p && minion.isEnemyListed(p)));
            };
            for (LivingEntity victim : victims) {
                if (victim.distanceTo(minion) <= radius && (!needsLos || minion.getSensing().hasLineOfSight(victim))) {
                    victim.addEffect(new MobEffectInstance(holder, duration, amplifier), minion);
                }
            }
        }

        @Override
        public Component name() {
            return Component.translatable(lang(), effectName(effect, amplifier));
        }

        @Override
        public Component description() {
            return Component.translatable(lang() + ".desc." + targets, num(radius), secs(interval));
        }
    }

    static final class Pull extends Base {
        final double radius, strength;
        final int interval;
        final String targets;

        Pull(String key, JsonObject t) {
            super("pull", key);
            radius = d(t, "radius", 8);
            strength = d(t, "strength", 0.4);
            interval = Math.max(5, i(t, "interval", 40));
            targets = s(t, "targets", "target");
        }

        @Override
        public void tick(MinionEntity minion, BodyPartLocation location) {
            LivingEntity target = combatTarget(minion);
            if (target == null || minion.tickCount % interval != 0 || !once(minion, location)) {
                return;
            }
            List<LivingEntity> victims = targets.equals("enemies")
                    ? minion.level().getEntitiesOfClass(LivingEntity.class, minion.getBoundingBox().inflate(radius),
                            e -> e != minion && e.isAlive() && !minion.isFriendly(e) && (e instanceof Enemy || e == target))
                    : List.of(target);
            double mine = volume(minion);
            boolean pulled = false;
            for (LivingEntity victim : victims) {
                if (victim.distanceTo(minion) > radius || isBoss(victim) || volume(victim) >= mine) {
                    continue;
                }
                Vec3 dir = minion.position().subtract(victim.position()).normalize().scale(strength);
                victim.push(dir.x, dir.y * 0.5, dir.z);
                victim.hurtMarked = true;
                pulled = true;
            }
            if (pulled) {
                minion.playSound(SoundEvents.FISHING_BOBBER_RETRIEVE, 1F, 0.8F);
            }
        }

        private static double volume(Entity e) {
            return e.getBbWidth() * e.getBbWidth() * e.getBbHeight();
        }

        @Override
        public Component description() {
            return Component.translatable(lang() + ".desc", num(radius));
        }
    }

    static final class Produce extends Base {
        final List<Item> items = new ArrayList<>();
        final List<Integer> weights = new ArrayList<>();
        final List<ResourceLocation> itemIds = new ArrayList<>();
        final int minInterval, maxInterval, minCount, maxCount;
        @Nullable
        final ResourceLocation sound;

        Produce(String key, JsonObject t) {
            super("produce", key);
            for (JsonElement e : t.getAsJsonArray("items")) {
                JsonObject o = e.getAsJsonObject();
                itemIds.add(rl(o, "item"));
                weights.add(Math.max(1, i(o, "weight", 1)));
            }
            int[] interval = pair(t, "interval", 6000, 12000);
            minInterval = Math.max(100, interval[0]);
            maxInterval = Math.max(minInterval, interval[1]);
            int[] count = pair(t, "count", 1, 1);
            minCount = Math.max(1, count[0]);
            maxCount = Math.max(minCount, count[1]);
            sound = t.has("sound") && !t.get("sound").isJsonNull() ? rl(t, "sound") : null;
        }

        @Override
        public void tick(MinionEntity minion, BodyPartLocation location) {
            if (!once(minion, location) || minion.isSpawnling() || minion.getVehicle() instanceof AbstractMinecart || minion.getVehicle() instanceof Boat) {
                return;
            }
            double[] next = minion.traitState(key, 1);
            if (next[0] == 0) {
                next[0] = minion.tickCount + Mth.nextInt(minion.getRandom(), minInterval, maxInterval);
                return;
            }
            if (minion.tickCount < next[0]) {
                return;
            }
            next[0] = minion.tickCount + Mth.nextInt(minion.getRandom(), minInterval, maxInterval);
            int total = weights.stream().mapToInt(Integer::intValue).sum();
            int roll = minion.getRandom().nextInt(total);
            int pick = 0;
            while (roll >= weights.get(pick)) {
                roll -= weights.get(pick++);
            }
            Item item = BuiltInRegistries.ITEM.get(itemIds.get(pick));
            if (item == Items.AIR) {
                return;
            }
            minion.spawnAtLocation(new ItemStack(item, Mth.nextInt(minion.getRandom(), minCount, maxCount)));
            SoundEvent event = sound == null ? SoundEvents.CHICKEN_EGG : BuiltInRegistries.SOUND_EVENT.get(sound);
            if (event != null) {
                minion.playSound(event, 1F, (minion.getRandom().nextFloat() - minion.getRandom().nextFloat()) * 0.2F + 1F);
            }
        }

        @Override
        public Component name() {
            Item first = itemIds.isEmpty() ? Items.AIR : BuiltInRegistries.ITEM.get(itemIds.get(0));
            return Component.translatable(itemIds.size() > 1 ? lang() + ".more" : lang(), Component.translatable(first.getDescriptionId()));
        }

        @Override
        public Component description() {
            return Component.translatable(lang() + ".desc", minutes(minInterval), minutes(maxInterval));
        }
    }

    static final class InteractYield extends Base {
        final ResourceLocation useItem, result;
        final int cooldown, minCount, maxCount;
        final String consume;
        @Nullable
        final ResourceLocation sound;

        InteractYield(String key, JsonObject t) {
            super("interact_yield", key);
            useItem = rl(t, "use_item");
            result = rl(t, "result");
            cooldown = i(t, "cooldown", 1200);
            int[] count = pair(t, "count", 1, 1);
            minCount = Math.max(1, count[0]);
            maxCount = Math.max(minCount, count[1]);
            consume = s(t, "consume", "replace");
            sound = t.has("sound") && !t.get("sound").isJsonNull() ? rl(t, "sound") : null;
        }

        @Override
        public InteractionResult onInteract(MinionEntity minion, BodyPartLocation location, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            Item use = BuiltInRegistries.ITEM.get(useItem);
            Item give = BuiltInRegistries.ITEM.get(result);
            if (use == Items.AIR || give == Items.AIR || !stack.is(use) || minion.isCoolingDown(key)) {
                return InteractionResult.PASS;
            }
            minion.setCooldown(key, cooldown);
            int count = Mth.nextInt(minion.getRandom(), minCount, maxCount);
            switch (consume) {
                case "replace" -> player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(give, count)));
                case "damage" -> {
                    minion.spawnAtLocation(new ItemStack(give, count));
                    stack.hurtAndBreak(1, player, hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND);
                }
                default -> minion.spawnAtLocation(new ItemStack(give, count));
            }
            SoundEvent event = sound == null ? SoundEvents.ITEM_PICKUP : BuiltInRegistries.SOUND_EVENT.get(sound);
            if (event != null) {
                minion.playSound(event, 1F, 1F);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public Component name() {
            return Component.translatable(lang(), Component.translatable(BuiltInRegistries.ITEM.get(result).getDescriptionId()));
        }

        @Override
        public Component description() {
            return Component.translatable(lang() + ".desc", Component.translatable(BuiltInRegistries.ITEM.get(useItem).getDescriptionId()), secs(cooldown));
        }
    }

    /** Breaks matching blocks near its post; the work itself is {@link com.sirolf2009.necromancy.entity.goal.MinionForageGoal}. */
    public static final class Forager extends Base {
        public final TagKey<Block> blocks;
        public final int radius, interval;
        public final boolean eat;
        public final double heal;
        public final boolean needsWork;
        @Nullable
        public final ResourceLocation replaceWith;

        Forager(String key, JsonObject t) {
            super("forager", key);
            String tag = s(t, "blocks", "#minecraft:logs");
            blocks = TagKey.create(Registries.BLOCK, ResourceLocation.parse(tag.startsWith("#") ? tag.substring(1) : tag));
            radius = Math.max(2, Math.min(16, i(t, "radius", 8)));
            interval = Math.max(10, i(t, "interval", 60));
            eat = "eat".equals(s(t, "action", "harvest"));
            heal = d(t, "heal", 0);
            needsWork = "work".equals(s(t, "needs_order", "work"));
            replaceWith = t.has("replace_with") && !t.get("replace_with").isJsonNull() ? rl(t, "replace_with") : null;
        }

        @Override
        public Component name() {
            return Component.translatable(lang() + (eat ? ".eat" : ".harvest"), Component.translatable(lang() + ".blocks." + blocks.location().getPath()));
        }

        @Override
        public Component description() {
            return eat ? Component.translatable(lang() + ".desc.eat", Component.translatable(lang() + ".blocks." + blocks.location().getPath()), num(heal))
                    : Component.translatable(lang() + ".desc", Component.translatable(lang() + ".blocks." + blocks.location().getPath()), radius);
        }
    }

    /** A tooltip line for the mount settings; the riding itself is in MinionEntity. */
    static final class MountTrait extends Base {
        final NecroMob.Mount mount;

        MountTrait(String key, JsonObject t) {
            super("mount", key);
            mount = mount(t);
        }

        @Override
        public Component name() {
            return Component.translatable(mount.water() ? lang() + ".water" : lang());
        }

        @Override
        public Component description() {
            MutableComponent text = Component.translatable(mount.direct() ? lang() + ".desc.direct" : lang() + ".desc.stick");
            if (mount.seats() > 1) {
                text.append(" ").append(Component.translatable(lang() + ".desc.seats", mount.seats()));
            }
            if (mount.jump() > 0) {
                text.append(" ").append(Component.translatable(lang() + ".desc.jump"));
            }
            return text;
        }
    }

    /** Wings: the falling itself is slowed in {@link #glide(MinionEntity)}, on both sides (a ridden Minion moves on the client). */
    public static final class Glide extends Base {
        final double fallSpeed, horizontalBoost;
        final int needs;

        Glide(String key, JsonObject t) {
            super("glide", key);
            fallSpeed = Math.max(0.02, d(t, "fall_speed", 0.1));
            needs = Math.max(1, i(t, "needs", 2));
            horizontalBoost = d(t, "horizontal_boost", 0);
        }

        @Override
        public Component description() {
            return Component.translatable(needs > 1 ? lang() + ".desc.pair" : lang() + ".desc");
        }
    }

    /** Slows a falling Minion that has enough gliding parts. Called every tick on both sides. */
    public static void glide(MinionEntity minion) {
        int parts = 0;
        int needs = 0;
        double fall = Double.MAX_VALUE;
        double boost = 0;
        for (BodyPartLocation location : BodyPartLocation.values()) {
            NecroMob mob = minion.part(location);
            if (mob == null) {
                continue;
            }
            for (PartTrait trait : mob.traits(location.type())) {
                if (trait instanceof Glide glide) {
                    parts++;
                    needs = Math.max(needs, glide.needs);
                    fall = Math.min(fall, glide.fallSpeed);
                    boost = Math.max(boost, glide.horizontalBoost);
                }
            }
        }
        if (parts == 0 || parts < needs || minion.onGround() || minion.isInLiquid() || minion.onClimbable() || minion.isPassenger()) {
            return;
        }
        Vec3 motion = minion.getDeltaMovement();
        if (motion.y >= -fall) {
            return;
        }
        double x = motion.x;
        double z = motion.z;
        if (boost > 0) {
            float yaw = minion.yBodyRot * Mth.DEG_TO_RAD;
            x -= Mth.sin(yaw) * boost;
            z += Mth.cos(yaw) * boost;
            double h = Math.sqrt(x * x + z * z);
            if (h > 0.4) {
                x *= 0.4 / h;
                z *= 0.4 / h;
            }
        }
        minion.setDeltaMovement(x, -fall, z);
        minion.resetFallDistance();
    }

    static final class Environment extends Base {
        final String condition;
        final float damage;
        final int interval;
        final Map<String, Double> multipliers = new java.util.LinkedHashMap<>();
        final ResourceLocation modifier;

        Environment(String key, ResourceLocation mob, PartType type, JsonObject t) {
            super("environment", key);
            condition = s(t, "condition", "dry");
            damage = (float) d(t, "damage", 0);
            interval = Math.max(10, i(t, "interval", 40));
            if (t.has("attribute_multipliers")) {
                for (Map.Entry<String, JsonElement> e : t.getAsJsonObject("attribute_multipliers").entrySet()) {
                    multipliers.put(e.getKey(), e.getValue().getAsDouble());
                }
            }
            modifier = Necromancy.id("env/" + mob.getNamespace() + "/" + mob.getPath() + "/" + type.getSerializedName() + "/" + key.substring(key.lastIndexOf('.') + 1));
        }

        boolean holds(MinionEntity minion) {
            Level level = minion.level();
            BlockPos pos = minion.blockPosition();
            return switch (condition) {
                case "water_or_rain" -> minion.isInWaterRainOrBubble();
                case "dry" -> !minion.isInWaterRainOrBubble();
                case "sunlight" -> minion.isInSunlight();
                case "daylight" -> level.isDay() && level.getBrightness(LightLayer.SKY, pos) >= 12;
                case "night" -> !level.isDay();
                case "hot_biome" -> level.getBiome(pos).value().getBaseTemperature() > 1.0F;
                case "cold_biome" -> level.getBiome(pos).value().coldEnoughToSnow(pos);
                case "in_lava" -> minion.isInLava();
                case "nether" -> level.dimension() == Level.NETHER;
                default -> false;
            };
        }

        @Override
        public void tick(MinionEntity minion, BodyPartLocation location) {
            if (minion.tickCount % 10 != 0 || !once(minion, location)) {
                return;
            }
            boolean on = holds(minion);
            for (Map.Entry<String, Double> e : multipliers.entrySet()) {
                Holder<Attribute> attribute = switch (e.getKey()) {
                    case "attack_damage" -> Attributes.ATTACK_DAMAGE;
                    case "movement_speed" -> Attributes.MOVEMENT_SPEED;
                    case "follow_range" -> Attributes.FOLLOW_RANGE;
                    case "armor" -> Attributes.ARMOR;
                    default -> null;
                };
                AttributeInstance instance = attribute == null ? null : minion.getAttribute(attribute);
                if (instance == null) {
                    continue;
                }
                boolean has = instance.hasModifier(modifier);
                if (on && !has) {
                    instance.addTransientModifier(new AttributeModifier(modifier, e.getValue() - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                } else if (!on && has) {
                    instance.removeModifier(modifier);
                }
            }
            if (on && damage > 0 && minion.tickCount % interval < 10) {
                minion.hurt(minion.damageSources().generic(), damage);
            }
        }

        @Override
        public Component name() {
            Component when = Component.translatable("trait.necromancy.condition." + condition);
            boolean good = damage <= 0 && !multipliers.isEmpty() && multipliers.values().stream().allMatch(v -> v >= 1);
            boolean bad = damage > 0 || multipliers.values().stream().allMatch(v -> v < 1);
            return Component.translatable(lang() + (good ? ".thrives" : bad ? ".suffers" : ".changes"), when);
        }

        @Override
        public Component description() {
            MutableComponent text = Component.empty();
            boolean first = true;
            for (Map.Entry<String, Double> e : multipliers.entrySet()) {
                long percent = Math.round((e.getValue() - 1) * 100);
                if (!first) {
                    text.append(", ");
                }
                first = false;
                text.append((percent > 0 ? "+" : "") + percent + "% ").append(Component.translatable("trait.necromancy.stat." + e.getKey()));
            }
            if (damage > 0) {
                if (!first) {
                    text.append(", ");
                }
                text.append(Component.translatable(lang() + ".damage", num(damage), secs(interval)));
            }
            return text;
        }
    }

    static final class EmitsLight extends Base {
        final int level;
        final boolean always;

        EmitsLight(String key, JsonObject t) {
            super("emits_light", key);
            level = Mth.clamp(i(t, "level", 10), 1, 15);
            always = "always".equals(s(t, "condition", "dark"));
        }

        @Override
        public void tick(MinionEntity minion, BodyPartLocation location) {
            if (minion.tickCount % 10 != 0 || !once(minion, location)) {
                return;
            }
            Level world = minion.level();
            BlockPos eyes = BlockPos.containing(minion.getX(), minion.getEyeY(), minion.getZ());
            BlockPos old = minion.lightPos();
            boolean wanted = !minion.isOrderedToSit() && minion.isAlive();
            if (wanted && !always) {
                BlockState there = world.getBlockState(eyes);
                int light = there.is(Blocks.LIGHT) ? world.getBrightness(LightLayer.BLOCK, eyes) - there.getValue(LightBlock.LEVEL) : world.getBrightness(LightLayer.BLOCK, eyes);
                wanted = Math.max(light, world.getBrightness(LightLayer.SKY, eyes) - world.getSkyDarken()) < level;
            }
            if (old != null && (!wanted || !old.equals(eyes))) {
                minion.clearLight();
            }
            if (wanted && !eyes.equals(minion.lightPos()) && world.getBlockState(eyes).isAir()) {
                world.setBlock(eyes, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, level), 3);
                minion.setLightPos(eyes);
            }
        }

        @Override
        public Component description() {
            return Component.translatable(lang() + ".desc", level);
        }
    }

    static final class Transfuse extends Base {
        final float amount;
        final int interval;
        final double ownerThreshold, selfFloor;

        Transfuse(String key, JsonObject t) {
            super("transfuse", key);
            amount = (float) d(t, "amount", 1);
            interval = Math.max(10, i(t, "interval", 40));
            ownerThreshold = d(t, "owner_threshold", 0.5);
            selfFloor = d(t, "self_floor", 0.3);
        }

        @Override
        public void tick(MinionEntity minion, BodyPartLocation location) {
            if (minion.tickCount % interval != 0 || !once(minion, location) || !(minion.getOwner() instanceof Player owner)) {
                return;
            }
            if (owner.isDeadOrDying() || owner.distanceTo(minion) > 8 || owner.getHealth() >= owner.getMaxHealth() * ownerThreshold
                    || minion.getHealth() <= minion.getMaxHealth() * selfFloor) {
                return;
            }
            minion.hurt(minion.damageSources().magic(), amount);
            owner.heal(amount);
            if (minion.level() instanceof ServerLevel level) {
                Vec3 from = minion.position().add(0, minion.getBbHeight() * 0.6, 0);
                Vec3 to = owner.position().add(0, owner.getBbHeight() * 0.6, 0);
                for (int n = 0; n < 6; n++) {
                    Vec3 p = from.lerp(to, n / 5.0);
                    level.sendParticles(ParticleTypes.HEART, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
                }
            }
        }

        @Override
        public Component description() {
            return Component.translatable(lang() + ".desc", pct(ownerThreshold));
        }
    }

    // ------------------------------------------------------------------ helpers

    /** The Minion's live target when it is free to fight (not sitting), else null. */
    @Nullable
    static LivingEntity combatTarget(MinionEntity minion) {
        LivingEntity target = minion.getTarget();
        return target != null && target.isAlive() && !minion.isOrderedToSit() ? target : null;
    }

    static void extraHit(MinionEntity minion, LivingEntity target, float amount) {
        if (amount > 0) {
            target.invulnerableTime = 0;
            target.hurt(minion.damageSources().mobAttack(minion), amount);
        }
    }

    @Nullable
    static LivingEntity attacker(MinionEntity minion, DamageSource source, boolean meleeOnly) {
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == minion || minion.isFriendly(attacker)) {
            return null;
        }
        if (meleeOnly && source.getDirectEntity() != attacker) {
            return null;
        }
        return attacker;
    }

    static boolean isBoss(Entity entity) {
        return entity.getType().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES);
    }

    @Nullable
    static Holder<MobEffect> effect(ResourceLocation id) {
        return BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);
    }

    static Component effectName(ResourceLocation id, int amplifier) {
        Holder<MobEffect> holder = effect(id);
        MutableComponent name = holder == null ? Component.literal(id.toString()) : Component.translatable(holder.value().getDescriptionId());
        return amplifier > 0 ? name.append(" ").append(Component.translatable("enchantment.level." + (amplifier + 1))) : name;
    }

    static String pct(double fraction) {
        return Math.round(fraction * 100) + "%";
    }

    static String secs(int ticks) {
        return num(ticks / 20.0);
    }

    static String minutes(int ticks) {
        return num(Math.round(ticks / 120.0) / 10.0);
    }

    static String num(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    static ResourceLocation rl(JsonObject t, String key) {
        return ResourceLocation.parse(t.get(key).getAsString());
    }

    static String s(JsonObject t, String key, String fallback) {
        return t.has(key) && !t.get(key).isJsonNull() ? t.get(key).getAsString() : fallback;
    }

    static double d(JsonObject t, String key, double fallback) {
        return t.has(key) && !t.get(key).isJsonNull() ? t.get(key).getAsDouble() : fallback;
    }

    static int i(JsonObject t, String key, int fallback) {
        return t.has(key) && !t.get(key).isJsonNull() ? Math.round(t.get(key).getAsFloat()) : fallback;
    }

    static boolean b(JsonObject t, String key, boolean fallback) {
        return t.has(key) && !t.get(key).isJsonNull() ? t.get(key).getAsBoolean() : fallback;
    }

    static int[] pair(JsonObject t, String key, int a, int b) {
        if (!t.has(key) || t.get(key).isJsonNull()) {
            return new int[] { a, b };
        }
        JsonElement e = t.get(key);
        if (e.isJsonArray()) {
            JsonArray array = e.getAsJsonArray();
            return new int[] { array.get(0).getAsInt(), array.get(array.size() - 1).getAsInt() };
        }
        return new int[] { e.getAsInt(), e.getAsInt() };
    }
}
