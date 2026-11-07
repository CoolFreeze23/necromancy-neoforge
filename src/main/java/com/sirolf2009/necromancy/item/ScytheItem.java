package com.sirolf2009.necromancy.item;

import java.util.List;

import com.sirolf2009.necromancy.registry.NecroItems;
import com.sirolf2009.necromancy.registry.NecroTags;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

/**
 * Reaps souls: a killing blow with a scythe fills an empty Glass Bottle from your inventory with a Soul in a Jar.
 * Handled from the death event too ({@code NecroEvents}), so sweeps and thorns kills also count.
 */
public class ScytheItem extends SwordItem {
    public ScytheItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    /**
     * Scythe attributes (docs/REVIEW.md #14): total attack damage and speed as a player sees them, plus a free sweep and a
     * little extra reach.
     */
    public static net.minecraft.world.item.component.ItemAttributeModifiers scytheAttributes(double damage, double speed, double sweep, double reach) {
        var hand = net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND;
        var add = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE;
        return net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                        new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_DAMAGE_ID, damage - 1, add), hand)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                        new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_SPEED_ID, speed - 4, add), hand)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.SWEEPING_DAMAGE_RATIO,
                        new net.minecraft.world.entity.ai.attributes.AttributeModifier(com.sirolf2009.necromancy.Necromancy.id("scythe_sweep"), sweep, add), hand)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE,
                        new net.minecraft.world.entity.ai.attributes.AttributeModifier(com.sirolf2009.necromancy.Necromancy.id("scythe_reach"), reach, add), hand)
                .build();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.necromancy.scythe").withStyle(ChatFormatting.GRAY));
        if (stack.is(NecroItems.NETHERITE_SCYTHE.get())) {
            tooltip.add(Component.translatable("tooltip.necromancy.soul_harvest").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    /** Puts a soul in a bottle when a creature dies to a scythe. Returns true if a soul was taken. */
    public static boolean reapSoul(Player player, LivingEntity victim) {
        if (!player.getMainHandItem().is(NecroTags.SCYTHES) || victim instanceof Player) {
            return false;
        }
        return bottleSoul(player, victim);
    }

    /**
     * Netherite Scythe, Soul Harvest: while you hold it in either hand, creatures your Minions kill within 16 blocks of
     * you give you their soul too.
     */
    public static boolean harvestForMinion(Player owner, LivingEntity victim) {
        boolean holding = owner.getMainHandItem().is(NecroItems.NETHERITE_SCYTHE.get()) || owner.getOffhandItem().is(NecroItems.NETHERITE_SCYTHE.get());
        return holding && !(victim instanceof Player) && owner.distanceToSqr(victim) < 256 && bottleSoul(owner, victim);
    }

    /** Fills one of the player's glass bottles with the victim's soul. False without a bottle or for a soulless creature. */
    public static boolean bottleSoul(Player player, LivingEntity victim) {
        if (victim.getType().is(NecroTags.NO_SOUL)) {
            return false;
        }
        var inventory = player.getInventory();
        int bottle = -1;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(Items.GLASS_BOTTLE)) {
                bottle = i;
                break;
            }
        }
        if (bottle < 0) {
            return false;
        }
        inventory.getItem(bottle).shrink(1);
        ItemStack soul = new ItemStack(NecroItems.SOUL_IN_A_JAR.get());
        soul.set(com.sirolf2009.necromancy.registry.NecroComponents.SOUL.get(),
                com.sirolf2009.necromancy.soul.SoulData.of(victim.getType(), com.sirolf2009.necromancy.soul.SoulPotency.of(victim)));
        if (!inventory.add(soul)) {
            player.drop(soul, false);
        }
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SCULK_SOUL, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 12, 0.25, 0.4, 0.25, 0.03);
            level.playSound(null, victim, SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.5F, 0.8F);
        }
        return true;
    }
}
