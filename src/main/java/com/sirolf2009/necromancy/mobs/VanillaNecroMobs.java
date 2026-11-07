package com.sirolf2009.necromancy.mobs;

import static com.sirolf2009.necromancy.api.BodyPartLocation.HEAD;
import static com.sirolf2009.necromancy.api.BodyPartLocation.LEGS;
import static com.sirolf2009.necromancy.api.BodyPartLocation.TORSO;
import static com.sirolf2009.necromancy.api.PartType.ARM;

import java.util.function.Supplier;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.api.NecroMob;
import com.sirolf2009.necromancy.api.NecroMobRegistry;
import com.sirolf2009.necromancy.api.PartStats;
import com.sirolf2009.necromancy.api.PartType;
import com.sirolf2009.necromancy.registry.NecroItems;
import com.sirolf2009.necromancy.registry.NecroSounds;
import com.sirolf2009.necromancy.trait.Traits;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Every creature that can lend its body to a Minion. The 17 of the original mod keep their 1.7.1 stats (sirolf2009's
 * NecroEntity* classes) apart from the Wolf head; parts gained traits, armour and weights (docs/REVIEW.md #1 and #5).
 * Ten modern creatures and the Night Crawler joined (REVIEW #4 and #13). Heads the original took from vanilla (Creeper,
 * Skeleton, Zombie) are still the vanilla mob heads, and so is the Wither Skeleton's.
 */
public final class VanillaNecroMobs {
    private VanillaNecroMobs() {
    }

    public static void register() {
        // ---------------- the original seventeen ----------------
        sewn("cave_spider", () -> Ingredient.of(Items.STRING)).weight(1).shape(0.3F, 0.35F, 0.7F)
                .stats(HEAD, PartStats.of(0.5, 1, 0, 0, 0.5))
                .stats(TORSO, PartStats.of(2, 0, 0, 0, 0))
                .stats(LEGS, PartStats.of(0.5, 0, 1, 2, 0.5))
                .trait(PartType.HEAD, Traits.VENOM)
                .trait(PartType.LEGS, Traits.CLIMBER)
                .attribute(PartType.LEGS, Attributes.SCALE, -0.15)
                .saddleable(0F)
                .sounds(sound(SoundEvents.SPIDER_AMBIENT), sound(SoundEvents.SPIDER_HURT), sound(SoundEvents.SPIDER_DEATH));
        sewn("chicken", () -> Ingredient.of(Items.CHICKEN)).weight(1).shape(0.3F, 0.4F, 0.4F)
                .stats(HEAD, PartStats.of(0.5, 1, 0, 0, 0.25))
                .stats(TORSO, PartStats.of(0.25, 0, 0, 0, 0))
                .armStats(PartStats.of(0.1, 0, 0, 0, 0))
                .stats(LEGS, PartStats.of(0.1, 0, 0, 0.5, 0))
                .trait(PartType.TORSO, Traits.LAYS_EGGS)
                .trait(ARM, Traits.flag("wings"))
                .attribute(ARM, Attributes.GRAVITY, -0.02)
                .attribute(ARM, Attributes.FALL_DAMAGE_MULTIPLIER, -0.5)
                .sounds(sound(SoundEvents.CHICKEN_AMBIENT), sound(SoundEvents.CHICKEN_HURT), sound(SoundEvents.CHICKEN_DEATH));
        sewn("cow", () -> Ingredient.of(Items.BEEF)).weight(1).shape(0.75F, 0.6F, 0.9F)
                .stats(HEAD, PartStats.of(0.5, 1, 0, 0, 0))
                .stats(TORSO, PartStats.of(1, 0, 0, 0, 0))
                .armStats(PartStats.of(0.25, 0, 0, 0, 0.25))
                .stats(LEGS, PartStats.of(0.25, 0, 1, 3, 0))
                .trait(PartType.TORSO, Traits.MILKABLE)
                .saddleable(0F)
                .sounds(sound(SoundEvents.COW_AMBIENT), sound(SoundEvents.COW_HURT), sound(SoundEvents.COW_DEATH));
        sewn("creeper", () -> Ingredient.of(Items.GUNPOWDER)).weight(2).shape(0.375F, 0.75F, 0.6F)
                .foundPart(PartType.HEAD, () -> Items.CREEPER_HEAD)
                .stats(HEAD, PartStats.of(0.5, 1, 0, 0, 0))
                .stats(TORSO, PartStats.of(1, 0, 0, 0, 0))
                .stats(LEGS, PartStats.of(0.25, 0, 3, 3, 0))
                .trait(PartType.TORSO, Traits.VOLATILE)
                .attribute(PartType.LEGS, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 0.5)
                .sounds(sound(SoundEvents.CREEPER_PRIMED), sound(SoundEvents.CREEPER_HURT), sound(SoundEvents.CREEPER_DEATH));
        sewn("enderman", () -> Ingredient.of(Items.ENDER_PEARL)).weight(3).shape(1.4F, 0.75F, 0.6F)
                .stats(HEAD, PartStats.of(1, 1, 1, 1, 0.5))
                .stats(TORSO, PartStats.of(4, 0, 1, 0, 0))
                .armStats(PartStats.of(1, 0, 0, 0, 1.5))
                .stats(LEGS, PartStats.of(1, 0, 4, 3, 0))
                .trait(PartType.HEAD, Traits.BLINK)
                .trait(PartType.HEAD, Traits.WATER_HURTS).trait(PartType.TORSO, Traits.WATER_HURTS)
                .trait(ARM, Traits.WATER_HURTS).trait(PartType.LEGS, Traits.WATER_HURTS)
                .sounds(sound(SoundEvents.ENDERMAN_AMBIENT), sound(SoundEvents.ENDERMAN_HURT), sound(SoundEvents.ENDERMAN_DEATH));
        sewn("iron_golem", () -> Ingredient.of(Items.IRON_BLOCK)).weight(4).shape(0.8F, 0.9F, 1.2F)
                .essence(PartType.HEAD, () -> Ingredient.of(Items.CARVED_PUMPKIN))
                .stats(HEAD, PartStats.of(1, 1, 2, 0, 0.5))
                .stats(TORSO, PartStats.of(5, 0, 2, 0, 0))
                .armStats(PartStats.of(1, 0, 1, 0, 1.5))
                .stats(LEGS, PartStats.of(4, 0, 3, 1, 0))
                .trait(ARM, Traits.GOLEM_TOSS)
                .trait(PartType.TORSO, Traits.HEAVY)
                .attribute(PartType.TORSO, Attributes.ARMOR, 6)
                .attribute(PartType.LEGS, Attributes.ARMOR, 4)
                .attribute(ARM, Attributes.ARMOR, 1)
                .sounds(sound(SoundEvents.IRON_GOLEM_STEP), sound(SoundEvents.IRON_GOLEM_HURT), sound(SoundEvents.IRON_GOLEM_DEATH));
        NecroMobRegistry.register(new NecroMob(Necromancy.id("isaac")).weight(3)
                .foundPart(PartType.HEAD, NecroItems.ISAACS_HEAD)
                .stats(HEAD, PartStats.of(2, 1, 1, 0, 0.5))
                .trait(PartType.HEAD, Traits.TEARS)
                .sounds(sound(SoundEvents.GHAST_AMBIENT), sound(SoundEvents.GHAST_HURT), sound(SoundEvents.GHAST_DEATH)));
        sewn("pig", () -> Ingredient.of(Items.PORKCHOP)).weight(1).shape(0.375F, 0.5F, 0.9F)
                .stats(HEAD, PartStats.of(0.5, 1, 0, 0, 0))
                .stats(TORSO, PartStats.of(1, 0, 0, 0, 0))
                .armStats(PartStats.of(0.25, 0, 0, 0, 0.25))
                .stats(LEGS, PartStats.of(0.25, 0, 1, 3, 0))
                .saddleable(0F)
                .sounds(sound(SoundEvents.PIG_AMBIENT), sound(SoundEvents.PIG_HURT), sound(SoundEvents.PIG_DEATH));
        sewn("zombified_piglin", () -> Ingredient.of(Items.GOLD_NUGGET)).weight(2)
                .stats(HEAD, PartStats.of(1.5, 1, 0, 0, 0.5))
                .stats(TORSO, PartStats.of(3, 0, 0, 0, 0))
                .armStats(PartStats.of(0.5, 0, 0, 0, 0.75))
                .stats(LEGS, PartStats.of(1.5, 0, 3, 3, 0))
                .trait(PartType.TORSO, Traits.FIRE_PROOF).trait(PartType.LEGS, Traits.FIRE_PROOF)
                .attribute(PartType.TORSO, Attributes.BURNING_TIME, -0.5)
                .attribute(PartType.LEGS, Attributes.BURNING_TIME, -0.5)
                .trait(ARM, Traits.GOLD_FIGHTER)
                .sounds(sound(SoundEvents.ZOMBIFIED_PIGLIN_AMBIENT), sound(SoundEvents.ZOMBIFIED_PIGLIN_HURT), sound(SoundEvents.ZOMBIFIED_PIGLIN_DEATH));
        sewn("sheep", () -> Ingredient.of(ItemTags.WOOL)).weight(1).shape(0.75F, 0.6F, 0.9F)
                .stats(HEAD, PartStats.of(0.5, 1, 0, 0, 0))
                .stats(TORSO, PartStats.of(1, 0, 0, 0, 0))
                .armStats(PartStats.of(0.25, 0, 0, 0, 0.25))
                .stats(LEGS, PartStats.of(0.25, 0, 1, 3, 0))
                .trait(PartType.TORSO, Traits.WOOLLY)
                .attribute(PartType.TORSO, Attributes.ARMOR, 2)
                .sounds(sound(SoundEvents.SHEEP_AMBIENT), sound(SoundEvents.SHEEP_HURT), sound(SoundEvents.SHEEP_DEATH));
        sewn("skeleton", () -> Ingredient.of(Items.BONE)).weight(2)
                .foundPart(PartType.HEAD, () -> Items.SKELETON_SKULL)
                .stats(HEAD, PartStats.of(1, 1, 0, 0, 0))
                .stats(TORSO, PartStats.of(1.5, 0, 0, 0, 0))
                .armStats(PartStats.of(0.5, 0, 0, 0, 0.5))
                .stats(LEGS, PartStats.of(0.75, 0, 3, 3, 0))
                .trait(PartType.HEAD, Traits.SUNBURN)
                .trait(ARM, Traits.flag("archer"))
                .sounds(sound(SoundEvents.SKELETON_AMBIENT), sound(SoundEvents.SKELETON_HURT), sound(SoundEvents.SKELETON_DEATH));
        sewn("spider", () -> Ingredient.of(Items.SPIDER_EYE)).weight(2).shape(0.45F, 0.5F, 1.2F)
                .stats(HEAD, PartStats.of(0.5, 1, 0, 0, 0.5))
                .stats(TORSO, PartStats.of(2, 0, 0, 0, 0))
                .stats(LEGS, PartStats.of(0.5, 0, 1, 2, 0.5))
                .trait(PartType.LEGS, Traits.CLIMBER)
                .saddleable(0F)
                .sounds(sound(SoundEvents.SPIDER_AMBIENT), sound(SoundEvents.SPIDER_HURT), sound(SoundEvents.SPIDER_DEATH));
        sewn("squid", () -> Ingredient.of(Items.INK_SAC)).weight(1).shape(0.6F, 0.75F, 0.75F)
                .stats(HEAD, PartStats.of(0.5, 1, 0, 0, 0.5))
                .stats(TORSO, PartStats.of(2, 0, 0, 0, 0))
                .stats(LEGS, PartStats.of(0.5, 0, 1, 2, 0.5))
                .trait(PartType.TORSO, Traits.INK_CLOUD)
                .attribute(PartType.LEGS, Attributes.WATER_MOVEMENT_EFFICIENCY, 1.0)
                .attribute(PartType.LEGS, Attributes.OXYGEN_BONUS, 4)
                .saddleable(0F)
                .sounds(sound(SoundEvents.SQUID_AMBIENT), sound(SoundEvents.SQUID_HURT), sound(SoundEvents.SQUID_DEATH));
        sewn("villager", () -> Ingredient.of(Items.BOOK)).weight(1)
                .stats(HEAD, PartStats.of(0.5, 1, 0, 0, 0))
                .stats(TORSO, PartStats.of(1, 0, 0, 0, 0))
                .armStats(PartStats.of(0.25, 0, 0, 0, 0.25))
                .stats(LEGS, PartStats.of(0.25, 0, 1, 3, 0))
                .trait(PartType.HEAD, Traits.THINKER)
                .sounds(sound(SoundEvents.VILLAGER_AMBIENT), sound(SoundEvents.VILLAGER_HURT), sound(SoundEvents.VILLAGER_DEATH));
        sewn("witch", () -> Ingredient.of(Items.POISONOUS_POTATO)).weight(2)
                .stats(HEAD, PartStats.of(1.5, 1, 0, 0, 0))
                .stats(TORSO, PartStats.of(2, 0, 0, 0, 0))
                .armStats(PartStats.of(0.5, 0, 0, 0, 0.75))
                .stats(LEGS, PartStats.of(1.5, 0, 3, 3, 0))
                .trait(PartType.HEAD, Traits.DARK_BREW)
                .trait(ARM, Traits.POTION_THROWER)
                .sounds(sound(SoundEvents.WITCH_AMBIENT), sound(SoundEvents.WITCH_HURT), sound(SoundEvents.WITCH_DEATH));
        sewn("wolf", () -> Ingredient.of(Items.NAME_TAG)).weight(2)
                .stats(HEAD, PartStats.of(1, 1, 0.5, 1, 1))
                .classicStats(HEAD, PartStats.of(2, 1, 1, 1, 2))
                .trait(PartType.HEAD, Traits.PACK_HUNTER)
                .sounds(sound(SoundEvents.WOLF_AMBIENT), sound(SoundEvents.WOLF_HURT), sound(SoundEvents.WOLF_DEATH));
        sewn("zombie", () -> Ingredient.of(Items.ROTTEN_FLESH)).weight(2)
                .foundPart(PartType.HEAD, () -> Items.ZOMBIE_HEAD)
                .stats(HEAD, PartStats.of(1, 1, 0, 0, 1))
                .stats(TORSO, PartStats.of(2, 0, 0, 0, 0))
                .armStats(PartStats.of(0.5, 0, 0, 0, 0.5))
                .stats(LEGS, PartStats.of(1, 0, 3, 3, 0))
                .trait(PartType.HEAD, Traits.HUNGER_BITE)
                .trait(PartType.HEAD, Traits.SUNBURN)
                .attribute(PartType.TORSO, Attributes.ARMOR, 2)
                .sounds(sound(SoundEvents.ZOMBIE_AMBIENT), sound(SoundEvents.ZOMBIE_HURT), sound(SoundEvents.ZOMBIE_DEATH));

        // ---------------- modern creatures ----------------
        sewn("husk", () -> Ingredient.of(Items.SAND)).weight(2)
                .stats(HEAD, PartStats.of(1, 1, 0, 0, 1))
                .stats(TORSO, PartStats.of(2, 0, 0, 0, 0))
                .armStats(PartStats.of(0.5, 0, 0, 0, 0.5))
                .stats(LEGS, PartStats.of(1, 0, 3, 3, 0))
                .trait(PartType.HEAD, Traits.HUSK_BITE)
                .trait(PartType.HEAD, Traits.SUN_PROOF)
                .attribute(PartType.TORSO, Attributes.ARMOR, 2)
                .sounds(sound(SoundEvents.HUSK_AMBIENT), sound(SoundEvents.HUSK_HURT), sound(SoundEvents.HUSK_DEATH));
        sewn("drowned", () -> Ingredient.of(Items.COPPER_INGOT)).weight(2)
                .stats(HEAD, PartStats.of(1, 1, 0, 0, 0.75))
                .stats(TORSO, PartStats.of(2, 0, 0, 0, 0))
                .armStats(PartStats.of(0.5, 0, 0, 0, 0.5))
                .stats(LEGS, PartStats.of(1, 0, 2, 2, 0))
                .trait(PartType.HEAD, Traits.SUNBURN)
                .trait(ARM, Traits.flag("trident_thrower"))
                .attribute(PartType.TORSO, Attributes.OXYGEN_BONUS, 4)
                .attribute(PartType.LEGS, Attributes.WATER_MOVEMENT_EFFICIENCY, 1.0)
                .sounds(sound(SoundEvents.DROWNED_AMBIENT), sound(SoundEvents.DROWNED_HURT), sound(SoundEvents.DROWNED_DEATH));
        sewn("bogged", () -> Ingredient.of(Items.BROWN_MUSHROOM)).weight(2)
                .stats(HEAD, PartStats.of(0.75, 1, 0, 0, 0))
                .stats(TORSO, PartStats.of(1.25, 0, 0, 0, 0))
                .armStats(PartStats.of(0.4, 0, 0, 0, 0.5))
                .stats(LEGS, PartStats.of(0.75, 0, 3, 3, 0))
                .trait(PartType.HEAD, Traits.SUNBURN)
                .trait(ARM, Traits.flag("poison_archer"))
                .sounds(sound(SoundEvents.BOGGED_AMBIENT), sound(SoundEvents.BOGGED_HURT), sound(SoundEvents.BOGGED_DEATH));
        sewn("wither_skeleton", () -> Ingredient.of(Items.COAL)).weight(3).shape(0.85F, 0.85F, 0.7F)
                .foundPart(PartType.HEAD, () -> Items.WITHER_SKELETON_SKULL)
                .stats(HEAD, PartStats.of(1, 1, 0, 0, 1))
                .stats(TORSO, PartStats.of(2, 0, 1, 0, 0))
                .armStats(PartStats.of(0.5, 0, 0, 0, 1))
                .stats(LEGS, PartStats.of(1, 0, 3, 3, 0))
                .trait(ARM, Traits.WITHERING)
                .trait(PartType.TORSO, Traits.FIRE_PROOF).trait(PartType.LEGS, Traits.FIRE_PROOF)
                .attribute(PartType.TORSO, Attributes.SCALE, 0.1)
                .attribute(PartType.LEGS, Attributes.SCALE, 0.1)
                .sounds(sound(SoundEvents.WITHER_SKELETON_AMBIENT), sound(SoundEvents.WITHER_SKELETON_HURT), sound(SoundEvents.WITHER_SKELETON_DEATH));
        sewn("blaze", () -> Ingredient.of(Items.BLAZE_ROD)).weight(3).shape(0.75F, 0.5F, 0.6F)
                .stats(HEAD, PartStats.of(0.5, 1, 0, 0, 0.5))
                .stats(TORSO, PartStats.of(1, 0, 0, 0, 0))
                .stats(LEGS, PartStats.of(0.5, 0, 0, 1, 0.5))
                .trait(PartType.HEAD, Traits.FIREBALLS)
                .trait(PartType.HEAD, Traits.WATER_HURTS).trait(PartType.TORSO, Traits.WATER_HURTS).trait(PartType.LEGS, Traits.WATER_HURTS)
                .trait(PartType.TORSO, Traits.FIRE_PROOF).trait(PartType.LEGS, Traits.FIRE_PROOF)
                .attribute(PartType.LEGS, Attributes.GRAVITY, -0.04)
                .attribute(PartType.LEGS, Attributes.SAFE_FALL_DISTANCE, 100)
                .sounds(sound(SoundEvents.BLAZE_AMBIENT), sound(SoundEvents.BLAZE_HURT), sound(SoundEvents.BLAZE_DEATH));
        sewn("slime", () -> Ingredient.of(Items.SLIME_BALL)).weight(1).shape(0.5F, 0.5F, 0.6F)
                .stats(HEAD, PartStats.of(0.5, 1, 0, 0, 0.25))
                .stats(TORSO, PartStats.of(1.5, 0, 0, 0, 0))
                .stats(LEGS, PartStats.of(0.5, 0, 1, 1, 0))
                .trait(PartType.TORSO, Traits.BOUNCY)
                .attribute(PartType.LEGS, Attributes.FALL_DAMAGE_MULTIPLIER, -1.0)
                .attribute(PartType.LEGS, Attributes.JUMP_STRENGTH, 0.15)
                .sounds(sound(SoundEvents.SLIME_SQUISH), sound(SoundEvents.SLIME_HURT), sound(SoundEvents.SLIME_DEATH));
        sewn("mooshroom", () -> Ingredient.of(Items.RED_MUSHROOM)).weight(1).shape(0.75F, 0.6F, 0.9F)
                .stats(HEAD, PartStats.of(0.5, 1, 0, 0, 0))
                .stats(TORSO, PartStats.of(1, 0, 0, 0, 0))
                .armStats(PartStats.of(0.25, 0, 0, 0, 0.25))
                .stats(LEGS, PartStats.of(0.25, 0, 1, 3, 0))
                .trait(PartType.TORSO, Traits.STEW)
                .trait(PartType.TORSO, Traits.MILKABLE)
                .saddleable(0F)
                .sounds(sound(SoundEvents.COW_AMBIENT), sound(SoundEvents.COW_HURT), sound(SoundEvents.COW_DEATH));
        sewn("snow_golem", () -> Ingredient.of(Items.SNOW_BLOCK)).weight(1).shape(0.6F, 0.6F, 0.7F)
                .essence(PartType.HEAD, () -> Ingredient.of(Items.CARVED_PUMPKIN))
                .essence(ARM, () -> Ingredient.of(Items.STICK))
                .stats(HEAD, PartStats.of(0.25, 1, 0, 0, 0))
                .stats(TORSO, PartStats.of(0.5, 0, 0, 0, 0))
                .armStats(PartStats.of(0.1, 0, 0, 0, 0))
                .stats(LEGS, PartStats.of(0.25, 0, 0, 1, 0))
                .trait(PartType.HEAD, Traits.SNOWBALLS)
                .trait(PartType.TORSO, Traits.MELTS).trait(PartType.LEGS, Traits.MELTS)
                .sounds(sound(SoundEvents.SNOW_GOLEM_AMBIENT), sound(SoundEvents.SNOW_GOLEM_HURT), sound(SoundEvents.SNOW_GOLEM_DEATH));
        sewn("armadillo", () -> Ingredient.of(Items.ARMADILLO_SCUTE)).weight(2).shape(0.3F, 0.4F, 0.7F)
                .stats(HEAD, PartStats.of(0.25, 1, 0, 0, 0))
                .stats(TORSO, PartStats.of(1, 0, 1, 0, 0))
                .stats(LEGS, PartStats.of(0.25, 0, 0.5, 1, 0))
                .trait(PartType.TORSO, Traits.ROLL_UP)
                .attribute(PartType.TORSO, Attributes.ARMOR, 4)
                .attribute(PartType.TORSO, Attributes.ARMOR_TOUGHNESS, 1)
                .sounds(sound(SoundEvents.ARMADILLO_AMBIENT), sound(SoundEvents.ARMADILLO_HURT), sound(SoundEvents.ARMADILLO_DEATH));
        sewn("goat", () -> Ingredient.of(Items.GOAT_HORN)).weight(2)
                .stats(HEAD, PartStats.of(1, 1, 0.5, 0.5, 1))
                .trait(PartType.HEAD, Traits.RAM)
                .attribute(PartType.HEAD, Attributes.ATTACK_KNOCKBACK, 1.0)
                .sounds(sound(SoundEvents.GOAT_AMBIENT), sound(SoundEvents.GOAT_HURT), sound(SoundEvents.GOAT_DEATH));
        sewn("night_crawler", () -> Ingredient.of(Items.ENDER_PEARL)).weight(3).shape(0.9F, 0.7F, 0.6F)
                .stats(HEAD, PartStats.of(1, 1.5, 0, 0, 0.75))
                .stats(TORSO, PartStats.of(2, 0, 0, 0, 0))
                .armStats(PartStats.of(0.5, 0, 0, 0, 0.75))
                .stats(LEGS, PartStats.of(1, 0, 2, 3.5, 0))
                .trait(PartType.HEAD, Traits.SHADOW_TOUCH)
                .trait(PartType.HEAD, Traits.SUNBURN)
                .sounds(() -> NecroSounds.NIGHTCRAWLER_HOWL.get(), sound(SoundEvents.ENDERMAN_HURT), sound(SoundEvents.ENDERMAN_DEATH));
    }

    /** A mob whose sewn parts are listed in {@link com.sirolf2009.necromancy.registry.BodyPartTable}. */
    static NecroMob sewn(String name, Supplier<Ingredient> essence) {
        NecroMob mob = new NecroMob(Necromancy.id(name)).essence(essence);
        NecroItems.BODY_PARTS.get(name).forEach((type, item) -> mob.part(type, item));
        return NecroMobRegistry.register(mob);
    }

    static Supplier<SoundEvent> sound(SoundEvent event) {
        return () -> event;
    }
}
