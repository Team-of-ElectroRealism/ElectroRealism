package com.teamofelectrorealism.electrorealism.recipe;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.recipe.crusher.ElectricCrusherRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ElectroRealism.MODID);
    public static final  DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, ElectroRealism.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ElectricCrusherRecipe>> ELECTRIC_CRUSHER_SERIALIZER =
            SERIALIZERS.register("crushing", ElectricCrusherRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<ElectricCrusherRecipe>> ELECTRIC_CRUSHER_TYPE =
            RECIPE_TYPES.register("crushing", () -> new RecipeType<ElectricCrusherRecipe>() {
                @Override
                public String toString() {
                    return "crushing";
                }
            });

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        RECIPE_TYPES.register(eventBus);
    }
}
