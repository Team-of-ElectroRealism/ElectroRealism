package com.teamofpowersim.powersim.justenoughitems.categories;

import com.teamofpowersim.powersim.PowerSim;
import com.teamofpowersim.powersim.block.ModBlocks;
import com.teamofpowersim.powersim.recipe.arc_furnace.ArcFurnaceRecipe;
import com.teamofpowersim.powersim.recipe.crusher.ElectricCrusherRecipe;
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

public class ArcFurnaceRecipeCategory implements IRecipeCategory<ArcFurnaceRecipe> {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(PowerSim.MODID, "smelting");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(PowerSim.MODID, "textures/gui/arc_furnace/arc_furnace_gui_jei.png");

    public static final RecipeType<ArcFurnaceRecipe> ARC_FURNACE_RECIPE_RECIPE_TYPE = new RecipeType<>(UID, ArcFurnaceRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public ArcFurnaceRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 85);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.ARC_FURNACE.get()));
    }

    @Override
    public RecipeType<ArcFurnaceRecipe> getRecipeType() {
        return ARC_FURNACE_RECIPE_RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.powersim.arc_furnace");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ArcFurnaceRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(56, 35).addIngredients(recipe.input());
        builder.addOutputSlot(116, 35).addItemStack(recipe.output());
    }
}
