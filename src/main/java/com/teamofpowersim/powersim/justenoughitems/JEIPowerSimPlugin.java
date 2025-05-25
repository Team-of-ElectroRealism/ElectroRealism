package com.teamofpowersim.powersim.justenoughitems;

import com.teamofpowersim.powersim.PowerSim;
import com.teamofpowersim.powersim.justenoughitems.categories.ElectricCrusherRecipeCategory;
import com.teamofpowersim.powersim.recipe.ModRecipes;
import com.teamofpowersim.powersim.recipe.crusher.ElectricCrusherRecipe;
import com.teamofpowersim.powersim.screen.crusher.ElectricCrusherScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;

public class JEIPowerSimPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(PowerSim.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new ElectricCrusherRecipeCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();

        List<ElectricCrusherRecipe> electricCrusherRecipes = recipeManager.getAllRecipesFor(ModRecipes.ELECTRIC_CRUSHER_TYPE.get()).stream().map(RecipeHolder::value).toList();

        registration.addRecipes(ElectricCrusherRecipeCategory.ELECTRIC_CRUSHER_RECIPE_RECIPE_TYPE, electricCrusherRecipes);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(ElectricCrusherScreen.class, 79, 34, 24, 16, ElectricCrusherRecipeCategory.ELECTRIC_CRUSHER_RECIPE_RECIPE_TYPE);
    }
}
