package com.sirolf2009.necromancy.item;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sirolf2009.necromancy.entity.MinionEntity;
import com.sirolf2009.necromancy.entity.MinionOrder;
import com.sirolf2009.necromancy.entity.MinionStance;
import com.sirolf2009.necromancy.registry.NecroComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Commands Minions (docs/REVIEW.md #2).
 * <ul>
 * <li>Use on your Minion: select or deselect it. Sneak: select all of yours within 24 blocks.</li>
 * <li>Use in the air: choose the order (Follow, Stay, Guard, Patrol, Work). Sneak: change the selected Minions' stance.</li>
 * <li>Use on a block: give the order there (Guard guards that block). With Patrol chosen, each use adds a waypoint and
 * sends the selected Minions along the route; sneak-use on a block clears the route. With Work chosen, use it on a chest
 * (or any container): that is where they work and store the yield.</li>
 * </ul>
 */
public class BoneScepterItem extends Item {
    private static final int SELECT_RANGE = 24;

    public BoneScepterItem(Properties properties) {
        super(properties);
    }

    public static MinionOrder order(ItemStack stack) {
        return stack.getOrDefault(NecroComponents.SCEPTER_ORDER.get(), MinionOrder.FOLLOW);
    }

    private static List<UUID> selected(ItemStack stack) {
        return stack.getOrDefault(NecroComponents.SELECTED_MINIONS.get(), List.of());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof MinionEntity minion) || !minion.isOwnedBy(player)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack held = player.getItemInHand(hand);
        List<UUID> selection = new ArrayList<>(selected(held));
        if (player.isSecondaryUseActive()) {
            selection.clear();
            for (MinionEntity other : nearby(player)) {
                selection.add(other.getUUID());
                mark(other);
            }
            player.displayClientMessage(Component.translatable("message.necromancy.scepter.selected_all", selection.size()), true);
        } else if (selection.remove(minion.getUUID())) {
            player.displayClientMessage(Component.translatable("message.necromancy.scepter.deselected", minion.getDisplayName()), true);
        } else {
            selection.add(minion.getUUID());
            mark(minion);
            player.displayClientMessage(Component.translatable("message.necromancy.scepter.selected", minion.getDisplayName(), selection.size()), true);
        }
        held.set(NecroComponents.SELECTED_MINIONS.get(), List.copyOf(selection));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (player.isSecondaryUseActive()) {
            List<MinionEntity> minions = resolve(player, stack);
            MinionStance stance = minions.isEmpty() ? MinionStance.DEFENSIVE : minions.get(0).stance().next();
            for (MinionEntity minion : minions) {
                minion.setStance(stance);
            }
            player.displayClientMessage(Component.translatable("message.necromancy.scepter.stance", stance.displayName(), minions.size()), true);
        } else {
            MinionOrder order = order(stack).next();
            stack.set(NecroComponents.SCEPTER_ORDER.get(), order);
            player.displayClientMessage(Component.translatable("message.necromancy.scepter.order", order.displayName()), true);
        }
        level.playSound(null, player, SoundEvents.SKELETON_STEP, SoundSource.PLAYERS, 0.6F, 1.4F);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        MinionOrder order = order(stack);
        BlockPos pos = order == MinionOrder.WORK ? context.getClickedPos() : context.getClickedPos().relative(context.getClickedFace());
        List<BlockPos> route = new ArrayList<>(stack.getOrDefault(NecroComponents.PATROL_ROUTE.get(), List.of()));
        if (order == MinionOrder.PATROL) {
            if (player.isSecondaryUseActive()) {
                stack.remove(NecroComponents.PATROL_ROUTE.get());
                player.displayClientMessage(Component.translatable("message.necromancy.scepter.route_cleared"), true);
                return InteractionResult.SUCCESS;
            }
            if (route.size() >= 4) {
                route.remove(0);
            }
            route.add(pos);
            stack.set(NecroComponents.PATROL_ROUTE.get(), List.copyOf(route));
            if (context.getLevel() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 8, 0.2, 0.1, 0.2, 0.01);
            }
        }
        List<MinionEntity> minions = resolve(player, stack);
        if (order != MinionOrder.PATROL || route.size() >= 2) {
            for (MinionEntity minion : minions) {
                minion.giveOrder(order, pos, route);
            }
        }
        Component message = order == MinionOrder.PATROL
                ? Component.translatable("message.necromancy.scepter.waypoint", route.size(), minions.size())
                : Component.translatable("message.necromancy.scepter.ordered", order.displayName(), minions.size());
        player.displayClientMessage(message, true);
        return InteractionResult.SUCCESS;
    }

    /** The selected Minions that are still alive, loaded and close enough to hear (64 blocks). */
    private static List<MinionEntity> resolve(Player player, ItemStack stack) {
        List<MinionEntity> list = new ArrayList<>();
        if (player.level() instanceof ServerLevel level) {
            for (UUID id : selected(stack)) {
                Entity entity = level.getEntity(id);
                if (entity instanceof MinionEntity minion && minion.isAlive() && minion.isOwnedBy(player) && minion.distanceToSqr(player) < 64 * 64) {
                    list.add(minion);
                }
            }
        }
        return list;
    }

    private static List<MinionEntity> nearby(Player player) {
        return player.level().getEntitiesOfClass(MinionEntity.class, player.getBoundingBox().inflate(SELECT_RANGE), m -> m.isOwnedBy(player));
    }

    private static void mark(MinionEntity minion) {
        if (minion.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SOUL, minion.getX(), minion.getY() + minion.getBbHeight() + 0.3, minion.getZ(), 6, 0.2, 0.1, 0.2, 0.01);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.necromancy.scepter.order", order(stack).displayName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.necromancy.scepter.selected", selected(stack).size()).withStyle(ChatFormatting.GRAY));
        int waypoints = stack.getOrDefault(NecroComponents.PATROL_ROUTE.get(), List.of()).size();
        if (waypoints > 0) {
            tooltip.add(Component.translatable("tooltip.necromancy.scepter.route", waypoints).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.necromancy.scepter.help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
