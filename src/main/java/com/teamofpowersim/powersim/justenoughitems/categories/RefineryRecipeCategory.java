package com.teamofpowersim.powersim.justenoughitems.categories;

import com.teamofpowersim.powersim.PowerSim;
import com.teamofpowersim.powersim.block.ModBlocks;
import com.teamofpowersim.powersim.recipe.refinery.RefineryRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class RefineryRecipeCategory implements IRecipeCategory<RefineryRecipe> {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(PowerSim.MODID, "refining");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(PowerSim.MODID, "textures/gui/crusher/electric_crusher_gui_jei.png");

    public static final RecipeType<RefineryRecipe> REFINERY_RECIPE_RECIPE_TYPE = new RecipeType<>(UID, RefineryRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public RefineryRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 85);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.REFINERY.get()));
    }

    @Override
    public RecipeType<RefineryRecipe> getRecipeType() {
        return REFINERY_RECIPE_RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.powersim.refinery");
    }

    @Override
    public @Nullable IDrawable getBackground() {
        return background;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RefineryRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(56, 35).addIngredients(recipe.input());
        builder.addOutputSlot(116, 35).addItemStack(recipe.output());
    }
}
