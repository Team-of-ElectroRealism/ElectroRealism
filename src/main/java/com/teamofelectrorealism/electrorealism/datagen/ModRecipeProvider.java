package com.teamofelectrorealism.electrorealism.datagen;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.data.recipes.ShapedRecipeBuilder.shaped;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        shaped(RecipeCategory.MISC, ModBlocks.PROGRAMMER_BLOCK)
                .pattern("OOO")
                .pattern("OXO")
                .pattern("OOO")
                .define('X', Items.DIRT)
                .define('O', Items.IRON_NUGGET)
                .unlockedBy(getHasName(ModBlocks.PROGRAMMER_BLOCK.get()), has(ModBlocks.PROGRAMMER_BLOCK.get()))
                .save(recipeOutput);
        shaped(RecipeCategory.MISC, ModBlocks.ARC_FURNACE)
                .pattern("CGC")
                .pattern("BGB")
                .pattern("BBB")
                .define('C', Items.COPPER_INGOT)
                .define('G', Items.COAL)
                .define('B', Items.BRICK)
                .unlockedBy(getHasName(ModBlocks.ARC_FURNACE.get()), has(ModBlocks.ARC_FURNACE.get()))
                .save(recipeOutput);
    }
}
