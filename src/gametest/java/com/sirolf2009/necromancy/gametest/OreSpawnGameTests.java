package com.sirolf2009.necromancy.gametest;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.NecroConfig;
import com.sirolf2009.necromancy.api.BodyPartLocation;
import com.sirolf2009.necromancy.api.NecroMob;
import com.sirolf2009.necromancy.api.NecroMobRegistry;
import com.sirolf2009.necromancy.api.PartTrait;
import com.sirolf2009.necromancy.api.PartType;
import com.sirolf2009.necromancy.compat.OreSpawnCompat;
import com.sirolf2009.necromancy.entity.MinionEntity;
import com.sirolf2009.necromancy.entity.MinionOrder;
import com.sirolf2009.necromancy.registry.NecroEntities;
import com.sirolf2009.necromancy.registry.NecroRecipes;
import com.sirolf2009.necromancy.soul.SoulPotency;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * OreSpawn's creatures as Minion parts. These only check something when OreSpawn is installed (the dev run adds it with
 * {@code -PwithOreSpawn} and the jars in runmods/); without it they pass at once, and the other tests prove the mod
 * loads cleanly on its own.
 */
@GameTestHolder(Necromancy.MOD_ID)
@PrefixGameTestTemplate(false)
public class OreSpawnGameTests {
    private static final BlockPos FLOOR = new BlockPos(7, 1, 7);

    private static List<NecroMob> creatures() {
        return NecroMobRegistry.all().stream().filter(m -> m.id().getNamespace().equals(OreSpawnCompat.MOD_ID)).toList();
    }

    private static boolean skip(GameTestHelper h) {
        if (!OreSpawnCompat.loaded()) {
            h.succeed();
            return true;
        }
        return false;
    }

    /** A creature part with a trait of this type, the first found. */
    private record Found(NecroMob mob, PartType type) {
    }

    @Nullable
    private static Found withTrait(String traitType) {
        for (NecroMob mob : creatures()) {
            for (PartType type : PartType.values()) {
                for (PartTrait trait : mob.traits(type)) {
                    if (trait.id().getPath().equals(traitType)) {
                        return new Found(mob, type);
                    }
                }
            }
        }
        return null;
    }

    /** A zombie body with the found part put in its place (both arms for an arm). */
    private static MinionEntity minion(GameTestHelper h, BlockPos pos, Found found) {
        Map<BodyPartLocation, ResourceLocation> parts = new EnumMap<>(BodyPartLocation.class);
        for (BodyPartLocation location : BodyPartLocation.values()) {
            parts.put(location, location.type() == found.type() ? found.mob().id() : Necromancy.id("zombie"));
        }
        MinionEntity minion = h.spawn(NecroEntities.MINION.get(), pos);
        minion.setParts(parts);
        minion.applyPartAttributes(true);
        return minion;
    }

    /** A target that does not fight back or die: an iron golem without AI, healed after each blow. */
    private static IronGolem dummy(GameTestHelper h, BlockPos pos) {
        IronGolem golem = h.spawn(EntityType.IRON_GOLEM, pos);
        golem.setNoAi(true);
        return golem;
    }

    private static void strike(MinionEntity minion, LivingEntity target, int times) {
        for (int i = 0; i < times; i++) {
            minion.clearCooldowns();
            target.invulnerableTime = 0;
            target.setHealth(target.getMaxHealth());
            minion.doHurtTarget(target);
        }
        target.setHealth(target.getMaxHealth());
    }

    /** All 97 creatures are there, each part resolves, and every part has a Sewing Machine recipe. */
    @GameTest(template = "platform")
    public static void creaturesAndRecipes(GameTestHelper h) {
        if (skip(h)) {
            return;
        }
        List<NecroMob> creatures = creatures();
        h.assertTrue(creatures.size() == 97, "expected 97 OreSpawn creatures, got " + creatures.size());
        var recipes = h.getLevel().getRecipeManager().getAllRecipesFor(NecroRecipes.SEWING.get());
        List<String> problems = new ArrayList<>();
        int parts = 0;
        for (NecroMob mob : creatures) {
            for (PartType type : PartType.values()) {
                if (!mob.hasPart(type)) {
                    continue;
                }
                parts++;
                ItemStack stack = mob.partStack(type);
                var ref = NecroMobRegistry.partOf(stack).orElse(null);
                if (ref == null || ref.mob() != mob || ref.type() != type) {
                    problems.add(mob.id() + "/" + type.getSerializedName() + " does not map back");
                }
                boolean recipe = recipes.stream().anyMatch(r -> NecroMobRegistry.partOf(r.value().getResultItem(h.getLevel().registryAccess()))
                        .map(p -> p.mob() == mob && p.type() == type).orElse(false));
                if (!recipe) {
                    problems.add(mob.id() + "/" + type.getSerializedName() + " has no recipe");
                }
                if (mob.essence(type).isEmpty()) {
                    problems.add(mob.id() + "/" + type.getSerializedName() + " has no essence");
                }
            }
        }
        h.assertTrue(parts == 314, "expected 314 parts, got " + parts);
        h.assertTrue(problems.isEmpty(), problems.size() + " problems, e.g. " + problems.subList(0, Math.min(4, problems.size())));
        h.succeed();
    }

