package com.sirolf2009.necromancy.item;

import java.util.List;

import com.sirolf2009.necromancy.api.NecroMob;
import com.sirolf2009.necromancy.api.NecroMobRegistry;
import com.sirolf2009.necromancy.registry.NecroComponents;
import com.sirolf2009.necromancy.soul.MinionBuild;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** What a Phylactery-bound Minion leaves behind. Put it in the altar's soul slot to raise the same Minion again. */
public class MinionRemainsItem extends Item {
    public MinionRemainsItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        MinionBuild build = stack.get(NecroComponents.MINION_BUILD.get());
        if (build != null && build.name().isPresent()) {
            return Component.translatable("item.necromancy.minion_remains.named", build.name().get());
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        MinionBuild build = stack.get(NecroComponents.MINION_BUILD.get());
        if (build == null) {
            tooltip.add(Component.translatable("tooltip.necromancy.remains.empty").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        build.locations().forEach((location, mob) -> {
            NecroMob necro = NecroMobRegistry.get(mob);
            if (necro != null) {
                tooltip.add(Component.translatable("tooltip.necromancy.remains.part", Component.translatable("location.necromancy." + location.getSerializedName()),
                        Component.translatable(necro.translationKey())).withStyle(ChatFormatting.GRAY));
            }
        });
        tooltip.add(build.soul().description().copy().withStyle(ChatFormatting.DARK_PURPLE));
        if (build.kills() > 0) {
            tooltip.add(Component.translatable("tooltip.necromancy.remains.kills", build.kills()).withStyle(ChatFormatting.DARK_RED));
        }
        tooltip.add(Component.translatable("tooltip.necromancy.remains.help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
