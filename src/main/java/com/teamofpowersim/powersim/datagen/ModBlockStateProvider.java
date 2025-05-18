package com.teamofpowersim.powersim.datagen;

import com.teamofpowersim.powersim.PowerSim;
import com.teamofpowersim.powersim.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.WallBlock;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput packOutput, ExistingFileHelper existingFileHelper) {
        super(packOutput, PowerSim.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        blockWithItem(ModBlocks.VOLTAGE_SOURCE);
        blockWithItem(ModBlocks.ALUMINIUM_BLOCK);
        blockWithItem(ModBlocks.BAUXITE_ORE);
        blockWithItem(ModBlocks.DEEPSLATE_BAUXITE_ORE);
        blockWithItem(ModBlocks.RUBBER_PLANKS);

        fenceBlock(((FenceBlock) ModBlocks.STRIPPED_DARK_OAK_FENCE.get()), mcLoc("block/stripped_dark_oak_log"));
        wallBlock(((WallBlock) ModBlocks.STONE_WALL.get()), mcLoc("block/stone"));

        logBlock((RotatedPillarBlock) ModBlocks.RUBBER_LOG.get());
        axisBlock((RotatedPillarBlock) ModBlocks.RUBBER_WOOD.get(), blockTexture(ModBlocks.RUBBER_LOG.get()), blockTexture(ModBlocks.RUBBER_LOG.get()));
        logBlock((RotatedPillarBlock) ModBlocks.STRIPPED_RUBBER_LOG.get());
        axisBlock((RotatedPillarBlock) ModBlocks.STRIPPED_RUBBER_WOOD.get(), blockTexture(ModBlocks.STRIPPED_RUBBER_LOG.get()), blockTexture(ModBlocks.STRIPPED_RUBBER_LOG.get()));

        blockItem(ModBlocks.RUBBER_LOG);
        blockItem(ModBlocks.RUBBER_WOOD);
        blockItem(ModBlocks.STRIPPED_RUBBER_LOG);
        blockItem(ModBlocks.STRIPPED_RUBBER_WOOD);

        leavesBlock(ModBlocks.RUBBER_LEAVES);
        saplingBlock(ModBlocks.RUBBER_SAPLING);
    }

    // Helpers
    private void leavesBlock(DeferredBlock<Block> deferredBlock) {
        simpleBlockWithItem(deferredBlock.get(),
                models().singleTexture(BuiltInRegistries.BLOCK.getKey(deferredBlock.get()).getPath(), ResourceLocation.parse("minecraft:block/leaves"),
                        "all", blockTexture(deferredBlock.get())).renderType("cutout"));
    }

    private void saplingBlock(DeferredBlock<Block> deferredBlock) {
        simpleBlock(deferredBlock.get(), models().cross(BuiltInRegistries.BLOCK.getKey(deferredBlock.get()).getPath(), blockTexture(deferredBlock.get())).renderType("cutout"));
    }

    private void blockWithItem(DeferredBlock<Block> deferredBlock) {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }

    private void blockItem(DeferredBlock<Block> deferredBlock) {
        simpleBlockItem(deferredBlock.get(), new ModelFile.UncheckedModelFile("powersim:block/" + deferredBlock.getId().getPath()));
    }
}
