package com.teamofelectrorealism.electrorealism.utils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FuelValues {
    private static final Map<Item, Integer> FUEL_VALUES = new HashMap<>();

    static {
        addFuel(Items.LAVA_BUCKET, 20000);
        addFuel(Items.COAL_BLOCK, 16000);
        addFuel(Items.BLAZE_ROD, 2400);
        addFuel(Items.COAL, 1600);
        addFuel(Items.CHARCOAL, 1600);
        addFuel(ItemTags.LOGS, 300);
        addFuel(ItemTags.BAMBOO_BLOCKS, 300);
        addFuel(ItemTags.PLANKS, 300);
        addFuel(Items.BAMBOO_MOSAIC, 300);
        addFuel(ItemTags.WOODEN_STAIRS, 300);
        addFuel(Items.BAMBOO_MOSAIC_STAIRS, 300);
        addFuel(ItemTags.WOODEN_SLABS, 150);
        addFuel(Items.BAMBOO_MOSAIC_SLAB, 150);
        addFuel(ItemTags.WOODEN_TRAPDOORS, 300);
        addFuel(ItemTags.WOODEN_PRESSURE_PLATES, 300);
        addFuel(ItemTags.WOODEN_FENCES, 300);
        addFuel(ItemTags.FENCE_GATES, 300);
        addFuel(Items.NOTE_BLOCK, 300);
        addFuel(Items.BOOKSHELF, 300);
        addFuel(Items.CHISELED_BOOKSHELF, 300);
        addFuel(Items.LECTERN, 300);
        addFuel(Items.JUKEBOX, 300);
        addFuel(Items.CHEST, 300);
        addFuel(Items.TRAPPED_CHEST, 300);
        addFuel(Items.CRAFTING_TABLE, 300);
        addFuel(Items.DAYLIGHT_DETECTOR, 300);
        addFuel(ItemTags.BANNERS, 300);
        addFuel(Items.BOW, 300);
        addFuel(Items.FISHING_ROD, 300);
        addFuel(Items.LADDER, 300);
        addFuel(ItemTags.SIGNS, 200);
        addFuel(ItemTags.HANGING_SIGNS, 800);
        addFuel(Items.WOODEN_SHOVEL, 200);
        addFuel(Items.WOODEN_SWORD, 200);
        addFuel(Items.WOODEN_HOE, 200);
        addFuel(Items.WOODEN_AXE, 200);
        addFuel(Items.WOODEN_PICKAXE, 200);
        addFuel(ItemTags.WOODEN_DOORS, 200);
        addFuel(ItemTags.BOATS, 1200);
        addFuel(ItemTags.WOOL, 100);
        addFuel(ItemTags.WOODEN_BUTTONS, 100);
        addFuel(Items.STICK, 100);
        addFuel(ItemTags.SAPLINGS, 100);
        addFuel(Items.BOWL, 100);
        addFuel(ItemTags.WOOL_CARPETS, 67);
        addFuel(Items.DRIED_KELP_BLOCK, 4001);
        addFuel(Items.CROSSBOW, 300);
        addFuel(Items.BAMBOO, 50);
        addFuel(Items.DEAD_BUSH, 100);
        addFuel(Items.SCAFFOLDING, 50);
        addFuel(Items.LOOM, 300);
        addFuel(Items.BARREL, 300);
        addFuel(Items.CARTOGRAPHY_TABLE, 300);
        addFuel(Items.FLETCHING_TABLE, 300);
        addFuel(Items.SMITHING_TABLE, 300);
        addFuel(Items.COMPOSTER, 300);
        addFuel(Items.AZALEA, 100);
        addFuel(Items.FLOWERING_AZALEA, 100);
        addFuel(Items.MANGROVE_ROOTS, 300);
    }

    private static void addFuel(Item item, int burnTime) {
        FUEL_VALUES.put(item, burnTime);
    }

    private static void addFuel(TagKey<Item> itemTag, int burnTime) {
        Set<Item> itemsInTag = BuiltInRegistries.ITEM.getOrCreateTag(itemTag).stream()
                .map(holder -> holder.value())
                .collect(java.util.stream.Collectors.toSet());

        for (Item item : itemsInTag) {
            FUEL_VALUES.put(item, burnTime);
        }
    }

    public static int getFuelValue(ItemStack fuelStack) {
        if (fuelStack.isEmpty()) {
            return 0;
        }
        Item fuelItem = fuelStack.getItem();
        return FUEL_VALUES.getOrDefault(fuelItem, 0);
    }
}
