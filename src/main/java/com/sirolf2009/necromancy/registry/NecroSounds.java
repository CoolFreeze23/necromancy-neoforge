package com.sirolf2009.necromancy.registry;

import com.sirolf2009.necromancy.Necromancy;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NecroSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Necromancy.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> SPAWN = register("spawn");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEAR = register("tear");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIGHTCRAWLER_HOWL = register("nightcrawler.howl");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIGHTCRAWLER_SCREAM = register("nightcrawler.scream");

    private NecroSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Necromancy.id(name)));
    }
}
