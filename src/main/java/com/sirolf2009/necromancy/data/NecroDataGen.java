package com.sirolf2009.necromancy.data;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.sirolf2009.necromancy.Necromancy;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/** Every generated asset and data file of the mod: {@code ./gradlew runData} writes them to src/generated/resources. */
@EventBusSubscriber(modid = Necromancy.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class NecroDataGen {
    private NecroDataGen() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper files = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        // client
        generator.addProvider(event.includeClient(), new NecroItemModels(output, files));
        generator.addProvider(event.includeClient(), new NecroBlockStates(output, files));
        generator.addProvider(event.includeClient(), new NecroSoundDefinitions(output, files));
        generator.addProvider(event.includeClient(), new NecroLanguage(output));
        generator.addProvider(event.includeClient(), new NecroLanguage.Fragments(output, "pt_br"));

        // server: the enchantment first, so the tag providers can see it
        DatapackBuiltinEntriesProvider builtins = generator.addProvider(event.includeServer(), new DatapackBuiltinEntriesProvider(output, lookup,
                new RegistrySetBuilder().add(Registries.ENCHANTMENT, NecroEnchantments::bootstrap), Set.of(Necromancy.MOD_ID)));
        CompletableFuture<HolderLookup.Provider> withEnchantments = builtins.getRegistryProvider();

        NecroTagProviders.BlockTagGen blockTags = generator.addProvider(event.includeServer(), new NecroTagProviders.BlockTagGen(output, lookup, files));
        generator.addProvider(event.includeServer(), new NecroTagProviders.ItemTagGen(output, lookup, blockTags.contentsGetter(), files));
        generator.addProvider(event.includeServer(), new NecroTagProviders.EntityTagGen(output, lookup, files));
        generator.addProvider(event.includeServer(), new NecroTagProviders.EnchantmentTagGen(output, withEnchantments, files));

        generator.addProvider(event.includeServer(), new NecroRecipeProvider(output, lookup));
        generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(NecroLootTables.Blocks::new, LootContextParamSets.BLOCK),
                new LootTableProvider.SubProviderEntry(NecroLootTables.Entities::new, LootContextParamSets.ENTITY)), lookup));
        generator.addProvider(event.includeServer(), new NecroLootModifiers(output, lookup));
        generator.addProvider(event.includeServer(), new AdvancementProvider(output, withEnchantments, files, List.of(new NecroAdvancements())));
    }
}
