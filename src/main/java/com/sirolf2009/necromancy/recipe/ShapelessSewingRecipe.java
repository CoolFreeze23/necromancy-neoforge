package com.sirolf2009.necromancy.recipe;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sirolf2009.necromancy.registry.NecroRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** A shapeless recipe for the 4x4 grid (up to 16 ingredients). JSON: {"ingredients": [...], "result": {...}}. */
public class ShapelessSewingRecipe implements SewingRecipe {
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;

    public ShapelessSewingRecipe(NonNullList<Ingredient> ingredients, ItemStack result) {
        this.ingredients = ingredients;
        this.result = result;
    }

    public ItemStack result() {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != ingredients.size()) {
            return false;
        }
        StackedContents contents = input.stackedContents();
        return contents.canCraft(this, null);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return NecroRecipes.SEWING_SHAPELESS.get();
    }

    public static final MapCodec<ShapelessSewingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap(list -> {
                if (list.isEmpty()) {
                    return DataResult.error(() -> "No ingredients for shapeless sewing recipe");
                }
                if (list.size() > SIZE * SIZE) {
                    return DataResult.error(() -> "Too many ingredients for shapeless sewing recipe (16 at most)");
                }
                NonNullList<Ingredient> out = NonNullList.create();
                out.addAll(list);
                return DataResult.success(out);
            }, DataResult::success).forGetter(r -> r.ingredients),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result)
    ).apply(instance, ShapelessSewingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShapelessSewingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC.apply(net.minecraft.network.codec.ByteBufCodecs.collection(NonNullList::createWithCapacity)), r -> r.ingredients,
            ItemStack.STREAM_CODEC, r -> r.result,
            ShapelessSewingRecipe::new);

    public static class Serializer implements RecipeSerializer<ShapelessSewingRecipe> {
        @Override
        public MapCodec<ShapelessSewingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ShapelessSewingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
