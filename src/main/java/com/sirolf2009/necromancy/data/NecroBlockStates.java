package com.sirolf2009.necromancy.data;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.registry.NecroBlocks;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Blockstates for the mod's blocks. Their visible shapes are drawn by block entity renderers (altar, sewing machine) or
 * by the fluid renderer (blood), so each block model only names a particle texture for break and landing particles.
 */
public class NecroBlockStates extends BlockStateProvider {
    public NecroBlockStates(PackOutput output, ExistingFileHelper files) {
        super(output, Necromancy.MOD_ID, files);
    }

    @Override
    protected void registerStatesAndModels() {
        particleOnly(NecroBlocks.SUMMONING_ALTAR.get(), "summoning_altar", mcLoc("block/oak_planks"));
        particleOnly(NecroBlocks.ALTAR_SIDE.get(), "altar_side", mcLoc("block/cobblestone"));
        particleOnly(NecroBlocks.SEWING_MACHINE.get(), "sewing_machine", mcLoc("block/iron_block"));
        particleOnly(NecroBlocks.BLOOD.get(), "blood", mcLoc("block/water_still"));
    }

    /** One model for every state (facing, distance, fluid level): the renderer handles the rest. */
    private void particleOnly(Block block, String name, ResourceLocation particle) {
        ModelFile model = models().getBuilder("block/" + name).texture("particle", particle);
        simpleBlock(block, model);
    }
}
