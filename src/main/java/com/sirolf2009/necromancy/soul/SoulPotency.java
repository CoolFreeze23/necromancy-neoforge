package com.sirolf2009.necromancy.soul;

import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * How strong a soul is, from the max health of the creature it was reaped from. A soul can only animate a body whose
 * parts weigh up to its cap (docs/REVIEW.md #5).
 */
public enum SoulPotency implements StringRepresentable {
    FAINT("faint", 6, ChatFormatting.GRAY),
    LESSER("lesser", 9, ChatFormatting.WHITE),
    COMMON("common", 12, ChatFormatting.GREEN),
    POTENT("potent", 16, ChatFormatting.AQUA),
    MIGHTY("mighty", 20, ChatFormatting.LIGHT_PURPLE),
    LEGENDARY("legendary", 20, ChatFormatting.GOLD);

    public static final Codec<SoulPotency> CODEC = StringRepresentable.fromEnum(SoulPotency::values);

    private final String name;
    private final int weightCap;
    private final ChatFormatting color;

    SoulPotency(String name, int weightCap, ChatFormatting color) {
        this.name = name;
        this.weightCap = weightCap;
        this.color = color;
    }

    public int weightCap() {
        return weightCap;
    }

    public ChatFormatting color() {
        return color;
    }

    public Component displayName() {
        return Component.translatable("soul.necromancy.potency." + name).withStyle(color);
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public SoulPotency stronger() {
        return this == LEGENDARY ? LEGENDARY : values()[ordinal() + 1];
    }

    public static SoulPotency ofHealth(double maxHealth) {
        if (maxHealth < 10) {
            return FAINT;
        }
        if (maxHealth < 20) {
            return LESSER;
        }
        if (maxHealth < 40) {
            return COMMON;
        }
        if (maxHealth < 100) {
            return POTENT;
        }
        return MIGHTY;
    }

    /** Potency of a creature's soul as it dies: bosses are Legendary, raid captains one step stronger. */
    public static SoulPotency of(LivingEntity victim) {
        if (victim.getType().is(com.sirolf2009.necromancy.registry.NecroTags.LEGENDARY_SOULS)) {
            return LEGENDARY;
        }
        SoulPotency potency = ofHealth(victim.getAttributeValue(Attributes.MAX_HEALTH));
        if (victim instanceof net.minecraft.world.entity.raid.Raider raider && raider.isPatrolLeader()) {
            potency = potency.stronger();
        }
        return potency;
    }
}
