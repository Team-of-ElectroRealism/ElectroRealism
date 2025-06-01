package com.teamofpowersim.powersim.item;

import com.teamofpowersim.powersim.PowerSim;
import com.teamofpowersim.powersim.block.ModBlocks;
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
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PowerSim.MODID);

    public static final Supplier<CreativeModeTab> MOD_ITEMS_TAB = CREATIVE_MODE_TAB.register("mod_items_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.COPPER_WIRE_SPOOL.get()))
                    .title(Component.translatable("creativetab.powersim.mod_items"))
                    .displayItems((itemDisplayParameters, output) -> {

                        // Items to be added to the tab
                        output.accept(ModItems.TEST_ITEM);
                        output.accept(ModItems.MULTIMETER);
                        output.accept(ModItems.SPOOL);
                        output.accept(ModItems.COPPER_WIRE_SPOOL);
                        output.accept(ModItems.ALUMINIUM_WIRE_SPOOL);
                        output.accept(ModItems.RAW_BAUXITE);
                        output.accept(ModItems.ALUMINA_POWDER);
                        output.accept(ModItems.ENRICHED_ALUMINA);
                        output.accept(ModItems.ALUMINIUM_INGOT);

                    }).build());

    public static final Supplier<CreativeModeTab> MOD_BLOCKS_TAB = CREATIVE_MODE_TAB.register("mod_blocks_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.ARC_FURNACE.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(PowerSim.MODID, "mod_items_tab"))
                    .title(Component.translatable("creativetab.powersim.mod_blocks"))
                    .displayItems((itemDisplayParameters, output) -> {

                        // Blocks to be added to the tab
                        output.accept(ModBlocks.ELECTRIC_CRUSHER);
                        output.accept(ModBlocks.ARC_FURNACE);
                        output.accept(ModBlocks.SOLAR_PANEL);
                        output.accept(ModBlocks.COMBUSTION_GENERATOR);
                        output.accept(ModBlocks.WATER_WHEEL);
                        output.accept(ModBlocks.ELECTRIC_GENERATOR);
                        output.accept(ModBlocks.POWER_EXPORTER);
                        output.accept(ModBlocks.POWER_IMPORTER);
                        output.accept(ModBlocks.REFINERY);
                        output.accept(ModBlocks.ELECTRIC_LAMP);

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
