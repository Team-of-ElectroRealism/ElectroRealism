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
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.COPPER_WIRE_SPOOL.get()))
                    .title(Component.translatable("creativetab.electrorealism.electrorealism_items"))
                    .displayItems((itemDisplayParameters, output) -> {

                        // Items to be added to the tab
                        output.accept(ModItems.TEST_ITEM);
                        output.accept(ModItems.SPOOL);
                        output.accept(ModItems.COPPER_WIRE_SPOOL);
                        output.accept(ModItems.ALUMINUM_WIRE_SPOOL);
                        output.accept(ModItems.RAW_BAUXITE);
                        output.accept(ModItems.ALUMNIA_POWDER);
                        output.accept(ModItems.ENRICHED_ALUMNIA);
                        output.accept(ModItems.ALUMINIUM_INGOT);

                    }).build());

    public static final Supplier<CreativeModeTab> ELECTROREALISM_BLOCKS_TAB = CREATIVE_MODE_TAB.register("electrorealism_blocks_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.ARC_FURNACE.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(ElectroRealism.MODID, "electrorealism_items_tab"))
                    .title(Component.translatable("creativetab.electrorealism.electrorealism_blocks"))
                    .displayItems((itemDisplayParameters, output) -> {

                        // Blocks to be added to the tab
                        output.accept(ModBlocks.PROGRAMMER_BLOCK);
                        output.accept(ModBlocks.ELECTRIC_CRUSHER);
                        output.accept(ModBlocks.ARC_FURNACE);
                        output.accept(ModBlocks.VOLTAGE_SOURCE);
                        output.accept(ModBlocks.SOLAR_PANEL);
                        output.accept(ModBlocks.COMBUSTION_GENERATOR);
                        output.accept(ModBlocks.WATER_WHEEL);
                        output.accept(ModBlocks.ELECTRIC_GENERATOR);
                        output.accept(ModBlocks.POWER_EXPORTER);
                        output.accept(ModBlocks.POWER_IMPORTER);
                        output.accept(ModBlocks.REFINERY);

                        output.accept(ModBlocks.SMALL_CONNECTOR);
                        output.accept(ModBlocks.LARGE_CONNECTOR);
                        output.accept(ModBlocks.DUO_CONNECTOR);

                        output.accept(ModBlocks.COPPER_WIRE);
                        output.accept(ModBlocks.STRIPPED_DARK_OAK_FENCE);
                        output.accept(ModBlocks.STONE_WALL);

                        output.accept(ModBlocks.BAUXITE_ORE);
                        output.accept(ModBlocks.DEEPSLATE_BAUXITE_ORE);

                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
