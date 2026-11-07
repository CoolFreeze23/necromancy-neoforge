package com.sirolf2009.necromancy.registry;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.entity.MinionOrder;
import com.sirolf2009.necromancy.soul.MinionBuild;
import com.sirolf2009.necromancy.soul.SoulData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NecroComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Necromancy.MOD_ID);

    /** What a Soul in a Jar was reaped from and how strong it is. */
    public static final Supplier<DataComponentType<SoulData>> SOUL = COMPONENTS.registerComponentType("soul",
            b -> b.persistent(SoulData.CODEC).networkSynchronized(SoulData.STREAM_CODEC));

    /** Minion Remains: the Minion to raise again. */
    public static final Supplier<DataComponentType<MinionBuild>> MINION_BUILD = COMPONENTS.registerComponentType("minion_build",
            b -> b.persistent(MinionBuild.CODEC).networkSynchronized(MinionBuild.STREAM_CODEC));

    /** Phylactery: the Minion bound to it. */
    public static final Supplier<DataComponentType<UUID>> BOUND_MINION = COMPONENTS.registerComponentType("bound_minion",
            b -> b.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

    /** Bone Scepter: the selected Minions. */
    public static final Supplier<DataComponentType<List<UUID>>> SELECTED_MINIONS = COMPONENTS.registerComponentType("selected_minions",
            b -> b.persistent(UUIDUtil.CODEC.listOf()).networkSynchronized(UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list())));

    /** Bone Scepter: the order it will give. */
    public static final Supplier<DataComponentType<MinionOrder>> SCEPTER_ORDER = COMPONENTS.registerComponentType("scepter_order",
            b -> b.persistent(net.minecraft.util.StringRepresentable.fromEnum(MinionOrder::values))
                    .networkSynchronized(ByteBufCodecs.idMapper(i -> MinionOrder.values()[i], MinionOrder::ordinal)));

    /** Bone Scepter: the patrol route being laid out. */
    public static final Supplier<DataComponentType<List<BlockPos>>> PATROL_ROUTE = COMPONENTS.registerComponentType("patrol_route",
            b -> b.persistent(BlockPos.CODEC.listOf()).networkSynchronized(BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list())));

    /** The generic creature part: which mob and which part it is. */
    public static final Supplier<DataComponentType<com.sirolf2009.necromancy.item.CreaturePartItem.Part>> CREATURE_PART = COMPONENTS.registerComponentType(
            "creature_part", b -> b.persistent(com.sirolf2009.necromancy.item.CreaturePartItem.Part.CODEC)
                    .networkSynchronized(com.sirolf2009.necromancy.item.CreaturePartItem.Part.STREAM_CODEC));

    private NecroComponents() {
    }
}
