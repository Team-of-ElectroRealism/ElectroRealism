package com.teamofpowersim.powersim.recipe.crusher;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record ElectricCrusherRecipeInput(ItemStack input) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> input;
            default -> throw new IndexOutOfBoundsException("Invalid index: " + index);
        };
    }

    public int size() {
        return 1;
    }
}
