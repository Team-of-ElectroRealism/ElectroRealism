package com.teamofpowersim.powersim.block;

import com.teamofpowersim.powersim.PowerSim;
import com.teamofpowersim.powersim.block.connector.duo.DuoConnectorBlockEntity;
import com.teamofpowersim.powersim.block.connector.large.LargeConnectorBlockEntity;
import com.teamofpowersim.powersim.block.machine.consumer.refinery.RefineryBlockEntity;
import com.teamofpowersim.powersim.block.machine.provider.combustion.CombustionGeneratorBlockEntity;
import com.teamofpowersim.powersim.block.connector.small.SmallConnectorBlockEntity;
import com.teamofpowersim.powersim.block.machine.provider.electric_generator.ElectricGeneratorBlockEntity;
import com.teamofpowersim.powersim.block.machine.provider.power_importer.PowerImporterBlockEntity;
import com.teamofpowersim.powersim.block.machine.provider.solarpanel.SolarPanelBlockEntity;
import com.teamofpowersim.powersim.block.machine.provider.test.VoltageSourceBlockEntity;
import com.teamofpowersim.powersim.block.machine.provider.waterwheel.WaterWheelBlockEntity;
import com.teamofpowersim.powersim.block.machine.consumer.arc_furnace.ArcFurnaceBlockEntity;
import com.teamofpowersim.powersim.block.machine.consumer.crusher.ElectricCrusherBlockEntity;
import com.teamofpowersim.powersim.block.machine.consumer.power_exporter.PowerExporterBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PowerSim.MODID);

    public static final Supplier<BlockEntityType<ElectricCrusherBlockEntity>> ELECTRIC_CRUSHER_BE =
            BLOCK_ENTITY_TYPES.register("electric_crusher_be",
                    () -> BlockEntityType.Builder.of(
                            ElectricCrusherBlockEntity::new,
                            ModBlocks.ELECTRIC_CRUSHER.get()
                    )
                    .build(null));
    public static final Supplier<BlockEntityType<ArcFurnaceBlockEntity>> ARC_FURNACE_BE =
            BLOCK_ENTITY_TYPES.register("arc_furnace_entity",
                    () -> BlockEntityType.Builder.of(
                            ArcFurnaceBlockEntity::new,
                            ModBlocks.ARC_FURNACE.get()
                    )
                    .build(null));

    public static final Supplier<BlockEntityType<VoltageSourceBlockEntity>> VOLTAGE_SOURCE_BE =
            BLOCK_ENTITY_TYPES.register("voltage_source_be",
                    () -> BlockEntityType.Builder.of(VoltageSourceBlockEntity::new, ModBlocks.VOLTAGE_SOURCE.get()).build(null));

    public static final Supplier<BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_BE =
            BLOCK_ENTITY_TYPES.register("solar_panel_be",
                    () -> BlockEntityType.Builder.of(SolarPanelBlockEntity::new, ModBlocks.SOLAR_PANEL.get()).build(null));

    public static final Supplier<BlockEntityType<WaterWheelBlockEntity>> WATER_WHEEL_BE =
            BLOCK_ENTITY_TYPES.register("water_wheel_be",
                    () -> BlockEntityType.Builder.of(WaterWheelBlockEntity::new, ModBlocks.WATER_WHEEL.get()).build(null));

    public static final Supplier<BlockEntityType<ElectricGeneratorBlockEntity>> ELECTRIC_GENERATOR_BE =
            BLOCK_ENTITY_TYPES.register("electric_generator_be",
                    () -> BlockEntityType.Builder.of(ElectricGeneratorBlockEntity::new, ModBlocks.ELECTRIC_GENERATOR.get()).build(null));

    public static final Supplier<BlockEntityType<CombustionGeneratorBlockEntity>> COMBUSTION_GENERATOR_BE =
            BLOCK_ENTITY_TYPES.register("combustion_generator_be",
                    () -> BlockEntityType.Builder.of(CombustionGeneratorBlockEntity::new, ModBlocks.COMBUSTION_GENERATOR.get()).build(null));

    public static final Supplier<BlockEntityType<RefineryBlockEntity>> REFINERY_BE =
            BLOCK_ENTITY_TYPES.register("refinery_be",
                    () -> BlockEntityType.Builder.of(RefineryBlockEntity::new, ModBlocks.REFINERY.get()).build(null));

    public static final Supplier<BlockEntityType<SmallConnectorBlockEntity>> SMALL_CONNECTOR_BE =
            BLOCK_ENTITY_TYPES.register("small_connector_be",
                    () -> BlockEntityType.Builder.of(SmallConnectorBlockEntity::new, ModBlocks.SMALL_CONNECTOR.get()).build(null));

    public static final Supplier<BlockEntityType<LargeConnectorBlockEntity>> LARGE_CONNECTOR_BE =
            BLOCK_ENTITY_TYPES.register("large_connector_be",
                    () -> BlockEntityType.Builder.of(LargeConnectorBlockEntity::new, ModBlocks.LARGE_CONNECTOR.get()).build(null));

    public static final Supplier<BlockEntityType<DuoConnectorBlockEntity>> DUO_CONNECTOR_BE =
            BLOCK_ENTITY_TYPES.register("duo_connector_be",
                    () -> BlockEntityType.Builder.of(DuoConnectorBlockEntity::new, ModBlocks.DUO_CONNECTOR.get()).build(null));

    public static final Supplier<BlockEntityType<PowerExporterBlockEntity>> POWER_EXPORTER_BE =
            BLOCK_ENTITY_TYPES.register("power_exporter_be",
                    () -> BlockEntityType.Builder.of(PowerExporterBlockEntity::new, ModBlocks.POWER_EXPORTER.get()).build(null));

    public static final Supplier<BlockEntityType<PowerImporterBlockEntity>> POWER_IMPORTER_BE =
            BLOCK_ENTITY_TYPES.register("power_importer_be",
                    () -> BlockEntityType.Builder.of(PowerImporterBlockEntity::new, ModBlocks.POWER_IMPORTER.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
