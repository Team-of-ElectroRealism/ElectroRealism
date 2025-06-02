package com.teamofpowersim.powersim;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = PowerSim.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("Specific operational parameters for the Small Connector")
                .push("small_connector_specifics");

        SMALL_CONNECTOR_MAX_WIRE_LENGTH = BUILDER
                .comment("Maximum wire length for Small Connectors.")
                .defineInRange("small_connector_max_wire_length", 16, 1, 64);

        SMALL_CONNECTOR_CONNECTION_POINT_COUNT = BUILDER
                .comment("Number of connection points for Small Connectors.")
                .defineInRange("small_connector_connection_point_count", 4, 1, 8);

        BUILDER.pop();
    }

    static {
        BUILDER.comment("Specific operational parameters for the Large Connector")
                .push("large_connector_specifics");

        LARGE_CONNECTOR_MAX_WIRE_LENGTH = BUILDER
                .comment("Maximum wire length for Large Connectors.")
                .defineInRange("large_connector_max_wire_length", 32, 1, 128);

        LARGE_CONNECTOR_CONNECTION_POINT_COUNT = BUILDER
                .comment("Number of connection points for Large Connectors.")
                .defineInRange("large_connector_connection_point_count", 6, 1, 12);

        BUILDER.pop();
    }

    static {
        BUILDER.comment("Specific operational parameters for the Duo Connector")
                .push("duo_connector_specifics");

        DUO_CONNECTOR_MAX_WIRE_LENGTH = BUILDER
                .comment("Maximum wire length for Duo Connectors.")
                .defineInRange("duo_connector_max_wire_length", 16, 1, 64);

        BUILDER.pop();
    }

    static {
        BUILDER.comment("Specific operational parameters for the Electric Crusher")
                .push("electric_crusher_specifics");

        ELECTRIC_CRUSHER_MIN_OPERATING_VOLTAGE = BUILDER
                .comment("Minimum voltage required for the Electric Crusher to start and continue operating (Volts).")
                .defineInRange("electric_crusher_min_operating_voltage", 70.0, 1.0, 500.0);

        ELECTRIC_CRUSHER_NOMINAL_OPERATING_CURRENT = BUILDER
                .comment("Nominal operating current for the Electric Crusher under its typical designed load (Amps).")
                .defineInRange("electric_crusher_nominal_operating_current", 8.0, 0.1, 50.0);

        ELECTRIC_CRUSHER_MAX_SAFE_CURRENT = BUILDER
                .comment("Maximum current the Electric Crusher can safely handle before risking damage or shutdown (Amps).")
                .defineInRange("electric_crusher_max_safe_current", 20.0, 1.0, 100.0);

        ELECTRIC_CRUSHER_DEFAULT_CRUSHING_TIME = BUILDER
                .comment("Default time in ticks it takes for the Electric Crusher to process one item/recipe.")
                .defineInRange("electric_crusher_default_crushing_time", 100, 20, 600);

        ELECTRIC_CRUSHER_INTERNAL_RESISTANCE = BUILDER
                .comment("Default internal resistance of the Electric Crusher (Ohms).")
                .defineInRange("electric_crusher_internal_resistance", 10, 1, 100);

        BUILDER.pop();
    }

    static {
        BUILDER.comment("Specific operational parameters for the Arc Furnace")
                .push("arc_furnace_specifics");

        ARC_FURNACE_MIN_OPERATING_VOLTAGE = BUILDER
                .comment("Minimum voltage required for the Arc Furnace to start and continue operating (Volts).")
                .defineInRange("arc_furnace_min_operating_voltage", 150.0, 10.0, 800.0);

        ARC_FURNACE_NOMINAL_OPERATING_CURRENT = BUILDER
                .comment("Nominal operating current for the Arc Furnace under its typical designed load (Amps).")
                .defineInRange("arc_furnace_nominal_operating_current", 15.0, 0.1, 100.0);

        ARC_FURNACE_MAX_SAFE_CURRENT = BUILDER
                .comment("Maximum current the Arc Furnace can safely handle before risking damage or shutdown (Amps).")
                .defineInRange("arc_furnace_max_safe_current", 40.0, 1.0, 200.0);

        ARC_FURNACE_DEFAULT_SMELTING_TIME = BUILDER
                .comment("Default time in ticks it takes for the Arc Furnace to process one item/recipe.")
                .defineInRange("arc_furnace_default_smelting_time", 200, 20, 1200);

        ARC_FURNACE_INTERNAL_RESISTANCE = BUILDER
                .comment("Internal resistance of the Arc Furnace used by the block entity (Ohms).")
                .defineInRange("arc_furnace_internal_resistance", 20, 1, 200);

        BUILDER.pop();
    }

    static {
        BUILDER.comment("Specific operational parameters for the Refinery")
                .push("refinery_specifics");

        REFINERY_MIN_OPERATING_VOLTAGE = BUILDER
                .comment("Minimum voltage required for the Refinery to start and continue operating (Volts).")
                .defineInRange("refinery_min_operating_voltage", 120.0, 10.0, 700.0);

        REFINERY_NOMINAL_OPERATING_CURRENT = BUILDER
                .comment("Nominal operating current for the Refinery under its typical designed load (Amps).")
                .defineInRange("refinery_nominal_operating_current", 12.0, 0.1, 80.0);

        REFINERY_MAX_SAFE_CURRENT = BUILDER
                .comment("Maximum current the Refinery can safely handle before risking damage or shutdown (Amps).")
                .defineInRange("refinery_max_safe_current", 30.0, 1.0, 150.0);

        REFINERY_DEFAULT_PROCESSING_TIME = BUILDER
                .comment("Default time in ticks it takes for the Refinery to process one item/recipe.")
                .defineInRange("refinery_default_processing_time", 150, 20, 1000);

        REFINERY_INTERNAL_RESISTANCE = BUILDER
                .comment("Internal resistance of the Refinery used by the block entity (Ohms).")
                .defineInRange("refinery_internal_resistance", 15, 1, 150);

        BUILDER.pop();
    }

    static {
        BUILDER.comment("Specific operational parameters for the Electric Lamp")
                .push("electric_lamp_specifics");

        ELECTRIC_LAMP_MIN_OPERATING_VOLTAGE = BUILDER
                .comment("Minimum voltage required for the Electric Lamp to start and continue operating (Volts).")
                .defineInRange("electric_lamp_min_operating_voltage", 24.0, 1.0, 100.0);

        ELECTRIC_LAMP_NOMINAL_OPERATING_CURRENT = BUILDER
                .comment("Nominal operating current for the Electric Lamp under its typical designed load (Amps).")
                .defineInRange("electric_lamp_nominal_operating_current", 0.5, 0.01, 10.0); // Adjusted range for typical lamp

        ELECTRIC_LAMP_MAX_SAFE_CURRENT = BUILDER
                .comment("Maximum current the Electric Lamp can safely handle before risking damage or shutdown (Amps).")
                .defineInRange("electric_lamp_max_safe_current", 2.0, 0.1, 20.0); // Adjusted range

        ELECTRIC_LAMP_MIN_LIGHT_POWER_WATTS = BUILDER
                .comment("Minimum power (in Watts) required for the lamp to produce any light.")
                .defineInRange("electric_lamp_min_light_power_watts", 5.0, 0.1, 50.0);

        ELECTRIC_LAMP_INTERNAL_RESISTANCE = BUILDER
                .comment("Default internal resistance of the Electric Lamp (Ohms).")
                .defineInRange("electric_lamp_internal_resistance", 25, 1, 200);

        BUILDER.pop();
    }

    static {
        BUILDER.comment("Specific operational parameters for the Combustion Generator")
                .push("combustion_generator_specifics");

        COMBUSTION_GENERATOR_OUTPUT_VOLTAGE = BUILDER
                .comment("Nominal output voltage of the Combustion Generator (Volts).")
                .defineInRange("combustion_generator_output_voltage", 400.0, 10.0, 2000.0);

        BUILDER.pop();
    }

    static {
        BUILDER.comment("Specific operational parameters for the Solar Panel")
                .push("solar_panel_specifics");

        SOLAR_PANEL_OUTPUT_VOLTAGE = BUILDER
                .comment("Nominal output voltage of the Solar Panel under optimal conditions (Volts).")
                .defineInRange("solar_panel_output_voltage", 230.0, 5.0, 500.0);

        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    // Connector Settings
    private static final ModConfigSpec.IntValue SMALL_CONNECTOR_MAX_WIRE_LENGTH;
    private static final ModConfigSpec.IntValue LARGE_CONNECTOR_MAX_WIRE_LENGTH;
    private static final ModConfigSpec.IntValue DUO_CONNECTOR_MAX_WIRE_LENGTH;
    private static final ModConfigSpec.IntValue SMALL_CONNECTOR_CONNECTION_POINT_COUNT;
    private static final ModConfigSpec.IntValue LARGE_CONNECTOR_CONNECTION_POINT_COUNT;

    // Electric Crusher Specific Settings
    private static final ModConfigSpec.DoubleValue ELECTRIC_CRUSHER_MIN_OPERATING_VOLTAGE;
    private static final ModConfigSpec.DoubleValue ELECTRIC_CRUSHER_NOMINAL_OPERATING_CURRENT;
    private static final ModConfigSpec.DoubleValue ELECTRIC_CRUSHER_MAX_SAFE_CURRENT;
    private static final ModConfigSpec.IntValue ELECTRIC_CRUSHER_DEFAULT_CRUSHING_TIME;
    private static final ModConfigSpec.IntValue ELECTRIC_CRUSHER_INTERNAL_RESISTANCE;

    // Arc Furnace Specific Settings
    private static final ModConfigSpec.DoubleValue ARC_FURNACE_MIN_OPERATING_VOLTAGE;
    private static final ModConfigSpec.DoubleValue ARC_FURNACE_NOMINAL_OPERATING_CURRENT;
    private static final ModConfigSpec.DoubleValue ARC_FURNACE_MAX_SAFE_CURRENT;
    private static final ModConfigSpec.IntValue ARC_FURNACE_DEFAULT_SMELTING_TIME;
    private static final ModConfigSpec.IntValue ARC_FURNACE_INTERNAL_RESISTANCE;

    // Refinery Specific Settings
    private static final ModConfigSpec.DoubleValue REFINERY_MIN_OPERATING_VOLTAGE;
    private static final ModConfigSpec.DoubleValue REFINERY_NOMINAL_OPERATING_CURRENT;
    private static final ModConfigSpec.DoubleValue REFINERY_MAX_SAFE_CURRENT;
    private static final ModConfigSpec.IntValue REFINERY_DEFAULT_PROCESSING_TIME;
    private static final ModConfigSpec.IntValue REFINERY_INTERNAL_RESISTANCE;

    // Electric Lamp Specific Settings
    private static final ModConfigSpec.DoubleValue ELECTRIC_LAMP_MIN_OPERATING_VOLTAGE;
    private static final ModConfigSpec.DoubleValue ELECTRIC_LAMP_NOMINAL_OPERATING_CURRENT;
    private static final ModConfigSpec.DoubleValue ELECTRIC_LAMP_MAX_SAFE_CURRENT;
    private static final ModConfigSpec.DoubleValue ELECTRIC_LAMP_MIN_LIGHT_POWER_WATTS;
    private static final ModConfigSpec.IntValue ELECTRIC_LAMP_INTERNAL_RESISTANCE;

    // Combustion Generator Specific Settings
    private static final ModConfigSpec.DoubleValue COMBUSTION_GENERATOR_OUTPUT_VOLTAGE;

    // Solar Panel Specific Settings
    private static final ModConfigSpec.DoubleValue SOLAR_PANEL_OUTPUT_VOLTAGE;

    // --- Public Static Fields for Accessing Loaded Values ---
    public static int smallConnectorMaxWireLength;
    public static int largeConnectorMaxWireLength;
    public static int duoConnectorMaxWireLength;
    public static int smallConnectorConnectionPointCount;
    public static int largeConnectorConnectionPointCount;

    // Electric Crusher
    public static double electricCrusherMinOperatingVoltage;
    public static double electricCrusherNominalOperatingCurrent;
    public static double electricCrusherMaxSafeCurrent;
    public static int electricCrusherDefaultCrushingTime;
    public static int electricCrusherInternalResistance;

    // Arc Furnace
    public static double arcFurnaceMinOperatingVoltage;
    public static double arcFurnaceNominalOperatingCurrent;
    public static double arcFurnaceMaxSafeCurrent;
    public static int arcFurnaceDefaultSmeltingTime;
    public static int arcFurnaceInternalResistance;

    // Refinery
    public static double refineryMinOperatingVoltage;
    public static double refineryNominalOperatingCurrent;
    public static double refineryMaxSafeCurrent;
    public static int refineryDefaultProcessingTime;
    public static int refineryInternalResistance;

    // Electric Lamp
    public static double electricLampMinOperatingVoltage;
    public static double electricLampNominalOperatingCurrent;
    public static double electricLampMaxSafeCurrent;
    public static double electricLampMinLightPowerWatts;
    public static int electricLampInternalResistance;

    // Combustion Generator
    public static double combustionGeneratorOutputVoltage;

    // Solar Panel
    public static double solarPanelOutputVoltage;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        smallConnectorMaxWireLength = SMALL_CONNECTOR_MAX_WIRE_LENGTH.get();
        largeConnectorMaxWireLength = LARGE_CONNECTOR_MAX_WIRE_LENGTH.get();
        duoConnectorMaxWireLength = DUO_CONNECTOR_MAX_WIRE_LENGTH.get();

        smallConnectorConnectionPointCount = SMALL_CONNECTOR_CONNECTION_POINT_COUNT.get();
        largeConnectorConnectionPointCount = LARGE_CONNECTOR_CONNECTION_POINT_COUNT.get();

        // Load Electric Crusher
        electricCrusherMinOperatingVoltage = ELECTRIC_CRUSHER_MIN_OPERATING_VOLTAGE.get();
        electricCrusherNominalOperatingCurrent = ELECTRIC_CRUSHER_NOMINAL_OPERATING_CURRENT.get();
        electricCrusherMaxSafeCurrent = ELECTRIC_CRUSHER_MAX_SAFE_CURRENT.get();
        electricCrusherDefaultCrushingTime = ELECTRIC_CRUSHER_DEFAULT_CRUSHING_TIME.get();
        electricCrusherInternalResistance = ELECTRIC_CRUSHER_INTERNAL_RESISTANCE.get();

        // Load Arc Furnace
        arcFurnaceMinOperatingVoltage = ARC_FURNACE_MIN_OPERATING_VOLTAGE.get();
        arcFurnaceNominalOperatingCurrent = ARC_FURNACE_NOMINAL_OPERATING_CURRENT.get();
        arcFurnaceMaxSafeCurrent = ARC_FURNACE_MAX_SAFE_CURRENT.get();
        arcFurnaceDefaultSmeltingTime = ARC_FURNACE_DEFAULT_SMELTING_TIME.get();
        arcFurnaceInternalResistance = ARC_FURNACE_INTERNAL_RESISTANCE.get();

        // Load Refinery
        refineryMinOperatingVoltage = REFINERY_MIN_OPERATING_VOLTAGE.get();
        refineryNominalOperatingCurrent = REFINERY_NOMINAL_OPERATING_CURRENT.get();
        refineryMaxSafeCurrent = REFINERY_MAX_SAFE_CURRENT.get();
        refineryDefaultProcessingTime = REFINERY_DEFAULT_PROCESSING_TIME.get();
        refineryInternalResistance = REFINERY_INTERNAL_RESISTANCE.get();

        // Load Electric Lamp
        electricLampMinOperatingVoltage = ELECTRIC_LAMP_MIN_OPERATING_VOLTAGE.get();
        electricLampNominalOperatingCurrent = ELECTRIC_LAMP_NOMINAL_OPERATING_CURRENT.get();
        electricLampMaxSafeCurrent = ELECTRIC_LAMP_MAX_SAFE_CURRENT.get();
        electricLampMinLightPowerWatts = ELECTRIC_LAMP_MIN_LIGHT_POWER_WATTS.get();
        electricLampInternalResistance = ELECTRIC_LAMP_INTERNAL_RESISTANCE.get();

        // Load Combustion Generator
        combustionGeneratorOutputVoltage = COMBUSTION_GENERATOR_OUTPUT_VOLTAGE.get();

        // Load Solar Panel
        solarPanelOutputVoltage = SOLAR_PANEL_OUTPUT_VOLTAGE.get();
    }
}
