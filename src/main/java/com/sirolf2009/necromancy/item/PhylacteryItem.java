package com.sirolf2009.necromancy.item;

import java.util.List;
import java.util.UUID;

import com.sirolf2009.necromancy.entity.MinionEntity;
import com.sirolf2009.necromancy.registry.NecroComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Keeps a Minion's soul safe (docs/REVIEW.md #7). Sneak-use it on your Minion to bind it (uses a charge): when a bound
 * Minion dies it leaves Minion Remains that the altar can raise again. Use it to call the bound Minion to your side
 * (same dimension, within 128 blocks, once a minute).
 */
public class PhylacteryItem extends Item {
    private static final int RECALL_RANGE = 128;
    private static final int RECALL_COOLDOWN = 1200;

    public PhylacteryItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof MinionEntity minion) || !minion.isOwnedBy(player) || !player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (minion.isBound()) {
            player.displayClientMessage(Component.translatable("message.necromancy.phylactery.already_bound", minion.getDisplayName()), true);
            return InteractionResult.SUCCESS;
        }
        minion.setBound(true);
        held.set(NecroComponents.BOUND_MINION.get(), minion.getUUID());
        held.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        player.level().playSound(null, minion, SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.5F, 0.6F);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SCULK_SOUL, minion.getX(), minion.getY(0.6), minion.getZ(), 16, 0.3, 0.4, 0.3, 0.03);
        }
        player.displayClientMessage(Component.translatable("message.necromancy.phylactery.bound", minion.getDisplayName()), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        UUID bound = stack.get(NecroComponents.BOUND_MINION.get());
        if (bound == null) {
            return InteractionResultHolder.pass(stack);
        }
        if (level instanceof ServerLevel server) {
            Entity entity = server.getEntity(bound);
            if (entity instanceof MinionEntity minion && minion.isAlive() && minion.distanceToSqr(player) < RECALL_RANGE * RECALL_RANGE) {
                server.sendParticles(ParticleTypes.SOUL, minion.getX(), minion.getY(0.5), minion.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
                minion.teleportTo(player.getX(), player.getY(), player.getZ());
                minion.getNavigation().stop();
                server.sendParticles(ParticleTypes.SOUL, minion.getX(), minion.getY(0.5), minion.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
                server.playSound(null, player, SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1F, 1.2F);
                player.getCooldowns().addCooldown(this, RECALL_COOLDOWN);
            } else {
                player.displayClientMessage(Component.translatable("message.necromancy.phylactery.too_far"), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(NecroComponents.BOUND_MINION.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(stack.has(NecroComponents.BOUND_MINION.get()) ? "tooltip.necromancy.phylactery.bound"
                : "tooltip.necromancy.phylactery.empty").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.necromancy.phylactery.help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
