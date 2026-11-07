package com.sirolf2009.necromancy.recipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sirolf2009.necromancy.registry.NecroRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** A shaped recipe up to 4x4, mirrored like vanilla's. JSON: {"pattern": [...], "key": {...}, "result": {...}}. */
public class ShapedSewingRecipe implements SewingRecipe {
    private final List<String> pattern;
    private final Map<Character, Ingredient> key;
    private final ItemStack result;
    private final int width;
    private final int height;
    private final NonNullList<Ingredient> ingredients;

    public ShapedSewingRecipe(List<String> pattern, Map<Character, Ingredient> key, ItemStack result) {
        this.pattern = List.copyOf(pattern);
        this.key = Map.copyOf(key);
        this.result = result;
        this.height = pattern.size();
        this.width = pattern.get(0).length();
        this.ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
        for (int y = 0; y < height; y++) {
            String row = pattern.get(y);
            for (int x = 0; x < width; x++) {
                char c = row.charAt(x);
                ingredients.set(x + y * width, c == ' ' ? Ingredient.EMPTY : key.get(c));
            }
        }
    }

    public List<String> pattern() {
        return pattern;
    }

    public Map<Character, Ingredient> key() {
        return key;
    }

    public ItemStack result() {
        return result;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != countIngredients() || input.width() != width || input.height() != height) {
            return false;
        }
        return matches(input, false) || matches(input, true);
    }

    private int countIngredients() {
        int n = 0;
        for (Ingredient ingredient : ingredients) {
            if (!ingredient.isEmpty()) {
                n++;
            }
        }
        return n;
    }

    private boolean matches(CraftingInput input, boolean mirrored) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Ingredient ingredient = mirrored ? ingredients.get(width - x - 1 + y * width) : ingredients.get(x + y * width);
                if (!ingredient.test(input.getItem(x, y))) {
                    return false;
                }
            }
        }
        return true;
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
    public net.minecraft.world.item.crafting.RecipeSerializer<?> getSerializer() {
        return NecroRecipes.SEWING_SHAPED.get();
    }

    private static final Codec<Character> SYMBOL = Codec.STRING.comapFlatMap(s -> s.length() == 1 && !" ".equals(s)
            ? DataResult.success(s.charAt(0)) : DataResult.error(() -> "Key '" + s + "' must be one character, not a space"), String::valueOf);

    private static final Codec<List<String>> PATTERN = Codec.STRING.listOf().comapFlatMap(rows -> {
        if (rows.isEmpty() || rows.size() > SIZE) {
            return DataResult.error(() -> "Sewing pattern needs 1 to 4 rows");
        }
        int w = rows.get(0).length();
        for (String row : rows) {
            if (row.length() != w || w == 0 || w > SIZE) {
                return DataResult.error(() -> "Sewing pattern rows must all be 1 to 4 wide and the same width");
            }
        }
        return DataResult.success(rows);
    }, rows -> rows);

    public static final MapCodec<ShapedSewingRecipe> CODEC = RecordCodecBuilder.<ShapedSewingRecipe>mapCodec(instance -> instance.group(
            PATTERN.fieldOf("pattern").forGetter(r -> r.pattern),
            ExtraCodecs.strictUnboundedMap(SYMBOL, Ingredient.CODEC_NONEMPTY).fieldOf("key").forGetter(r -> r.key),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result)
    ).apply(instance, ShapedSewingRecipe::new)).validate(recipe -> {
        Set<Character> used = new HashSet<>();
        for (String row : recipe.pattern) {
            for (char c : row.toCharArray()) {
                if (c != ' ') {
                    if (!recipe.key.containsKey(c)) {
                        return DataResult.error(() -> "Pattern uses '" + c + "' but the key does not define it");
                    }
                    used.add(c);
                }
            }
        }
        return used.size() == recipe.key.size() ? DataResult.success(recipe) : DataResult.error(() -> "Key defines symbols the pattern never uses");
    });

    public static final StreamCodec<RegistryFriendlyByteBuf, ShapedSewingRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, recipe) -> {
                ByteBufCodecs.VAR_INT.encode(buf, recipe.pattern.size());
                for (String row : recipe.pattern) {
                    buf.writeUtf(row);
                }
                ByteBufCodecs.VAR_INT.encode(buf, recipe.key.size());
                recipe.key.forEach((c, ingredient) -> {
                    buf.writeChar(c);
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
                });
                ItemStack.STREAM_CODEC.encode(buf, recipe.result);
            },
            buf -> {
                int rows = ByteBufCodecs.VAR_INT.decode(buf);
                List<String> pattern = new ArrayList<>();
                for (int i = 0; i < rows; i++) {
                    pattern.add(buf.readUtf());
                }
                int keys = ByteBufCodecs.VAR_INT.decode(buf);
                Map<Character, Ingredient> key = new java.util.HashMap<>();
                for (int i = 0; i < keys; i++) {
                    char c = buf.readChar();
                    key.put(c, Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                }
                return new ShapedSewingRecipe(pattern, key, ItemStack.STREAM_CODEC.decode(buf));
            });

    public static class Serializer implements RecipeSerializer<ShapedSewingRecipe> {
        @Override
        public MapCodec<ShapedSewingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ShapedSewingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
