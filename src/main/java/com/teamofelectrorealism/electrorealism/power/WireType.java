package com.teamofelectrorealism.electrorealism.power;

public enum WireType {
    COPPER(0, 17.54e-9, 100.0, 255, 255, 255);

    //should add CAPACITY, INDUCTANCE, THICKNESS, not MAX_CURRENT maybe?
    private final int ID, COLOR_RED, COLOR_GREEN, COLOR_BLUE;
    private final double RESISTIVITY, MAX_CURRENT; // RESISTIVITY in Ω⋅m

    WireType(int id, double resistivity, double maxCurrent, int colorRed, int colorGreen, int colorBlue) {
        this.ID = id;
        this.RESISTIVITY = resistivity;
        this.MAX_CURRENT = maxCurrent;
        this.COLOR_RED = colorRed;
        this.COLOR_GREEN = colorGreen;
        this.COLOR_BLUE = colorBlue;
    }

    public static WireType fromIndex(int index) {
        return switch (index) {
            case 0 -> COPPER;
            default -> null;
        };
    }

    public int getID() {
        return ID;
    }

    public double getResistance() {
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
}
