package com.sirolf2009.necromancy.soul;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

/** The {@code necromancy:soul} component of a Soul in a Jar: what it was reaped from and how strong it is. */
public record SoulData(Optional<ResourceLocation> entity, SoulPotency potency) {
    public static final SoulData UNKNOWN = new SoulData(Optional.empty(), SoulPotency.COMMON);

    public static final Codec<SoulData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("entity").forGetter(SoulData::entity),
            SoulPotency.CODEC.optionalFieldOf("potency", SoulPotency.COMMON).forGetter(SoulData::potency)
    ).apply(instance, SoulData::new));

    public static final StreamCodec<ByteBuf, SoulData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), SoulData::entity,
            ByteBufCodecs.idMapper(i -> SoulPotency.values()[i], SoulPotency::ordinal), SoulData::potency,
            SoulData::new);

    public static SoulData of(EntityType<?> type, SoulPotency potency) {
        return new SoulData(Optional.of(BuiltInRegistries.ENTITY_TYPE.getKey(type)), potency);
    }

    /** The soul in a stack: a jar without the component (creative, trades) holds a Common soul. */
    public static SoulData get(ItemStack stack) {
        SoulData data = stack.get(com.sirolf2009.necromancy.registry.NecroComponents.SOUL.get());
        return data == null ? UNKNOWN : data;
    }

    public Component description() {
        return entity.flatMap(BuiltInRegistries.ENTITY_TYPE::getOptional)
                .map(type -> (Component) Component.translatable("soul.necromancy.of", type.getDescription(), potency.displayName()))
                .orElseGet(() -> Component.translatable("soul.necromancy.nameless", potency.displayName()));
    }
}
