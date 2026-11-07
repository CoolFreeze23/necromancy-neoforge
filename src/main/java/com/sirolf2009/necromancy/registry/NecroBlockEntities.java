package com.sirolf2009.necromancy.registry;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.blockentity.AltarBlockEntity;
import com.sirolf2009.necromancy.blockentity.SewingMachineBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NecroBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Necromancy.MOD_ID);

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AltarBlockEntity>> ALTAR = BLOCK_ENTITIES.register("summoning_altar",
            () -> BlockEntityType.Builder.of(AltarBlockEntity::new, NecroBlocks.SUMMONING_ALTAR.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SewingMachineBlockEntity>> SEWING_MACHINE = BLOCK_ENTITIES.register("sewing_machine",
            () -> BlockEntityType.Builder.of(SewingMachineBlockEntity::new, NecroBlocks.SEWING_MACHINE.get()).build(null));

    private NecroBlockEntities() {
    }
}
