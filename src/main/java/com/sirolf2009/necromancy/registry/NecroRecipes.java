package com.sirolf2009.necromancy.registry;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.recipe.SewingRecipe;
import com.sirolf2009.necromancy.recipe.ShapedSewingRecipe;
import com.sirolf2009.necromancy.recipe.ShapelessSewingRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NecroRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Necromancy.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Necromancy.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<SewingRecipe>> SEWING = TYPES.register("sewing",
            () -> RecipeType.simple(Necromancy.id("sewing")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> SEWING_SHAPED = SERIALIZERS.register("sewing_shaped",
            ShapedSewingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> SEWING_SHAPELESS = SERIALIZERS.register("sewing_shapeless",
            ShapelessSewingRecipe.Serializer::new);

    private NecroRecipes() {
    }
}
