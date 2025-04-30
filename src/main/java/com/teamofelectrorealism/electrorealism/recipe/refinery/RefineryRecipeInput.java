package com.teamofelectrorealism.electrorealism.recipe.refinery;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record RefineryRecipeInput(ItemStack input) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> input;
            default -> throw new IndexOutOfBoundsException("Invalid index: " + index);
        };
    }

    @Override
    public int size() {
        return 1;
    }
}