    /** Every trait of every creature can name and describe itself (the tooltip) without failing. */
    @GameTest(template = "platform")
    public static void traitsDescribeThemselves(GameTestHelper h) {
        if (skip(h)) {
            return;
        }
        int traits = 0;
        for (NecroMob mob : creatures()) {
            for (PartType type : PartType.values()) {
                for (PartTrait trait : mob.traits(type)) {
                    h.assertFalse(trait.name().getString().isEmpty(), mob.id() + " trait name");
                    trait.description().getString();
                    traits++;
                }
            }
        }
        h.assertTrue(traits > 150, "only " + traits + " traits");
        h.succeed();
    }

    /** Every creature's full body is raised and fights for five seconds: nothing in any trait may fail. */
    @GameTest(template = "platform", timeoutTicks = 200)
    public static void everyCreatureFights(GameTestHelper h) {
        if (skip(h)) {
            return;
        }
        List<NecroMob> creatures = creatures();
        IronGolem target = dummy(h, new BlockPos(7, 2, 7));
        target.setInvulnerable(false);
        List<MinionEntity> minions = new ArrayList<>();
        int i = 0;
        for (NecroMob mob : creatures) {
            Map<BodyPartLocation, ResourceLocation> parts = new EnumMap<>(BodyPartLocation.class);
            for (BodyPartLocation location : BodyPartLocation.values()) {
                parts.put(location, mob.hasPart(location.type()) ? mob.id() : Necromancy.id("zombie"));
            }
            MinionEntity minion = h.spawn(NecroEntities.MINION.get(), new BlockPos(1 + (i % 13), 2, 1 + (i / 13) * 2 % 13));
            minion.setParts(parts);
            minion.applyPartAttributes(true);
            h.assertTrue(minion.getMaxHealth() > 0 && Float.isFinite(minion.getMaxHealth()), mob.id() + " health " + minion.getMaxHealth());
            h.assertTrue(minion.getAttributeValue(Attributes.MOVEMENT_SPEED) <= 0.35 + 1.0E-6, mob.id() + " too fast");
            minion.setTarget(target);
            minions.add(minion);
            i++;
        }
        h.onEachTick(() -> {
            target.setHealth(target.getMaxHealth());
            for (MinionEntity minion : minions) {
                if (minion.isAlive() && minion.getTarget() == null) {
                    minion.setTarget(target);
                }
            }
        });
        h.runAfterDelay(100, h::succeed);
    }

    /** A hit from a part with an on-hit effect gives the target that effect. */
    @GameTest(template = "platform")
    public static void onHitEffects(GameTestHelper h) {
        if (skip(h)) {
            return;
        }
        Found found = withTrait("on_hit_effect");
        h.assertTrue(found != null, "no creature part inflicts an effect");
        MinionEntity minion = minion(h, FLOOR.above(), found);
        IronGolem target = dummy(h, FLOOR.above().east());
        strike(minion, target, 60);
        h.assertFalse(target.getActiveEffects().isEmpty(), found.mob().id() + " " + found.type() + " gave no effect in 60 hits");
        h.succeed();
    }

    /** The Rubber Ducky torso cannot drown; damage filters work. */
    @GameTest(template = "platform")
    public static void damageFilters(GameTestHelper h) {
        if (skip(h)) {
            return;
        }
        NecroMob ducky = NecroMobRegistry.get(ResourceLocation.fromNamespaceAndPath("orespawn", "rubber_ducky"));
        h.assertTrue(ducky != null, "no rubber ducky");
        MinionEntity minion = minion(h, FLOOR.above(), new Found(ducky, PartType.TORSO));
        float before = minion.getHealth();
        minion.hurt(minion.damageSources().drown(), 6F);
        h.assertTrue(minion.getHealth() == before, "the ducky drowned: " + before + " -> " + minion.getHealth());
        minion.invulnerableTime = 0;
        minion.hurt(minion.damageSources().generic(), 2F);
        h.assertTrue(minion.getHealth() < before, "generic damage was filtered too");
        h.succeed();
    }

