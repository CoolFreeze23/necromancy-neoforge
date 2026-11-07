package com.sirolf2009.necromancy.recipe;

import com.sirolf2009.necromancy.registry.NecroItems;
import com.sirolf2009.necromancy.registry.NecroRecipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

/** A Sewing Machine recipe on its 4x4 grid. Shaped ({@code necromancy:sewing_shaped}) or shapeless ({@code necromancy:sewing_shapeless}). */
public interface SewingRecipe extends Recipe<CraftingInput> {
    int SIZE = 4;

    @Override
    default RecipeType<?> getType() {
        return NecroRecipes.SEWING.get();
    }

    @Override
    default boolean canCraftInDimensions(int width, int height) {
        return width <= SIZE && height <= SIZE;
    }

    @Override
    default ItemStack getToastSymbol() {
        return new ItemStack(NecroItems.SEWING_MACHINE.get());
    }

    @Override
    default boolean isSpecial() {
        return false;
    }

    @Override
    default boolean showNotification() {
        return true;
    }
}
