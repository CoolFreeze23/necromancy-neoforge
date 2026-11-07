package com.sirolf2009.necromancy.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sirolf2009.necromancy.recipe.ShapedSewingRecipe;
import com.sirolf2009.necromancy.recipe.ShapelessSewingRecipe;

import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/**
 * Writes Sewing Machine recipes ({@code necromancy:sewing_shaped} / {@code necromancy:sewing_shapeless}). The Sewing
 * Machine has no recipe book, so the recipes carry no unlock advancement: they are always known.
 */
public final class SewingRecipeBuilder {
    private final ItemStack result;
    private final List<String> pattern = new ArrayList<>();
    private final Map<Character, Ingredient> key = new LinkedHashMap<>();
    private final NonNullList<Ingredient> ingredients = NonNullList.create();
    private final boolean shaped;

    private SewingRecipeBuilder(ItemStack result, boolean shaped) {
        this.result = result;
        this.shaped = shaped;
    }

    public static SewingRecipeBuilder shaped(ItemLike result) {
        return shaped(result, 1);
    }

    public static SewingRecipeBuilder shaped(ItemLike result, int count) {
        return new SewingRecipeBuilder(new ItemStack(result, count), true);
    }

    public static SewingRecipeBuilder shapeless(ItemLike result, int count) {
        return new SewingRecipeBuilder(new ItemStack(result, count), false);
    }

    public SewingRecipeBuilder pattern(String... rows) {
        pattern.addAll(List.of(rows));
        return this;
    }

    public SewingRecipeBuilder define(char symbol, Ingredient ingredient) {
        if (ingredient.isEmpty()) {
            throw new IllegalArgumentException("Empty ingredient for '" + symbol + "' in the sewing recipe for " + result);
        }
        key.put(symbol, ingredient);
        return this;
    }

    public SewingRecipeBuilder define(char symbol, ItemLike item) {
        return define(symbol, Ingredient.of(item));
    }

    public SewingRecipeBuilder define(char symbol, TagKey<Item> tag) {
        return define(symbol, Ingredient.of(tag));
    }

    public SewingRecipeBuilder requires(ItemLike item, int count) {
        for (int i = 0; i < count; i++) {
            ingredients.add(Ingredient.of(item));
        }
        return this;
    }

    public void save(RecipeOutput output, ResourceLocation id) {
        if (shaped) {
            output.accept(id, new ShapedSewingRecipe(pattern, key, result), null);
        } else {
            output.accept(id, new ShapelessSewingRecipe(ingredients, result), null);
        }
    }
}
