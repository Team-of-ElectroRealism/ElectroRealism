package com.teamofpowersim.powersim.datagen;

import com.teamofpowersim.powersim.PowerSim;
import com.teamofpowersim.powersim.block.ModBlocks;
import net.minecraft.world.level.block.Block;
import com.teamofpowersim.powersim.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput packOutput, ExistingFileHelper existingFileHelper) {
        super(packOutput, PowerSim.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {

        basicItem(ModItems.TEST_ITEM.get());
        basicItem(ModItems.MULTIMETER.get());

        fenceItem(ModBlocks.STRIPPED_DARK_OAK_FENCE, mcLoc("block/stripped_dark_oak_log"));
        wallItem(ModBlocks.STONE_WALL, mcLoc("block/stone"));
        basicItem(ModItems.COPPER_WIRE_SPOOL.get());
        basicItem(ModItems.ALUMINIUM_WIRE_SPOOL.get());
        basicItem(ModItems.SPOOL.get());
        basicItem(ModItems.RAW_BAUXITE.get());
        basicItem(ModItems.ALUMINIUM_INGOT.get());
        basicItem(ModItems.ALUMINA_POWDER.get());
        basicItem(ModItems.ENRICHED_ALUMINA.get());

        saplingItem(ModBlocks.RUBBER_SAPLING);
    }

    private ItemModelBuilder handheldItem(DeferredItem<?> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/handheld")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(PowerSim.MODID,"item/" + item.getId().getPath()));
    }

    private ItemModelBuilder saplingItem(DeferredBlock<Block> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/generated")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(PowerSim.MODID,"block/" + item.getId().getPath()));
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


