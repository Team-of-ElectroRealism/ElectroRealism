package com.teamofelectrorealism.electrorealism.capabilities;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.block.machine.consumer.power_exporter.PowerExporterBlockEntity;
import com.teamofelectrorealism.electrorealism.block.machine.provider.power_importer.PowerImporterBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = ElectroRealism.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModCapabilities {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntityTypes.POWER_EXPORTER_BE.get(),
                PowerExporterBlockEntity::getEnergyStorage
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntityTypes.POWER_IMPORTER_BE.get(),
                PowerImporterBlockEntity::getEnergyStorage
        );
    }
}
