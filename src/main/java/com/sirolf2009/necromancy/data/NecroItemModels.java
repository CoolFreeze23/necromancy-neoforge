package com.sirolf2009.necromancy.data;

import java.util.HashSet;
import java.util.Set;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.registry.NecroItems;

import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Flat sprite models for every item drawn from a texture in textures/item. The 3D items are modelled by hand in
 * src/main/resources ({@link #HAND_MADE}, {@link #HAND_MADE_PART_MOBS}); a missing texture fails the run.
 */
public class NecroItemModels extends ItemModelProvider {
    /** Items whose models are hand-written (block entity, OBJ or multi-part models). */
    public static final Set<String> HAND_MADE = Set.of("sewing_machine", "blood_scythe", "bone_scythe", "netherite_scythe", "bone_scepter", "phylactery", "minion_remains");
    /** Mobs whose body part items are modelled by hand (they have no flat icon). */
    public static final Set<String> HAND_MADE_PART_MOBS = Set.of("husk", "drowned", "bogged", "wither_skeleton", "blaze", "slime", "mooshroom",
            "snow_golem", "armadillo", "goat", "night_crawler");

    public NecroItemModels(PackOutput output, ExistingFileHelper files) {
        super(output, Necromancy.MOD_ID, files);
    }

    @Override
    protected void registerModels() {
        Set<Item> parts = new HashSet<>();
        NecroItems.BODY_PARTS.forEach((mob, byType) -> byType.values().forEach(item -> {
            parts.add(item.get());
            if (!HAND_MADE_PART_MOBS.contains(mob)) {
                String name = item.getId().getPath();
                withExistingParent(name, mcLoc("item/generated")).texture("layer0", modLoc("item/bodypart/" + name));
            }
        }));
        for (Holder<Item> holder : NecroItems.ITEMS.getEntries()) {
            String name = holder.unwrapKey().orElseThrow().location().getPath();
            Item item = holder.value();
            if (HAND_MADE.contains(name) || parts.contains(item)) {
                continue;
            }
            if (item instanceof DeferredSpawnEggItem) {
                spawnEggItem(item);
            } else if (item == NecroItems.BRAIN_ON_A_STICK.get()) {
                withExistingParent(name, mcLoc("item/handheld_rod")).texture("layer0", modLoc("item/" + name));
            } else {
                basicItem(item);
            }
        }
    }
}
