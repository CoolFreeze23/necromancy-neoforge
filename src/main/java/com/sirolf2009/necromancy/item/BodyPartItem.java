package com.sirolf2009.necromancy.item;

import java.util.List;

import com.sirolf2009.necromancy.api.BodyPartLocation;
import com.sirolf2009.necromancy.api.NecroMob;
import com.sirolf2009.necromancy.api.NecroMobRegistry;
import com.sirolf2009.necromancy.api.PartTrait;
import com.sirolf2009.necromancy.api.PartStats;
import com.sirolf2009.necromancy.api.PartType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A sewn body part. The tooltip lists what it gives a Minion, which the original kept hidden. */
public class BodyPartItem extends Item {
    public BodyPartItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        addPartTooltip(stack, tooltip);
    }

    /** Shared with the vanilla heads, which get the same lines through the tooltip event. */
    public static void addPartTooltip(ItemStack stack, List<Component> tooltip) {
        NecroMobRegistry.partOf(stack).ifPresent(ref -> {
            BodyPartLocation location = switch (ref.type()) {
                case HEAD -> BodyPartLocation.HEAD;
                case TORSO -> BodyPartLocation.TORSO;
                case ARM -> BodyPartLocation.ARM_RIGHT;
                case LEGS -> BodyPartLocation.LEGS;
            };
            tooltip.add(Component.translatable("tooltip.necromancy.part." + ref.type().getSerializedName(),
                    Component.translatable(ref.mob().translationKey())).withStyle(ChatFormatting.DARK_PURPLE));
            PartStats stats = ref.mob().stats(location);
            boolean perArm = ref.type() == PartType.ARM;
            line(tooltip, "health", stats.health(), perArm);
            line(tooltip, "attack", stats.attackDamage(), perArm);
            line(tooltip, "speed", stats.movementSpeed(), perArm);
            line(tooltip, "knockback", stats.knockbackResistance(), perArm);
            line(tooltip, "follow", stats.followRange(), perArm);
            for (NecroMob.ExtraAttribute extra : ref.mob().extraAttributes(ref.type())) {
                double amount = extra.amount();
                String text = extra.operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                        ? (amount > 0 ? "+" : "") + trim(amount) : (amount > 0 ? "+" : "") + Math.round(amount * 100) + "%";
                boolean good = amount > 0 != isCostly(extra);
                tooltip.add(Component.literal(" " + text + " ").append(Component.translatable(extra.attribute().value().getDescriptionId()))
                        .withStyle(good ? ChatFormatting.BLUE : ChatFormatting.RED));
            }
            if (ref.mob().isSaddleable() && ref.type() == PartType.TORSO) {
                tooltip.add(Component.translatable("tooltip.necromancy.saddleable").withStyle(ChatFormatting.GRAY));
            }
            for (PartTrait trait : ref.mob().traits(ref.type())) {
                if (trait.visible()) {
                    tooltip.add(trait.name().copy().withStyle(ChatFormatting.GOLD));
                    tooltip.add(Component.literal("  ").append(trait.description()).withStyle(ChatFormatting.DARK_GRAY));
                }
            }
            tooltip.add(Component.translatable("tooltip.necromancy.weight", ref.mob().weight()).withStyle(ChatFormatting.DARK_GRAY));
        });
    }

    private static String trim(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(Math.round(v * 100) / 100.0);
    }

    /** Attributes where more is worse for the Minion. */
    private static boolean isCostly(NecroMob.ExtraAttribute extra) {
        return extra.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY.unwrapKey().orElseThrow())
                || extra.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.FALL_DAMAGE_MULTIPLIER.unwrapKey().orElseThrow())
                || extra.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.BURNING_TIME.unwrapKey().orElseThrow());
    }

    private static void line(List<Component> tooltip, String stat, double value, boolean perArm) {
        if (value == 0) {
            return;
        }
        String percent = (value > 0 ? "+" : "") + Math.round(value * 100) + "%";
        Component text = Component.translatable("tooltip.necromancy.stat." + stat, percent);
        if (perArm) {
            text = Component.translatable("tooltip.necromancy.per_arm", text);
        }
        tooltip.add(Component.literal(" ").append(text).withStyle(value > 0 ? ChatFormatting.BLUE : ChatFormatting.RED));
    }
}
