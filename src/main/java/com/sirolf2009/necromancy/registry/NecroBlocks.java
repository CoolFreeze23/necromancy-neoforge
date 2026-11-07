package com.sirolf2009.necromancy.registry;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.block.AltarBlock;
import com.sirolf2009.necromancy.block.AltarSideBlock;
import com.sirolf2009.necromancy.block.SewingMachineBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NecroBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Necromancy.MOD_ID);

    public static final DeferredBlock<AltarBlock> SUMMONING_ALTAR = BLOCKS.register("summoning_altar", () -> new AltarBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .instrument(NoteBlockInstrument.BASS)
            .strength(4F)
            .sound(SoundType.WOOD)
            .noOcclusion()
            .pushReaction(PushReaction.BLOCK)));

    public static final DeferredBlock<AltarSideBlock> ALTAR_SIDE = BLOCKS.register("altar_side", () -> new AltarSideBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .strength(4F)
            .sound(SoundType.STONE)
            .noOcclusion()
            .pushReaction(PushReaction.BLOCK)));

    public static final DeferredBlock<SewingMachineBlock> SEWING_MACHINE = BLOCKS.register("sewing_machine", () -> new SewingMachineBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(4F)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .noOcclusion()));

    public static final DeferredBlock<LiquidBlock> BLOOD = BLOCKS.register("blood", () -> new LiquidBlock(NecroFluids.BLOOD.get(), BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED)
            .replaceable()
            .noCollission()
            .strength(100F)
            .pushReaction(PushReaction.DESTROY)
            .noLootTable()
            .liquid()
            .sound(SoundType.EMPTY)));

    private NecroBlocks() {
    }

    static Block[] all() {
        return new Block[] { SUMMONING_ALTAR.get(), ALTAR_SIDE.get(), SEWING_MACHINE.get() };
    }
}
