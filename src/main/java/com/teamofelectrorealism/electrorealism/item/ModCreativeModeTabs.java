package com.teamofelectrorealism.electrorealism.item;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ElectroRealism.MODID);

    public static final Supplier<CreativeModeTab> ELECTROREALISM_ITEMS_TAB = CREATIVE_MODE_TAB.register("electrorealism_items_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.PROGRAMMER_BLOCK.get()))
                    .title(Component.translatable("creativetab.electrorealism.electrorealism_items"))
                    .displayItems((itemDisplayParameters, output) -> {

                        // Items to be added to the tab
                        output.accept(ModItems.TEST_ITEM);

                    }).build());

    public static final Supplier<CreativeModeTab> ELECTROREALISM_BLOCKS_TAB = CREATIVE_MODE_TAB.register("electrorealism_blocks_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.PROGRAMMER_BLOCK.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(ElectroRealism.MODID, "electrorealism_items_tab"))
                    .title(Component.translatable("creativetab.electrorealism.electrorealism_blocks"))
                    .displayItems((itemDisplayParameters, output) -> {

                        // Blocks to be added to the tab
                        output.accept(ModBlocks.PROGRAMMER_BLOCK);
                        output.accept(ModBlocks.ELECTRIC_CRUSHER);
                        output.accept(ModBlocks.VOLTAGE_SOURCE);

                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
