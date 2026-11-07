package com.sirolf2009.necromancy.item;

import com.sirolf2009.necromancy.registry.NecroItems;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Draws blood: use it on a living creature while carrying a Glass Bottle to fill a Jar of Blood (a left click works too, as
 * in the original). Each jar and each stitch in the Sewing Machine wears the needle by one; it lasts 10 uses, where the
 * original broke on every use.
 */
public class BoneNeedleItem extends Item {
    public BoneNeedleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return drawBlood(stack, player, target, LivingEntity.getSlotForHand(hand)) ? InteractionResult.sidedSuccess(player.level().isClientSide) : InteractionResult.PASS;
    }

    /** Also called from the attack event, so the original left click still works. */
    public static boolean drawBlood(ItemStack needle, Player player, LivingEntity target, net.minecraft.world.entity.EquipmentSlot slot) {
        if (!target.isAlive() || !canBleed(target)) {
            return false;
        }
        int bottle = findBottle(player);
        if (bottle < 0 && !player.getAbilities().instabuild) {
            if (player.level().isClientSide) {
                return false;
            }
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.necromancy.need_bottle"), true);
            return false;
        }
        if (!player.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                player.getInventory().getItem(bottle).shrink(1);
                needle.hurtAndBreak(1, player, slot);
            }
            ItemStack jar = new ItemStack(NecroItems.JAR_OF_BLOOD.get());
            if (!player.getInventory().add(jar)) {
                player.drop(jar, false);
            }
            target.hurt(player.damageSources().playerAttack(player), 1F);
            player.level().playSound(null, target, SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1F, 0.8F);
            target.gameEvent(GameEvent.ENTITY_INTERACT, player);
        }
        return true;
    }

    private static boolean canBleed(LivingEntity target) {
        return !target.getType().is(com.sirolf2009.necromancy.registry.NecroTags.BLOODLESS);
    }

    private static int findBottle(Player player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(Items.GLASS_BOTTLE)) {
                return i;
            }
        }
        return -1;
    }
}
