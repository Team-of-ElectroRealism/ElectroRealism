package com.teamofelectrorealism.electrorealism.block;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.block.crusher.ElectricCrusherBlockEntity;
import com.teamofelectrorealism.electrorealism.block.generator.solarpanel.SolarPanelBlock;
import com.teamofelectrorealism.electrorealism.block.generator.solarpanel.SolarPanelBlockEntity;
import com.teamofelectrorealism.electrorealism.block.generator.test.VoltageSourceBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ElectroRealism.MODID);

    public static final Supplier<BlockEntityType<ElectricCrusherBlockEntity>> ELECTRIC_CRUSHER_BE =
            BLOCK_ENTITY_TYPES.register("electric_crusher_be",
                    () -> BlockEntityType.Builder.of(ElectricCrusherBlockEntity::new, ModBlocks.ELECTRIC_CRUSHER.get()).build(null));

    public static final Supplier<BlockEntityType<VoltageSourceBlockEntity>> VOLTAGE_SOURCE_BE =
            BLOCK_ENTITY_TYPES.register("voltage_source_be",
                    () -> BlockEntityType.Builder.of(VoltageSourceBlockEntity::new, ModBlocks.VOLTAGE_SOURCE.get()).build(null));

    public static final Supplier<BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_BE =
            BLOCK_ENTITY_TYPES.register("solar_panel_be",
                    () -> BlockEntityType.Builder.of(SolarPanelBlockEntity::new, ModBlocks.SOLAR_PANEL.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
