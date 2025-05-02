package com.teamofelectrorealism.electrorealism.datagen;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.block.ModBlocks;
import com.teamofelectrorealism.electrorealism.utils.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(packOutput, lookupProvider, ElectroRealism.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.BAUXITE_ORE.get())
                .add(ModBlocks.DEEPSLATE_BAUXITE_ORE.get())
                .add(ModBlocks.ALUMINIUM_BLOCK.get());

        this.tag(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.BAUXITE_ORE.get())
                .add(ModBlocks.DEEPSLATE_BAUXITE_ORE.get());

        this.tag(BlockTags.WOODEN_FENCES)
                .add(ModBlocks.STRIPPED_DARK_OAK_FENCE.get());

        this.tag(BlockTags.WALLS)
                .add(ModBlocks.STONE_WALL.get());

        this.tag(BlockTags.LOGS_THAT_BURN)
                .add(ModBlocks.RUBBER_LOG.get())
                .add(ModBlocks.RUBBER_WOOD.get())
                .add(ModBlocks.STRIPPED_RUBBER_LOG.get())
                .add(ModBlocks.STRIPPED_RUBBER_WOOD.get());

        this.tag(ModTags.Blocks.BAUXITE_ORES)
                .add(ModBlocks.BAUXITE_ORE.get())
                .add(ModBlocks.DEEPSLATE_BAUXITE_ORE.get());
    }
}
