package com.sirolf2009.necromancy.data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.api.NecroMob;
import com.sirolf2009.necromancy.api.NecroMobRegistry;
import com.sirolf2009.necromancy.api.PartType;
import com.sirolf2009.necromancy.registry.NecroItems;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/** The original mod's recipes: crafting table tools, and the Sewing Machine's body parts and oddities. */
public class NecroRecipeProvider extends RecipeProvider {
    public NecroRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        crafting(output);
        sewingExtras(output);
        bodyParts(output);
    }

    // --- crafting table (the original's RegistryNecromancyItems.initRecipes and the Sewing Machine block) ---

    private void crafting(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, NecroItems.NECRONOMICON.get())
                .pattern("LSL").pattern("IBF").pattern("LNL")
                .define('L', Items.LEATHER).define('S', NecroItems.JAR_OF_BLOOD.get()).define('I', Items.INK_SAC)
                .define('B', Items.BOOK).define('F', Items.FEATHER).define('N', Items.NETHER_WART)
                .unlockedBy("has_jar_of_blood", has(NecroItems.JAR_OF_BLOOD.get()))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, NecroItems.BONE_NEEDLE.get())
                .requires(Items.BONE_MEAL)
                .unlockedBy("has_bone_meal", has(Items.BONE_MEAL))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NecroItems.BLOOD_SCYTHE.get())
                .pattern("IH").pattern(" S").pattern(" B")
                .define('I', Items.OBSIDIAN).define('H', Items.IRON_HOE).define('S', Items.STICK).define('B', NecroItems.JAR_OF_BLOOD.get())
                .unlockedBy("has_jar_of_blood", has(NecroItems.JAR_OF_BLOOD.get()))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NecroItems.BONE_SCYTHE.get())
                .pattern("IH").pattern(" S").pattern(" B")
                .define('I', Items.OBSIDIAN).define('H', NecroItems.BLOOD_SCYTHE.get()).define('S', Items.BONE).define('B', Items.DIAMOND)
                .unlockedBy("has_blood_scythe", has(NecroItems.BLOOD_SCYTHE.get()))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, NecroItems.BRAIN_ON_A_STICK.get())
                .pattern("# ").pattern(" X")
                .define('#', Items.FISHING_ROD).define('X', NecroItems.BRAINS.get())
                .unlockedBy("has_brains", has(NecroItems.BRAINS.get()))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, NecroItems.JAR_OF_BLOOD.get(), 8)
                .requires(NecroItems.BLOOD_BUCKET.get())
                .requires(Items.GLASS_BOTTLE, 8)
                .unlockedBy("has_blood_bucket", has(NecroItems.BLOOD_BUCKET.get()))
                .save(output, Necromancy.id("jar_of_blood_from_bucket"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, NecroItems.BLOOD_BUCKET.get())
                .requires(Items.BUCKET)
                .requires(NecroItems.JAR_OF_BLOOD.get(), 8)
                .unlockedBy("has_jar_of_blood", has(NecroItems.JAR_OF_BLOOD.get()))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, NecroItems.SEWING_MACHINE.get())
                .pattern("III").pattern("ISB").pattern("III")
                .define('I', Items.IRON_INGOT).define('S', Items.STRING).define('B', NecroItems.BONE_NEEDLE.get())
                .unlockedBy("has_bone_needle", has(NecroItems.BONE_NEEDLE.get()))
                .save(output);
    }

    // --- Sewing Machine: the original CraftingManagerSewing extras ---

    private void sewingExtras(RecipeOutput output) {
        SewingRecipeBuilder.shapeless(NecroItems.SKIN.get(), 8)
                .requires(Items.LEATHER, 1)
                .save(output, Necromancy.id("sewing/skin"));
        SewingRecipeBuilder.shapeless(NecroItems.SOUL_HEART.get(), 1)
                .requires(Items.ROTTEN_FLESH, 5)
                .requires(Items.GHAST_TEAR, 2)
                .requires(NecroItems.SOUL_IN_A_JAR.get(), 1)
                .requires(NecroItems.HEART.get(), 1)
                .save(output, Necromancy.id("sewing/soul_heart"));
        SewingRecipeBuilder.shaped(NecroItems.TEDDY_SPAWN_EGG.get())
                .pattern("LLLL", "LWWL", "LWWL", "LLLL")
                .define('L', Items.LEATHER).define('W', ItemTags.WOOL)
                .save(output, Necromancy.id("sewing/teddy_spawn_egg"));
    }

    // --- Sewing Machine: every sewn body part (the original NecroEntityBase.initDefaultRecipes shapes) ---

    private void bodyParts(RecipeOutput output) {
        Map<String, String> shapes = new HashMap<>();
        for (NecroMob mob : NecroMobRegistry.all()) {
            for (PartType type : PartType.values()) {
                if (!mob.isSewable(type)) {
                    continue;
                }
                if (mob.isCreaturePart(type)) {
                    continue;
                }
                Item item = mob.partItem(type).orElseThrow();
                Ingredient essence = mob.essence(type);
                String shape = type.getSerializedName() + " " + Arrays.stream(essence.getItems())
                        .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).sorted().toList();
                String clash = shapes.putIfAbsent(shape, mob.id().toString());
                if (clash != null) {
                    // same pattern and essence: the Sewing Machine can only ever make one of the two
                    Necromancy.LOGGER.warn("Sewing recipe clash: the {} of {} and of {} use the same pattern and essence {}", type.getSerializedName(),
                            clash, mob.id(), shape);
                }
                SewingRecipeBuilder recipe = SewingRecipeBuilder.shaped(item);
                switch (type) {
                    case HEAD -> recipe.pattern("SSSS", "SBFS", "SEES")
                            .define('S', NecroItems.SKIN.get()).define('B', NecroItems.BRAINS.get()).define('F', essence).define('E', Items.SPIDER_EYE);
                    case TORSO -> recipe.pattern(" LL ", "BHUB", "LEEL", "BLLB")
                            .define('L', NecroItems.SKIN.get()).define('E', essence).define('H', NecroItems.HEART.get())
                            .define('U', NecroItems.LUNGS.get()).define('B', Items.BONE);
                    case ARM -> recipe.pattern("LLLL", "BMEB", "LLLL")
                            .define('L', NecroItems.SKIN.get()).define('E', essence).define('M', NecroItems.MUSCLE.get()).define('B', Items.BONE);
                    case LEGS -> recipe.pattern("LBBL", "LMML", "LEEL", "LBBL")
                            .define('L', NecroItems.SKIN.get()).define('E', essence).define('M', NecroItems.MUSCLE.get()).define('B', Items.BONE);
                }
                recipe.save(output, Necromancy.id("sewing/" + BuiltInRegistries.ITEM.getKey(item).getPath()));
            }
        }
    }
}
