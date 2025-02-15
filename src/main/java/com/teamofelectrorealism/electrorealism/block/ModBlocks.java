package com.teamofelectrorealism.electrorealism.block;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.block.crusher.ElectricCrusherBlock;
import com.teamofelectrorealism.electrorealism.block.generator.solarpanel.SolarPanelBlock;
import com.teamofelectrorealism.electrorealism.block.generator.test.VoltageSourceBlock;
import com.teamofelectrorealism.electrorealism.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ElectroRealism.MODID);

    // Blocks under here

    public static final DeferredBlock<Block> PROGRAMMER_BLOCK = registerBlock("programmer_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .instabreak()
                    .mapColor(MapColor.COLOR_MAGENTA)));

    public static final DeferredBlock<Block> ELECTRIC_CRUSHER = registerBlock("electric_crusher",
            () -> new ElectricCrusherBlock(BlockBehaviour.Properties.of()
                    .instabreak()
                    .mapColor(MapColor.COLOR_GRAY)
                    .noOcclusion()));

    public static final DeferredBlock<Block> VOLTAGE_SOURCE = registerBlock("voltage_source",
            () -> new VoltageSourceBlock(BlockBehaviour.Properties.of()
                    .instabreak()
                    .mapColor(MapColor.COLOR_GRAY)
                    .noOcclusion()));

    public static final DeferredBlock<Block> SOLAR_PANEL = registerBlock("solar_panel",
            () -> new SolarPanelBlock(BlockBehaviour.Properties.of()
                    .instabreak()
                    .mapColor(MapColor.COLOR_GRAY)
                    .noOcclusion()));

    // Stop Blocks

    private static  <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