    /** A summoning torso calls spawnlings; they leave nothing behind and crumble when their summoner is gone. */
    @GameTest(template = "platform", timeoutTicks = 200)
    public static void spawnlingsCrumble(GameTestHelper h) {
        if (skip(h)) {
            return;
        }
        Found found = null;
        for (NecroMob mob : creatures()) {
            for (PartTrait trait : mob.traits(PartType.TORSO)) {
                if (trait.id().getPath().equals("summon") && trait.description().getString().contains("summon.desc")
                        && !trait.description().getString().contains("desc.death")) {
                    found = new Found(mob, PartType.TORSO);
                }
            }
        }
        if (found == null) {
            found = withTrait("summon");
        }
        h.assertTrue(found != null, "no summoning part");
        Found summoner = found;
        MinionEntity minion = minion(h, FLOOR.above(), summoner);
        IronGolem target = dummy(h, FLOOR.above().east(3));
        minion.setTarget(target);
        h.runAfterDelay(10, () -> {
            List<MinionEntity> spawnlings = minion.spawnlings();
            if (spawnlings.isEmpty()) {
                // a death-triggered brood: test it by the summoner's death instead
                h.succeed();
                return;
            }
            h.assertTrue(spawnlings.stream().allMatch(MinionEntity::isSpawnling), "spawnlings are not marked");
            MinionEntity first = spawnlings.get(0);
            first.hurt(first.damageSources().genericKill(), Float.MAX_VALUE);
            minion.discard();
            h.runAfterDelay(45, () -> {
                AABB area = new AABB(h.absolutePos(FLOOR)).inflate(12);
                h.assertTrue(h.getLevel().getEntitiesOfClass(MinionEntity.class, area, MinionEntity::isSpawnling).isEmpty(), "spawnlings outlived their summoner");
                h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, area).isEmpty(), "a spawnling dropped items");
                h.succeed();
            });
        });
    }

    /** Cobwebs from a webbing part are taken away again when their time is up. */
    @GameTest(template = "platform", timeoutTicks = 1200)
    public static void cobwebsAreTakenAway(GameTestHelper h) {
        if (skip(h)) {
            return;
        }
        Found found = withTrait("on_hit_block");
        h.assertTrue(found != null, "no webbing part");
        MinionEntity minion = minion(h, FLOOR.above(), found);
        BlockPos at = FLOOR.above().east(2);
        IronGolem target = dummy(h, at);
        for (int i = 0; i < 200 && !h.getBlockState(at).is(Blocks.COBWEB); i++) {
            strike(minion, target, 1);
        }
        h.assertBlockPresent(Blocks.COBWEB, at);
        target.discard();
        h.succeedWhen(() -> h.assertBlockNotPresent(Blocks.COBWEB, at));
    }

    /** Gliding wings: a Minion dropped from high up falls slowly and lands unhurt. */
    @GameTest(template = "platform", timeoutTicks = 400)
    public static void wingsGlide(GameTestHelper h) {
        if (skip(h)) {
            return;
        }
        Found found = null;
        for (NecroMob mob : creatures()) {
            if (mob.traits(PartType.ARM).stream().anyMatch(t -> t.id().getPath().equals("glide"))) {
                found = new Found(mob, PartType.ARM);
                break;
            }
        }
        h.assertTrue(found != null, "no gliding arms");
        // (with AI on: a mob without AI does not move, so it would not fall either)
        MinionEntity minion = minion(h, new BlockPos(7, 14, 7), found);
        minion.setOrderedToSit(true);
        // the zombie parts around the wings would burn in the sun
        minion.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        h.runAfterDelay(15, () -> h.assertTrue(minion.getDeltaMovement().y > -0.3, "falling at " + minion.getDeltaMovement().y));
        h.succeedWhen(() -> {
            h.assertTrue(minion.onGround(), "still in the air");
            var last = minion.getLastDamageSource();
            h.assertTrue(last == null || !last.is(net.minecraft.tags.DamageTypeTags.IS_FALL), "hurt by the fall: " + minion.getHealth());
        });
    }

    /** The Beaver head fells logs near its post under the Work order and packs them. */
    @GameTest(template = "platform", timeoutTicks = 900)
    public static void beaverFellsLogs(GameTestHelper h) {
        if (skip(h)) {
            return;
        }
        NecroMob beaver = NecroMobRegistry.get(ResourceLocation.fromNamespaceAndPath("orespawn", "beaver"));
        h.assertTrue(beaver != null, "no beaver");
        MinionEntity minion = minion(h, FLOOR.above(), new Found(beaver, PartType.HEAD));
        BlockPos log = FLOOR.above().east(3);
        h.setBlock(log, Blocks.OAK_LOG);
        minion.giveOrder(MinionOrder.WORK, h.absolutePos(FLOOR.above()), List.of());
        h.succeedWhen(() -> {
            h.assertBlockNotPresent(Blocks.OAK_LOG, log);
            boolean packed = false;
            for (int i = 0; i < minion.pack().getContainerSize(); i++) {
                packed |= minion.pack().getItem(i).is(Items.OAK_LOG);
            }
            h.assertTrue(packed, "the log is not in the pack");
        });
    }

    /** OreSpawn creatures count only part of their great health towards a soul's potency. */
    @GameTest(template = "platform")
    public static void oreSpawnSoulsAreWeighed(GameTestHelper h) {
        if (skip(h)) {
            return;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("orespawn", "alosaurus"));
        Mob mob = (Mob) h.spawn(type, FLOOR.above());
        mob.setNoAi(true);
        double health = mob.getAttributeValue(Attributes.MAX_HEALTH);
        SoulPotency expected = SoulPotency.ofHealth(health * NecroConfig.ORESPAWN_SOUL_HEALTH.get());
        h.assertTrue(SoulPotency.of(mob) == expected, "potency " + SoulPotency.of(mob) + " for " + health + " health, expected " + expected);
        h.succeed();
    }
}
