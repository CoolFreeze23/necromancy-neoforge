package com.sirolf2009.necromancy.api;

import com.sirolf2009.necromancy.entity.MinionEntity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * A body part's special ability. Every hook runs on the server for each location that holds a part with this trait
 * (a trait on arms runs once per arm, so per-arm traits should scale themselves). The id names the tooltip line
 * {@code trait.<namespace>.<path>} and its description {@code trait.<namespace>.<path>.desc}.
 */
public interface PartTrait {
    net.minecraft.resources.ResourceLocation id();

    /** After a successful melee hit. */
    default void onAttack(MinionEntity minion, BodyPartLocation location, LivingEntity target) {
    }

    /** Before damage is applied; return the new amount. */
    default float onHurt(MinionEntity minion, BodyPartLocation location, DamageSource source, float amount) {
        return amount;
    }

    /** Every tick on the server. Use {@link MinionEntity#cooldown} for timers. */
    default void tick(MinionEntity minion, BodyPartLocation location) {
    }

    default void onDeath(MinionEntity minion, BodyPartLocation location, DamageSource source) {
    }

    /** The owner used an item on the Minion (milk a cow torso, shear a sheep torso). Return PASS to let others handle it. */
    default net.minecraft.world.InteractionResult onInteract(MinionEntity minion, BodyPartLocation location, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand) {
        return net.minecraft.world.InteractionResult.PASS;
    }

    /** Passive properties the Minion checks: climbing, fire immunity, sunburn, ... */
    default boolean has(TraitFlag flag) {
        return false;
    }

    /** The tooltip line naming this trait. */
    default net.minecraft.network.chat.Component name() {
        return net.minecraft.network.chat.Component.translatable("trait." + id().getNamespace() + "." + id().getPath());
    }

    /** The tooltip line under the name: what the trait does. */
    default net.minecraft.network.chat.Component description() {
        return net.minecraft.network.chat.Component.translatable("trait." + id().getNamespace() + "." + id().getPath() + ".desc");
    }

    /** Shown in the tooltip; false for traits that only exist as flags of a bigger mechanic. */
    default boolean visible() {
        return true;
    }
}
