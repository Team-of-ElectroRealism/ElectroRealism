package com.teamofelectrorealism.electrorealism.api;

public class ElectricalAPI {
    public static int getCurrent(int voltage, int resistance) {
        if (resistance == 0) {
            throw new IllegalArgumentException("Resistance cannot be zero.");
        }
        return voltage / resistance;
    }

    public static int getVoltage(int current, int resistance) {
        return current * resistance;
    }

    public static int getResistance(int voltage, int current) {
        if (current == 0) {
            throw new IllegalArgumentException("Current cannot be zero.");
        }
        return voltage / current;
    }

    public static int getPower(int voltage, int current) {
        return voltage * current;
    }

    public static int getChargeIncreaseMah(int voltage, int resistance, double timeInSeconds) {
        if (voltage <= 0) {
            throw new IllegalArgumentException("Voltage must be positive.");
        }
        if (resistance <= 0) {
            throw new IllegalArgumentException("Resistance must be positive.");
        }
        if (timeInSeconds <= 0) {
            throw new IllegalArgumentException("Time must be positive.");
        }

        double current = (double) voltage / resistance; // Use double for precision

        // Calculate charge in mAh (mAh = (I * t) * 3600 / 1000)
        double chargeIncrease = (current * timeInSeconds * 3600) / 1000;

        return (int) chargeIncrease;
    }

    public static boolean isOverloaded(int current, int maxCurrent) {
        return current > maxCurrent;
    }
}
