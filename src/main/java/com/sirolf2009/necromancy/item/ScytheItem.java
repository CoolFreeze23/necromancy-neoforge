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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.necromancy.scythe").withStyle(ChatFormatting.GRAY));
    }

    /** Puts a soul in a bottle when a creature dies to a scythe. Returns true if a soul was taken. */
    public static boolean reapSoul(Player player, LivingEntity victim) {
        if (!player.getMainHandItem().is(NecroTags.SCYTHES) || victim instanceof Player) {
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
