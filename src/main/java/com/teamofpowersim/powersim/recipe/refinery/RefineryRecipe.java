package com.teamofpowersim.powersim.recipe.refinery;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teamofpowersim.powersim.recipe.ModRecipes;
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

public record RefineryRecipe(Ingredient input, ItemStack output) implements Recipe<RefineryRecipeInput> {
    @Override
    public boolean matches(RefineryRecipeInput recipeInput, Level level) {
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
    public ItemStack assemble(RefineryRecipeInput recipeInput, HolderLookup.Provider provider) {
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
        return ModRecipes.REFINERY_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.REFINERY_TYPE.get();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(input);
        return ingredients;
    }

    public static class Serializer implements RecipeSerializer<RefineryRecipe> {

        public static final MapCodec<RefineryRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(RefineryRecipe::input),
                        ItemStack.CODEC.fieldOf("result").forGetter(RefineryRecipe::output))
                .apply(instance, RefineryRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, RefineryRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, RefineryRecipe::input,
                ItemStack.STREAM_CODEC, RefineryRecipe::output, RefineryRecipe::new);

        @Override
        public MapCodec<RefineryRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RefineryRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
