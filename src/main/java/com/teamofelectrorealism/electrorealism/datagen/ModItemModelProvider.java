package com.teamofelectrorealism.electrorealism.datagen;

import com.teamofelectrorealism.electrorealism.block.ModBlocks;
import net.minecraft.world.level.block.Block;
import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput packOutput, ExistingFileHelper existingFileHelper) {
        super(packOutput, ElectroRealism.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {

        basicItem(ModItems.TEST_ITEM.get());

        fenceItem(ModBlocks.STRIPPED_DARK_OAK_FENCE, mcLoc("block/stripped_dark_oak_log"));
        wallItem(ModBlocks.STONE_WALL, mcLoc("block/stone"));
        basicItem(ModItems.COPPER_SPOOL.get());
        basicItem(ModItems.SPOOL.get());
        basicItem(ModItems.RAW_BAUXITE.get());
        basicItem(ModItems.ALUMINIUM_INGOT.get());
        basicItem(ModItems.ALUMNIA_POWDER.get());
        basicItem(ModItems.ENRICHED_ALUMNIA.get());
    }

    private ItemModelBuilder handheldItem(DeferredItem<?> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/handheld")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(ElectroRealism.MODID,"item/" + item.getId().getPath()));
    }

    public void fenceItem(DeferredBlock<Block> block, ResourceLocation baseTexture) {
        this.withExistingParent(block.getId().getPath(), mcLoc("block/fence_inventory"))
                .texture("texture", baseTexture);
    }

    public void wallItem(DeferredBlock<Block> block, ResourceLocation baseTexture) {
        this.withExistingParent(block.getId().getPath(), mcLoc("block/wall_inventory"))
                .texture("wall", baseTexture);
    }
}


