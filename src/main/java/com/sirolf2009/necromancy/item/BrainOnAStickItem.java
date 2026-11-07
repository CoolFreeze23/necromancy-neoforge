package com.sirolf2009.necromancy.item;

import com.sirolf2009.necromancy.entity.MinionEntity;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Carrot on a Stick for Minions: steers a saddled Minion, use it while riding for a burst of speed. Minions follow it. */
public class BrainOnAStickItem extends Item {
    private static final int BOOST_DAMAGE = 7;

    public BrainOnAStickItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }
        Entity vehicle = player.getControlledVehicle();
        if (player.isPassenger() && vehicle instanceof MinionEntity minion && minion.boost()) {
            EquipmentSlot slot = LivingEntity.getSlotForHand(hand);
            return InteractionResultHolder.success(stack.hurtAndConvertOnBreak(BOOST_DAMAGE, Items.FISHING_ROD, player, slot));
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.pass(stack);
    }
}
