package com.teamofpowersim.powersim.datagen;

import com.teamofpowersim.powersim.PowerSim;
import com.teamofpowersim.powersim.block.ModBlocks;
import com.teamofpowersim.powersim.utils.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagsProvider.TagLookup<Block>> tagLookupCompletableFuture, ExistingFileHelper existingFileHelper) {
        super(packOutput, lookupProvider, tagLookupCompletableFuture, PowerSim.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(ItemTags.LOGS_THAT_BURN)
                .add(ModBlocks.RUBBER_LOG.get().asItem())
                .add(ModBlocks.RUBBER_WOOD.get().asItem())
                .add(ModBlocks.STRIPPED_RUBBER_LOG.get().asItem())
                .add(ModBlocks.STRIPPED_RUBBER_WOOD.get().asItem());

        this.tag(ItemTags.PLANKS)
                .add(ModBlocks.RUBBER_PLANKS.get().asItem());

        this.tag(ModTags.Items.BAUXITE_ORES)
                .add(ModBlocks.DEEPSLATE_BAUXITE_ORE.get().asItem())
                .add(ModBlocks.BAUXITE_ORE.get().asItem());
    }
}
