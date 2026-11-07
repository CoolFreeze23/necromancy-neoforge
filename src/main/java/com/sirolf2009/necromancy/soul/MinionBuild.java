package com.sirolf2009.necromancy.soul;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sirolf2009.necromancy.api.BodyPartLocation;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * Everything needed to raise the same Minion again: its parts, soul, name and kills. Stored on Minion Remains
 * ({@code necromancy:minion_build}) when a Minion bound to a Phylactery dies (docs/REVIEW.md #7).
 */
public record MinionBuild(Map<String, ResourceLocation> parts, SoulData soul, Optional<Component> name, int kills) {
    public static final Codec<MinionBuild> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC).fieldOf("parts").forGetter(MinionBuild::parts),
            SoulData.CODEC.optionalFieldOf("soul", SoulData.UNKNOWN).forGetter(MinionBuild::soul),
            ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(MinionBuild::name),
            Codec.INT.optionalFieldOf("kills", 0).forGetter(MinionBuild::kills)
    ).apply(instance, MinionBuild::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MinionBuild> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ResourceLocation.STREAM_CODEC), MinionBuild::parts,
            SoulData.STREAM_CODEC.cast(), MinionBuild::soul,
            ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC), MinionBuild::name,
            ByteBufCodecs.VAR_INT, MinionBuild::kills,
            MinionBuild::new);

    public static MinionBuild of(Map<BodyPartLocation, ResourceLocation> parts, SoulData soul, Optional<Component> name, int kills) {
        Map<String, ResourceLocation> map = new HashMap<>();
        parts.forEach((location, mob) -> map.put(location.getSerializedName(), mob));
        return new MinionBuild(map, soul, name, kills);
    }

    public Map<BodyPartLocation, ResourceLocation> locations() {
        Map<BodyPartLocation, ResourceLocation> map = new EnumMap<>(BodyPartLocation.class);
        for (BodyPartLocation location : BodyPartLocation.values()) {
            ResourceLocation mob = parts.get(location.getSerializedName());
            if (mob != null) {
                map.put(location, mob);
            }
        }
        return map;
    }
}
