package com.teamofelectrorealism.electrorealism.item;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.block.ModBlocks;
import com.teamofelectrorealism.electrorealism.item.tool.DrillItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElectroRealism.MODID);

    public static final DeferredItem<Item> COPPER_WIRE_ITEM = ModItems.ITEMS.register("copper_wire",
            () -> new BlockItem(ModBlocks.COPPER_WIRE.get(), new Item.Properties()));


    // Items under here

    public static final DeferredItem<Item> TEST_ITEM = ITEMS.register("test_item",
            () -> new TestItem(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_SPOOL = ITEMS.register("copper_spool",
            () -> new WireSpool(new Item.Properties()));
    public static final DeferredItem<Item> SPOOL = ITEMS.register("spool",
            () -> new WireSpool(new Item.Properties()));
    public static final DeferredItem<Item> RAW_BAUXITE = ITEMS.register("raw_bauxite",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ALUMINIUM_INGOT = ITEMS.register("aluminium_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ALUMNIA_POWDER = ITEMS.register("alumnia_powder",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ENRICHED_ALUMNIA = ITEMS.register("enriched_alumnia",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> POWER_DRILL = ITEMS.register("power_drill",
            () -> new DrillItem(Tiers.DIAMOND,
                    new Item.Properties().attributes(PickaxeItem.createAttributes(Tiers.DIAMOND, 3, -3f))));

    // Stop items

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
