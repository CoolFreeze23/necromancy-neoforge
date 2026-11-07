package com.sirolf2009.necromancy.item;

import java.util.List;

import com.sirolf2009.necromancy.registry.NecroEntities;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.gameevent.GameEvent;

/** Isaac's Soul Heart: sewn from flesh, tears and a soul, it wakes Isaac where it is used (a repeatable way to his head). */
public class SoulHeartItem extends Item {
    public SoulHeartItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        EntityType<?> type = NecroEntities.ISAAC.get();
        var entity = type.spawn(level, context.getItemInHand(), context.getPlayer(), pos, MobSpawnType.SPAWN_EGG, true,
                !pos.equals(context.getClickedPos()) && context.getClickedFace() == net.minecraft.core.Direction.UP);
        if (entity != null) {
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
            level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.necromancy.soul_heart").withStyle(ChatFormatting.GRAY));
    }
}
