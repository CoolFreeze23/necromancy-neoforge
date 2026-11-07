package com.sirolf2009.necromancy.item;

import java.util.List;

import com.sirolf2009.necromancy.soul.SoulData;
import com.sirolf2009.necromancy.soul.SoulPotency;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A Soul in a Jar. Remembers what it was reaped from; its potency limits how heavy a body it can animate. */
public class SoulJarItem extends Item {
    public SoulJarItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SoulData soul = SoulData.get(stack);
        tooltip.add(soul.description().copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.necromancy.soul.cap", soul.potency().weightCap()).withStyle(ChatFormatting.DARK_GRAY));
        if (soul.potency() == SoulPotency.LEGENDARY) {
            tooltip.add(Component.translatable("tooltip.necromancy.soul.legendary").withStyle(ChatFormatting.GOLD));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return SoulData.get(stack).potency().ordinal() >= SoulPotency.MIGHTY.ordinal();
    }
}
