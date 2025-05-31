package com.teamofpowersim.powersim.item;

import com.teamofpowersim.powersim.PowerSim;
import com.teamofpowersim.powersim.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PowerSim.MODID);

    public static final DeferredItem<Item> COPPER_WIRE_ITEM = ModItems.ITEMS.register("copper_wire",
            () -> new BlockItem(ModBlocks.COPPER_WIRE.get(), new Item.Properties()));


    // Items under here

    public static final DeferredItem<Item> TEST_ITEM = ITEMS.register("test_item",
            () -> new TestItem(new Item.Properties()));
    public static final DeferredItem<Item> MULTIMETER = ITEMS.register("multimeter",
            () -> new Multimeter(new Item.Properties()));
    public static final DeferredItem<Item> ALUMINIUM_WIRE_SPOOL = ITEMS.register("aluminium_wire_spool",
            () -> new WireSpool(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_WIRE_SPOOL = ITEMS.register("copper_wire_spool",
            () -> new WireSpool(new Item.Properties()));
    public static final DeferredItem<Item> SPOOL = ITEMS.register("spool",
            () -> new WireSpool(new Item.Properties()));
    public static final DeferredItem<Item> RAW_BAUXITE = ITEMS.register("raw_bauxite",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ALUMINIUM_INGOT = ITEMS.register("aluminium_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ALUMINA_POWDER = ITEMS.register("alumina_powder",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ENRICHED_ALUMINA = ITEMS.register("enriched_alumina",
            () -> new Item(new Item.Properties()));

    // Stop items

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
