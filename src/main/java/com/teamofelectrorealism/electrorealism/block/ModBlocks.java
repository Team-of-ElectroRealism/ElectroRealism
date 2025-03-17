package com.teamofelectrorealism.electrorealism.block;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.item.ModItems;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ElectroRealism.MODID);

    // Blocks under here

    public static final DeferredBlock<Block> PROGRAMMER_BLOCK = registerBlock("programmer_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .instabreak()
                    .mapColor(MapColor.COLOR_MAGENTA)));

    public static final DeferredBlock<CopperWireBlock> COPPER_WIRE = BLOCKS.register("copper_wire",
            () -> new CopperWireBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .noCollission()
                    .instabreak()
                    .strength(0.2F)
                    .sound(SoundType.METAL)
            ));



    // Stop Blocks

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    private static  <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }


}
