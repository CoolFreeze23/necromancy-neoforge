package com.sirolf2009.necromancy.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Isaac's severed head, worn as a helmet. While worn, the "Shoot Tear" and "Shoot Blood Tear" keys fire tears where you
 * look (blood tears hit harder but cost half a heart). It is also the head of the Isaac Minion. Repaired with Skin.
 */
public class IsaacsHeadItem extends ArmorItem {
    public IsaacsHeadItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.HELMET, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.necromancy.isaacs_head").withStyle(ChatFormatting.GRAY));
        BodyPartItem.addPartTooltip(stack, tooltip);
    }
}
