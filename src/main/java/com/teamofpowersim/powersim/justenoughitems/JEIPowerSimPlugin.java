package com.teamofpowersim.powersim.justenoughitems;

import com.teamofpowersim.powersim.PowerSim;
import com.teamofpowersim.powersim.justenoughitems.categories.ArcFurnaceRecipeCategory;
import com.teamofpowersim.powersim.justenoughitems.categories.ElectricCrusherRecipeCategory;
import com.teamofpowersim.powersim.justenoughitems.categories.RefineryRecipeCategory;
import com.teamofpowersim.powersim.recipe.ModRecipes;
import com.teamofpowersim.powersim.recipe.arc_furnace.ArcFurnaceRecipe;
import com.teamofpowersim.powersim.recipe.crusher.ElectricCrusherRecipe;
import com.teamofpowersim.powersim.recipe.refinery.RefineryRecipe;
import com.teamofpowersim.powersim.screen.arc_furnace.ArcFurnaceScreen;
import com.teamofpowersim.powersim.screen.crusher.ElectricCrusherScreen;
import com.teamofpowersim.powersim.screen.refinery.RefineryScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;

@JeiPlugin
public class JEIPowerSimPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(PowerSim.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new ElectricCrusherRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new RefineryRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new ArcFurnaceRecipeCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();

        List<ElectricCrusherRecipe> electricCrusherRecipes = recipeManager.getAllRecipesFor(ModRecipes.ELECTRIC_CRUSHER_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();

        List<RefineryRecipe> refineryRecipes = recipeManager.getAllRecipesFor(ModRecipes.REFINERY_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();

        List<ArcFurnaceRecipe> arcFurnaceRecipes = recipeManager.getAllRecipesFor(ModRecipes.ARC_FURNACE_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();

        registration.addRecipes(ElectricCrusherRecipeCategory.ELECTRIC_CRUSHER_RECIPE_RECIPE_TYPE, electricCrusherRecipes);
        registration.addRecipes(RefineryRecipeCategory.REFINERY_RECIPE_RECIPE_TYPE, refineryRecipes);
        registration.addRecipes(ArcFurnaceRecipeCategory.ARC_FURNACE_RECIPE_RECIPE_TYPE, arcFurnaceRecipes);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(ElectricCrusherScreen.class, 79, 34, 24, 16, ElectricCrusherRecipeCategory.ELECTRIC_CRUSHER_RECIPE_RECIPE_TYPE);
        registration.addRecipeClickArea(RefineryScreen.class, 79, 34, 24, 16, RefineryRecipeCategory.REFINERY_RECIPE_RECIPE_TYPE);
        registration.addRecipeClickArea(ArcFurnaceScreen.class, 79, 34, 24, 16, ArcFurnaceRecipeCategory.ARC_FURNACE_RECIPE_RECIPE_TYPE);
    }
}
