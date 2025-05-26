package com.teamofpowersim.powersim.block.machine.consumer.power_exporter;

import com.teamofpowersim.powersim.api.ElectricalAPI;
import com.teamofpowersim.powersim.block.ModBlockEntityTypes;
import com.teamofpowersim.powersim.block.machine.consumer.AbstractPowerConsumerBlockEntity;
import com.teamofpowersim.powersim.energy.ModEnergyStorage;
import com.teamofpowersim.powersim.energy.ModEnergyUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public class PowerExporterBlockEntity extends AbstractPowerConsumerBlockEntity {

    private static final String ENERGY_KEY = "power_exporter.energy";
    private static final String BUFFER_LEVEL_KEY = "power_exporter.buffer_level";
    private static final String BUFFER_TOTAL_LEVEL_KEY = "power_exporter.buffer_total_level";
    private static final String INTERNAL_RESISTANCE_KEY = "power_exporter.internal_resistance";

    private static final int TOTAL_BUFFER_CAPACITY = 2000; // In mAh
    private static final int INTERNAL_RESISTANCE = 10; // In ohm
    private static final int ENERGY_TRANSFER_AMOUNT = 320;

    private int bufferLevel;
    private int bufferTotalLevel = TOTAL_BUFFER_CAPACITY;
    private int internalResistance = INTERNAL_RESISTANCE;

    private final ModEnergyStorage ENERGY_STORAGE = createEnergyStorage();

    private ModEnergyStorage createEnergyStorage() {
        return new ModEnergyStorage(64000, ENERGY_TRANSFER_AMOUNT) {
            @Override
            public void onEnergyChanged() {
                setChanged();
                getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        };
    }

    public PowerExporterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.POWER_EXPORTER_BE.get(), pos, blockState);
    }
    public IEnergyStorage getEnergyStorage(@Nullable Direction direction) {
        return this.ENERGY_STORAGE;
    }

    @Override
    public int getBufferCharge() {
        return bufferLevel;
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (isPowered() && hasBufferEnoughCharge()) {
            fillUpOnEnergy();
            decreaseBufferCharge();
            pushEnergyToNeighborAbove();
        }
    }

    private boolean hasBufferEnoughCharge() {
        return bufferLevel >= 100;
    }

    private void decreaseBufferCharge() {
        bufferLevel = bufferLevel - 3;
    }

    private boolean isPowered() {
        return this.bufferLevel > 0;
    }

    @Override
    public boolean isFaceAllowed(BlockState state, Direction faceAccessed) {
        return true;
    }

    //  --- SAVE/LOAD ---
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt(ENERGY_KEY, ENERGY_STORAGE.getEnergyStored());
        tag.putInt(BUFFER_LEVEL_KEY, bufferLevel);
        tag.putInt(BUFFER_TOTAL_LEVEL_KEY, bufferTotalLevel);
        tag.putInt(INTERNAL_RESISTANCE_KEY, internalResistance);
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ENERGY_STORAGE.setEnergy(tag.getInt(ENERGY_KEY));
        bufferLevel = tag.getInt(BUFFER_LEVEL_KEY);
        bufferTotalLevel = tag.getInt(BUFFER_TOTAL_LEVEL_KEY);
        internalResistance = tag.getInt(INTERNAL_RESISTANCE_KEY);
    }

    // --- FUNCTIONALITY ---
    private void fillUpOnEnergy() {
        this.ENERGY_STORAGE.receiveEnergy(ENERGY_TRANSFER_AMOUNT, false);
    }

    private void pushEnergyToNeighborAbove() {
        if (ModEnergyUtil.doesBlockHaveEnergyStorage(this.worldPosition.above(), this.level)) {
            ModEnergyUtil.move(this.worldPosition, this.worldPosition.above(), ENERGY_TRANSFER_AMOUNT, this.level);
        }
    }
}
