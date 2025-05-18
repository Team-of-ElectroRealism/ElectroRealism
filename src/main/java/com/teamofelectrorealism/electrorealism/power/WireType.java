package com.teamofelectrorealism.electrorealism.power;

import com.teamofelectrorealism.electrorealism.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public enum WireType {
    COPPER(0, 17.54e-9, 100.0, 200, 116, 86, ModItems.COPPER_WIRE_SPOOL.toStack()),
    ALUMINUM(1, 27.7e-9, 80.0, 192, 192, 192, ModItems.ALUMINIUM_WIRE_SPOOL.toStack());

    //should add CAPACITY, INDUCTANCE, THICKNESS, not MAX_CURRENT maybe?
    private final int ID, COLOR_RED, COLOR_GREEN, COLOR_BLUE;
    private final double RESISTIVITY, MAX_CURRENT; // RESISTIVITY in Ω⋅m
    private final ItemStack SOURCE_DROP;

    WireType(int id, double resistivity, double maxCurrent, int colorRed, int colorGreen, int colorBlue, ItemStack source) {
        this.ID = id;
        this.RESISTIVITY = resistivity;
        this.MAX_CURRENT = maxCurrent;
        this.COLOR_RED = colorRed;
        this.COLOR_GREEN = colorGreen;
        this.COLOR_BLUE = colorBlue;
        this.SOURCE_DROP = source;
    }

    public static WireType fromIndex(int index) {
        return switch (index) {
            case 0 -> COPPER;
            case 1 -> ALUMINUM;
            default -> null;
        };
    }

    public int getID() {
        return ID;
    }

    public double getResistivity() {
        return RESISTIVITY;
    }

    public double getMaxCurrent() {
        return MAX_CURRENT;
    }

    public int getColorRed() {
        return COLOR_RED;
    }

    public int getColorGreen() {
        return COLOR_GREEN;
    }

    public int getColorBlue() {
        return COLOR_BLUE;
    }

    public static WireType of(Item item) {
        if (item == ModItems.COPPER_WIRE_SPOOL.get()) {
            return WireType.COPPER;
        }
        if (item == ModItems.ALUMINIUM_WIRE_SPOOL.get()) {
            return WireType.ALUMINUM;
        }
        return WireType.COPPER;
    }

    public ItemStack getSourceDrop() {
        return SOURCE_DROP.copy();
    }
}
