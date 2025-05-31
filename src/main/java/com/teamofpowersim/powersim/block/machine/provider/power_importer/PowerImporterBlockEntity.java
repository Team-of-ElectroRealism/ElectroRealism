package com.teamofpowersim.powersim.block.machine.provider.power_importer;

import com.teamofpowersim.powersim.PowerSim; // For NetworkManager
import com.teamofpowersim.powersim.block.IActiveVoltageProvider; // Import this
import com.teamofpowersim.powersim.block.IVoltageConsumer;
import com.teamofpowersim.powersim.block.ModBlockEntityTypes;
import com.teamofpowersim.powersim.block.machine.provider.AbstractPowerProviderBlockEntity;
import com.teamofpowersim.powersim.energy.ModEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.slf4j.Logger; // For logging
import com.mojang.logging.LogUtils; // For logging

import javax.annotation.Nullable;

public class PowerImporterBlockEntity extends AbstractPowerProviderBlockEntity implements IActiveVoltageProvider { // Implement IActiveVoltageProvider
    private static final Logger LOGGER = LogUtils.getLogger();

    public final int nominalVoltage = 230; // Renamed for clarity
    private static final int ENERGY_TRANSFER_AMOUNT = 320; // Max FE transfer rate
    private static final int ENERGY_PER_TICK_REQUIRED_TO_BE_ACTIVE = 100; // FE consumed per tick to be "active"

    private static final String ENERGY_KEY = "power_importer.energy";

    private final ModEnergyStorage ENERGY_STORAGE = createEnergyStorage();

    private ModEnergyStorage createEnergyStorage() {
        return new ModEnergyStorage(64000, ENERGY_TRANSFER_AMOUNT) {
            @Override
            public void onEnergyChanged() {
                setChanged();
                if (getLevel() != null && !getLevel().isClientSide()) {
                    // Important: If energy level changes might toggle isActive(), need to check and mark dirty.
                    // This is handled in tick() now.
                    getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
                }
            }
        };
    }

    public PowerImporterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.POWER_IMPORTER_BE.get(), pos, blockState);
    }

    public IEnergyStorage getEnergyStorage(@Nullable Direction direction) {
        return this.ENERGY_STORAGE;
    }

    @Override
    public int getNominalVoltage() {
        return this.nominalVoltage;
    }

    @Override
    public boolean isActive() {
        // Active if it has enough energy stored to output for at least one tick's worth
        return this.ENERGY_STORAGE.getEnergyStored() >= ENERGY_PER_TICK_REQUIRED_TO_BE_ACTIVE;
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }

        boolean wasPreviouslyActive = isActive();

        if (isActive()) { // Check if it's currently able to provide power
            // Consume FE to represent power output
            this.ENERGY_STORAGE.extractEnergy(ENERGY_PER_TICK_REQUIRED_TO_BE_ACTIVE, false);
            // The direct transferVoltage call is likely redundant if ngspice handles power flow.
            // transferVoltage(level, pos); // Consider removing
        }

        boolean isCurrentlyActive = isActive();

        if (wasPreviouslyActive != isCurrentlyActive) {
            LOGGER.debug("Power Importer at {} active state changed: {} -> {}. Requesting network dirty status.",
                    pos.toShortString(), wasPreviouslyActive, isCurrentlyActive);
            if (PowerSim.NETWORK_MANAGER != null) {
                PowerSim.NETWORK_MANAGER.markNetworkDirty(this);
            } else {
                LOGGER.error("Power Importer at {} changed active state, but NetworkManager is NULL!", pos.toShortString());
            }
        }
        // setChanged() is called by ENERGY_STORAGE.onEnergyChanged() if energy actually changed.
    }


    @Override
    public boolean isFaceAllowed(BlockState state, Direction faceAccessed) {
        return true;
    }

    //  --- SAVE/LOAD ---
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt(ENERGY_KEY, ENERGY_STORAGE.getEnergyStored());
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ENERGY_STORAGE.setEnergy(tag.getInt(ENERGY_KEY));
    }
}