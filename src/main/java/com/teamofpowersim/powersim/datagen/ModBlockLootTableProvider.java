package com.teamofpowersim.powersim.datagen;

import com.teamofpowersim.powersim.block.ModBlocks;
import com.teamofpowersim.powersim.item.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
    protected ModBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        this.dropSelf(ModBlocks.PROGRAMMER_BLOCK.get());
        this.dropSelf(ModBlocks.ELECTRIC_CRUSHER.get());
        this.dropSelf(ModBlocks.ARC_FURNACE.get());
        this.dropSelf(ModBlocks.COMBUSTION_GENERATOR.get());
        this.dropSelf(ModBlocks.VOLTAGE_SOURCE.get());
        this.dropSelf(ModBlocks.SOLAR_PANEL.get());
        this.dropSelf(ModBlocks.COMBUSTION_GENERATOR.get());
        this.dropSelf(ModBlocks.WATER_WHEEL.get());
        this.dropSelf(ModBlocks.POWER_EXPORTER.get());
        this.dropSelf(ModBlocks.POWER_IMPORTER.get());
        this.dropSelf(ModBlocks.ELECTRIC_GENERATOR.get());
        this.dropSelf(ModBlocks.REFINERY.get());

        this.dropSelf(ModBlocks.STRIPPED_DARK_OAK_FENCE.get());
        this.dropSelf(ModBlocks.STONE_WALL.get());
        this.dropSelf(ModBlocks.COPPER_WIRE.get());
        this.dropSelf(ModBlocks.MOUNTING_PLATE.get());

        this.dropSelf(ModBlocks.SMALL_CONNECTOR.get());
        this.dropSelf(ModBlocks.LARGE_CONNECTOR.get());
        this.dropSelf(ModBlocks.DUO_CONNECTOR.get());

        this.dropSelf(ModBlocks.ALUMINIUM_BLOCK.get());

        this.add(ModBlocks.BAUXITE_ORE.get(),
                block -> createOreDrop(ModBlocks.BAUXITE_ORE.get(), ModItems.RAW_BAUXITE.get()));
        this.add(ModBlocks.DEEPSLATE_BAUXITE_ORE.get(),
                block -> createOreDrop(ModBlocks.DEEPSLATE_BAUXITE_ORE.get(), ModItems.RAW_BAUXITE.get()));


        this.dropSelf(ModBlocks.RUBBER_LOG.get());
        this.dropSelf(ModBlocks.RUBBER_WOOD.get());
        this.dropSelf(ModBlocks.STRIPPED_RUBBER_LOG.get());
        this.dropSelf(ModBlocks.STRIPPED_RUBBER_WOOD.get());
        this.dropSelf(ModBlocks.RUBBER_PLANKS.get());
        this.dropSelf(ModBlocks.RUBBER_SAPLING.get());
        this.add(ModBlocks.RUBBER_LEAVES.get(),
                block -> createLeavesDrops(block, ModBlocks.RUBBER_SAPLING.get(), NORMAL_LEAVES_SAPLING_CHANCES));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
