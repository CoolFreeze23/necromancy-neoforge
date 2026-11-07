package com.sirolf2009.necromancy.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sirolf2009.necromancy.api.NecroMob;
import com.sirolf2009.necromancy.api.NecroMobRegistry;
import com.sirolf2009.necromancy.api.PartType;
import com.sirolf2009.necromancy.registry.NecroComponents;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

/**
 * A body part of another mod's creature (OreSpawn's, for one): a single item whose {@code necromancy:creature_part}
 * component names the mob and the part type. Named "&lt;creature&gt; Head" and so on, drawn from the creature's own model.
 */
public class CreaturePartItem extends BodyPartItem {
    public CreaturePartItem(Properties properties) {
        super(properties);
    }

    public record Part(ResourceLocation mob, PartType type) {
        public static final Codec<PartType> TYPE_CODEC = StringRepresentable.fromEnum(PartType::values);
        public static final Codec<Part> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("mob").forGetter(Part::mob),
                TYPE_CODEC.fieldOf("type").forGetter(Part::type)
        ).apply(instance, Part::new));
        public static final StreamCodec<ByteBuf, Part> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Part::mob,
                ByteBufCodecs.idMapper(i -> PartType.values()[i], PartType::ordinal), Part::type,
                Part::new);
    }

    public static ItemStack of(net.minecraft.world.item.Item item, ResourceLocation mob, PartType type) {
        ItemStack stack = new ItemStack(item);
        stack.set(NecroComponents.CREATURE_PART.get(), new Part(mob, type));
        return stack;
    }

    /** Wires the API hooks once the item and component exist. */
    public static void bind(java.util.function.Supplier<? extends net.minecraft.world.item.Item> item) {
        NecroMob.CREATURE_PART_ITEM = item;
        NecroMob.CREATURE_PART_STACK = (mob, type) -> of(item.get(), mob, type);
        NecroMobRegistry.CREATURE_PART_READER = stack -> {
            Part part = stack.get(NecroComponents.CREATURE_PART.get());
            NecroMob mob = part == null ? null : NecroMobRegistry.get(part.mob());
            return mob == null || !mob.hasPart(part.type()) ? null : new NecroMobRegistry.PartRef(mob, part.type());
        };
    }

    @Override
    public Component getName(ItemStack stack) {
        Part part = stack.get(NecroComponents.CREATURE_PART.get());
        NecroMob mob = part == null ? null : NecroMobRegistry.get(part.mob());
        if (mob == null) {
            return super.getName(stack);
        }
        return Component.translatable("item.necromancy.creature_part." + part.type().getSerializedName(), Component.translatable(mob.translationKey()));
    }
}
