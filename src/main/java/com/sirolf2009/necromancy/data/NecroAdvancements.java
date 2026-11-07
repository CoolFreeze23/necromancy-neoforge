package com.sirolf2009.necromancy.data;

import java.util.function.Consumer;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.registry.NecroBlocks;
import com.sirolf2009.necromancy.registry.NecroEntities;
import com.sirolf2009.necromancy.registry.NecroItems;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemUsedOnLocationTrigger;
import net.minecraft.advancements.critereon.KilledTrigger;
import net.minecraft.advancements.critereon.SummonedEntityTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * The "Necromancy" advancement tab (docs/REVIEW.md #15), on vanilla triggers only. Titles and descriptions are
 * {@code advancements.necromancy.<name>.title|description} in the language file.
 *
 * <pre>
 * Forbidden Knowledge ─┬─ Bound in Blood ── It's Alive!
 *                      ├─ Stitches ── Blood Bank
 *                      ├─ Soul Reaper
 *                      ├─ Night Terror
 *                      └─ Crying Shame ── Basement Dweller
 * </pre>
 *
 * "Mix and Match" (parts from five different mobs) needs the Minion's parts, which no vanilla entity predicate can
 * read, so it is left for a custom trigger.
 */
public class NecroAdvancements implements AdvancementProvider.AdvancementGenerator {
    public static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/block/soul_soil.png");

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper files) {
        AdvancementHolder root = Advancement.Builder.advancement()
                .display(NecroItems.NECRONOMICON.get(), title("root"), description("root"), BACKGROUND, AdvancementType.TASK, true, true, false)
                .addCriterion("has_necronomicon", InventoryChangeTrigger.TriggerInstance.hasItems(NecroItems.NECRONOMICON.get()))
                .save(saver, id("root"));

        AdvancementHolder bound = child(saver, root, "bound_in_blood", NecroItems.JAR_OF_BLOOD.get(), AdvancementType.TASK,
                "bound_altar", ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(NecroBlocks.SUMMONING_ALTAR.get()));
        child(saver, bound, "its_alive", NecroItems.SOUL_IN_A_JAR.get(), AdvancementType.GOAL,
                "summoned_minion", SummonedEntityTrigger.TriggerInstance.summonedEntity(EntityPredicate.Builder.entity().of(NecroEntities.MINION.get())));

        AdvancementHolder stitches = child(saver, root, "stitches", NecroItems.BONE_NEEDLE.get(), AdvancementType.TASK,
                "has_sewing_machine", InventoryChangeTrigger.TriggerInstance.hasItems(NecroItems.SEWING_MACHINE.get()));
        child(saver, stitches, "blood_bank", NecroItems.BLOOD_BUCKET.get(), AdvancementType.TASK,
                "has_blood_bucket", InventoryChangeTrigger.TriggerInstance.hasItems(NecroItems.BLOOD_BUCKET.get()));

        child(saver, root, "soul_reaper", NecroItems.SOUL_IN_A_JAR.get(), AdvancementType.TASK,
                "has_soul", InventoryChangeTrigger.TriggerInstance.hasItems(NecroItems.SOUL_IN_A_JAR.get()));
        child(saver, root, "night_terror", NecroItems.NIGHT_CRAWLER_SPAWN_EGG.get(), AdvancementType.TASK,
                "killed_night_crawler", killed(NecroEntities.NIGHT_CRAWLER.get()));
        AdvancementHolder crying = child(saver, root, "crying_shame", NecroItems.TEAR.get(), AdvancementType.TASK,
                "killed_blood_isaac", killed(NecroEntities.BLOOD_ISAAC.get()));
        child(saver, crying, "basement_dweller", NecroItems.ISAACS_HEAD.get(), AdvancementType.CHALLENGE,
                "has_isaacs_head", InventoryChangeTrigger.TriggerInstance.hasItems(NecroItems.ISAACS_HEAD.get()));
    }

    private static AdvancementHolder child(Consumer<AdvancementHolder> saver, AdvancementHolder parent, String name, ItemLike icon, AdvancementType type,
            String criterion, Criterion<?> trigger) {
        return Advancement.Builder.advancement()
                .parent(parent)
                .display(icon, title(name), description(name), null, type, true, true, false)
                .addCriterion(criterion, trigger)
                .save(saver, id(name));
    }

    private static Criterion<KilledTrigger.TriggerInstance> killed(EntityType<?> type) {
        return KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(type));
    }

    private static String id(String name) {
        return Necromancy.id("necromancy/" + name).toString();
    }

    private static Component title(String name) {
        return Component.translatable("advancements.necromancy." + name + ".title");
    }

    private static Component description(String name) {
        return Component.translatable("advancements.necromancy." + name + ".description");
    }
}
