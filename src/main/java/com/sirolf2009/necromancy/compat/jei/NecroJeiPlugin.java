package com.sirolf2009.necromancy.compat.jei;

import java.util.List;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.recipe.SewingRecipe;
import com.sirolf2009.necromancy.registry.NecroItems;
import com.sirolf2009.necromancy.registry.NecroRecipes;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * JEI support: the Sewing category and info pages. JEI is an optional dependency; this class is only loaded by JEI
 * itself (it finds plugins by the annotation), so the mod runs without it.
 */
@JeiPlugin
public class NecroJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = Necromancy.id("jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    /** Creature parts are one item: tell their mob and part type apart, so each shows with its own recipe. */
    @Override
    public void registerItemSubtypes(mezz.jei.api.registration.ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(com.sirolf2009.necromancy.registry.NecroItems.CREATURE_PART.get(),
                new mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter<net.minecraft.world.item.ItemStack>() {
                    @Override
                    public Object getSubtypeData(net.minecraft.world.item.ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
                        return stack.get(com.sirolf2009.necromancy.registry.NecroComponents.CREATURE_PART.get());
                    }

                    @Override
                    public String getLegacyStringSubtypeInfo(net.minecraft.world.item.ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
                        var part = stack.get(com.sirolf2009.necromancy.registry.NecroComponents.CREATURE_PART.get());
                        return part == null ? "" : part.mob() + "/" + part.type().getSerializedName();
                    }
                });
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new SewingCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            List<RecipeHolder<SewingRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(NecroRecipes.SEWING.get());
            registration.addRecipes(SewingCategory.TYPE, recipes);
        }

        // The Summoning Altar has no item of its own: its page sits on the book that binds it.
        registration.addIngredientInfo(NecroItems.NECRONOMICON.get(), Component.translatable("jei.necromancy.info.altar"));
        registration.addItemStackInfo(List.of(new ItemStack(NecroItems.BRAINS.get()), new ItemStack(NecroItems.HEART.get()),
                new ItemStack(NecroItems.MUSCLE.get()), new ItemStack(NecroItems.LUNGS.get())), Component.translatable("jei.necromancy.info.organs"));
        registration.addIngredientInfo(NecroItems.SKIN.get(), Component.translatable("jei.necromancy.info.skin"));
        registration.addIngredientInfo(NecroItems.SOUL_IN_A_JAR.get(), Component.translatable("jei.necromancy.info.soul_in_a_jar"));
        registration.addIngredientInfo(NecroItems.JAR_OF_BLOOD.get(), Component.translatable("jei.necromancy.info.jar_of_blood"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(NecroItems.SEWING_MACHINE.get(), SewingCategory.TYPE);
    }
}
