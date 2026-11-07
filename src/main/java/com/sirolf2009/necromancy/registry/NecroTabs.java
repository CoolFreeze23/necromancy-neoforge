package com.sirolf2009.necromancy.registry;

import com.sirolf2009.necromancy.Necromancy;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NecroTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Necromancy.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("necromancy", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.necromancy"))
            .icon(() -> new ItemStack(NecroItems.NECRONOMICON.get()))
            .displayItems((params, output) -> {
                output.accept(NecroItems.NECRONOMICON.get());
                output.accept(NecroItems.SEWING_MACHINE.get());
                output.accept(NecroItems.BONE_NEEDLE.get());
                output.accept(NecroItems.JAR_OF_BLOOD.get());
                output.accept(NecroItems.SOUL_IN_A_JAR.get());
                output.accept(NecroItems.BLOOD_BUCKET.get());
                output.accept(NecroItems.BLOOD_SCYTHE.get());
                output.accept(NecroItems.BONE_SCYTHE.get());
                output.accept(NecroItems.NETHERITE_SCYTHE.get());
                output.accept(NecroItems.BONE_SCEPTER.get());
                output.accept(NecroItems.PHYLACTERY.get());
                output.accept(NecroItems.BRAIN_ON_A_STICK.get());
                output.accept(NecroItems.BRAINS.get());
                output.accept(NecroItems.HEART.get());
                output.accept(NecroItems.MUSCLE.get());
                output.accept(NecroItems.LUNGS.get());
                output.accept(NecroItems.SKIN.get());
                output.accept(NecroItems.ISAACS_HEAD.get());
                output.accept(NecroItems.SOUL_HEART.get());
                output.accept(NecroItems.TEDDY_SPAWN_EGG.get());
                output.accept(NecroItems.NIGHT_CRAWLER_SPAWN_EGG.get());
                output.accept(NecroItems.ISAAC_SPAWN_EGG.get());
                output.accept(NecroItems.BLOOD_ISAAC_SPAWN_EGG.get());
                output.accept(NecroItems.ISAAC_HEAD_SPAWN_EGG.get());
                output.accept(NecroItems.ISAAC_BODY_SPAWN_EGG.get());
            })
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BODY_PARTS = TABS.register("body_parts", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.necromancy.body_parts"))
            .icon(() -> new ItemStack(NecroItems.part("zombie", com.sirolf2009.necromancy.api.PartType.TORSO).get()))
            .withTabsBefore(MAIN.getKey())
            .displayItems((params, output) -> {
                for (var byType : NecroItems.BODY_PARTS.values()) {
                    for (DeferredItem<?> item : byType.values()) {
                        output.accept(item.get());
                    }
                }
                // creatures of other mods (OreSpawn): one creature part item per mob and part
                for (var mob : com.sirolf2009.necromancy.api.NecroMobRegistry.all()) {
                    for (var type : com.sirolf2009.necromancy.api.PartType.values()) {
                        if (mob.isCreaturePart(type)) {
                            output.accept(mob.partStack(type));
                        }
                    }
                }
            })
            .build());

    private NecroTabs() {
    }
}
