package com.sirolf2009.necromancy.registry;

import com.sirolf2009.necromancy.Necromancy;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Blood: a thick red fluid made from Jars of Blood. Other mods' "blood" fluids can be matched through the c:blood tag. */
public final class NecroFluids {
    public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Necromancy.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Necromancy.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> BLOOD_TYPE = TYPES.register("blood", () -> new FluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.necromancy.blood")
            .density(1100)
            .viscosity(1600)
            .temperature(310)
            .rarity(Rarity.UNCOMMON)
            .canSwim(true)
            .canDrown(true)
            .supportsBoating(true)
            .pathType(PathType.WATER)
            .adjacentPathType(null)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)));

    public static final DeferredHolder<Fluid, FlowingFluid> BLOOD = FLUIDS.register("blood", () -> new BaseFlowingFluid.Source(properties()));
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_BLOOD = FLUIDS.register("flowing_blood", () -> new BaseFlowingFluid.Flowing(properties()));

    private NecroFluids() {
    }

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(BLOOD_TYPE, BLOOD, FLOWING_BLOOD)
                .bucket(NecroItems.BLOOD_BUCKET)
                .block(NecroBlocks.BLOOD)
                .slopeFindDistance(3)
                .levelDecreasePerBlock(2)
                .tickRate(15)
                .explosionResistance(100F);
    }
}
