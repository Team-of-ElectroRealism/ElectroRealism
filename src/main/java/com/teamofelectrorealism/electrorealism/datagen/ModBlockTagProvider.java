package com.teamofelectrorealism.electrorealism.datagen;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.block.ModBlocks;
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

        tag(BlockTags.WOODEN_FENCES).add(ModBlocks.STRIPPED_DARK_OAK_FENCE.get());
        tag(BlockTags.WALLS).add(ModBlocks.STONE_WALL.get());

    }
}
