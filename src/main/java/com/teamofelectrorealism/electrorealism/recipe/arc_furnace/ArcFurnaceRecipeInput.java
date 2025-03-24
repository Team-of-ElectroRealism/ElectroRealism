package com.teamofelectrorealism.electrorealism.recipe.arc_furnace;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record ArcFurnaceRecipeInput(ItemStack input) implements RecipeInput {
    @Override
    public ItemStack getItem(int i) {
        return switch (i) {
            case 0 -> input;
            default -> throw new IndexOutOfBoundsException("Invalid index: " + i);
        };
    }

    @Override
    public int size() {
        return 1;
    }
}
