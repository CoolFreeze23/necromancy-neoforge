package com.sirolf2009.necromancy.data;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.registry.NecroSounds;

import net.minecraft.data.PackOutput;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;
import net.neoforged.neoforge.registries.DeferredHolder;

/** sounds.json: the original mod's four sounds, each with a subtitle. */
public class NecroSoundDefinitions extends SoundDefinitionsProvider {
    public NecroSoundDefinitions(PackOutput output, ExistingFileHelper files) {
        super(output, Necromancy.MOD_ID, files);
    }

    @Override
    public void registerSounds() {
        sound(NecroSounds.SPAWN, "spawn");
        sound(NecroSounds.TEAR, "tear");
        sound(NecroSounds.NIGHTCRAWLER_HOWL, "nightcrawler/howl");
        sound(NecroSounds.NIGHTCRAWLER_SCREAM, "nightcrawler/scream");
    }

    private void sound(DeferredHolder<SoundEvent, SoundEvent> event, String file) {
        add(event, definition()
                .subtitle("subtitles." + Necromancy.MOD_ID + "." + event.getId().getPath())
                .with(sound(Necromancy.id(file))));
    }
}
