package com.teamofpowersim.powersim.utils;

import com.teamofpowersim.powersim.PowerSim;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> BAUXITE_ORES = createTag("bauxite_ores");

        private static TagKey<Block> createTag(String name) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(PowerSim.MODID, name));
        }
    }
    public static class Items {
        public static final TagKey<Item> BAUXITE_ORES = createTag("bauxite_ores");

        private static TagKey<Item> createTag(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(PowerSim.MODID, name));
        }
    }
}
