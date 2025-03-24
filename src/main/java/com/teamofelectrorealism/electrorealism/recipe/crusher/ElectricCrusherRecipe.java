package com.teamofelectrorealism.electrorealism.recipe.crusher;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teamofelectrorealism.electrorealism.recipe.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.stream.IntStream;

public record ElectricCrusherRecipe(Ingredient input,ItemStack output) implements Recipe<ElectricCrusherRecipeInput> {
    @Override
    public boolean matches(ElectricCrusherRecipeInput recipeInput, Level level) {
        if (level.isClientSide()) {
            return false;
        }
        if (getIngredients().size() != recipeInput.size()) {
            return false;
        }

        return IntStream.range(0, getIngredients().size())
                .allMatch(i -> getIngredients().get(i).test(recipeInput.getItem(i)));
    }

    @Override
    public ItemStack assemble(ElectricCrusherRecipeInput electricCrusherRecipeInput, HolderLookup.Provider provider) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return output;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ELECTRIC_CRUSHER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ELECTRIC_CRUSHER_TYPE.get();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(input);
        return ingredients;
    }

    public static class Serializer implements RecipeSerializer<ElectricCrusherRecipe> {

        public static final MapCodec<ElectricCrusherRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ElectricCrusherRecipe::input),
                        ItemStack.CODEC.fieldOf("result").forGetter(ElectricCrusherRecipe::output))
                .apply(instance, ElectricCrusherRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ElectricCrusherRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, ElectricCrusherRecipe::input,
                ItemStack.STREAM_CODEC, ElectricCrusherRecipe::output, ElectricCrusherRecipe::new);

        @Override
        public MapCodec<ElectricCrusherRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ElectricCrusherRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
