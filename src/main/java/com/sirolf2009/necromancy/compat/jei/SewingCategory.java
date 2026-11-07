package com.sirolf2009.necromancy.compat.jei;

import java.util.List;

import com.sirolf2009.necromancy.Necromancy;
import com.sirolf2009.necromancy.recipe.SewingRecipe;
import com.sirolf2009.necromancy.recipe.ShapedSewingRecipe;
import com.sirolf2009.necromancy.registry.NecroItems;
import com.sirolf2009.necromancy.registry.NecroTags;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

/** The Sewing Machine: a 4x4 grid, the needle and thread it uses, and the result. */
public class SewingCategory extends AbstractRecipeCategory<RecipeHolder<SewingRecipe>> {
    public static final RecipeType<RecipeHolder<SewingRecipe>> TYPE = RecipeType.createRecipeHolderType(Necromancy.id("sewing"));

    private static final int SLOT = 18;
    private static final int GRID = SewingRecipe.SIZE * SLOT;
    private static final int TOOLS_X = GRID + 8;
    private static final int ARROW_X = TOOLS_X + SLOT + 6;
    private static final int OUTPUT_X = ARROW_X + 24 + 8;
    private static final int WIDTH = OUTPUT_X + SLOT + 4;
    private static final int HEIGHT = GRID;

    private final IDrawable arrow;

    public SewingCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("jei.necromancy.category.sewing"), gui.createDrawableItemLike(NecroItems.SEWING_MACHINE.get()), WIDTH, HEIGHT);
        this.arrow = gui.getRecipeArrow();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<SewingRecipe> holder, IFocusGroup focuses) {
        SewingRecipe recipe = holder.value();
        List<Ingredient> ingredients = recipe.getIngredients();
        if (recipe instanceof ShapedSewingRecipe shaped) {
            for (int y = 0; y < SewingRecipe.SIZE; y++) {
                for (int x = 0; x < SewingRecipe.SIZE; x++) {
                    var slot = builder.addSlot(RecipeIngredientRole.INPUT, x * SLOT + 1, y * SLOT + 1).setStandardSlotBackground();
                    if (x < shaped.width() && y < shaped.height()) {
                        slot.addIngredients(ingredients.get(x + y * shaped.width()));
                    }
                }
            }
        } else {
            builder.setShapeless(OUTPUT_X, 0);
            for (int i = 0; i < SewingRecipe.SIZE * SewingRecipe.SIZE; i++) {
                var slot = builder.addSlot(RecipeIngredientRole.INPUT, i % SewingRecipe.SIZE * SLOT + 1, i / SewingRecipe.SIZE * SLOT + 1)
                        .setStandardSlotBackground();
                if (i < ingredients.size()) {
                    slot.addIngredients(ingredients.get(i));
                }
            }
        }
        builder.addSlot(RecipeIngredientRole.INPUT, TOOLS_X + 1, 1 + SLOT / 2)
                .setStandardSlotBackground()
                .addItemStack(new ItemStack(NecroItems.BONE_NEEDLE.get()))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.necromancy.sewing.needle").withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.INPUT, TOOLS_X + 1, GRID - SLOT - SLOT / 2 + 1)
                .setStandardSlotBackground()
                .addIngredients(Ingredient.of(NecroTags.SEWING_THREAD))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.necromancy.sewing.thread").withStyle(ChatFormatting.GRAY)));
        var level = Minecraft.getInstance().level;
        ItemStack result = level == null ? ItemStack.EMPTY : recipe.getResultItem(level.registryAccess());
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X + 1, (HEIGHT - 16) / 2)
                .setOutputSlotBackground()
                .addItemStack(result);
    }

    @Override
    public void draw(RecipeHolder<SewingRecipe> recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, ARROW_X, (HEIGHT - 17) / 2);
    }
}
