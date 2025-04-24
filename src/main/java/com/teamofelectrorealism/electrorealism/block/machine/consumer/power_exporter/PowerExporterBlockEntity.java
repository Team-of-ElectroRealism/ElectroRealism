package com.teamofelectrorealism.electrorealism.block.machine.consumer.power_exporter;

import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.block.machine.consumer.AbstractPowerConsumerBlockEntity;
import com.teamofelectrorealism.electrorealism.energy.ModEnergyStorage;
import com.teamofelectrorealism.electrorealism.energy.ModEnergyUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public class PowerExporterBlockEntity extends AbstractPowerConsumerBlockEntity {
    private static final int ENERGY_TRANSFER_AMOUNT = 320;

    private static final String ENERGY_KEY = "power_exporter.energy";

    private final ModEnergyStorage ENERGY_STORAGE = createEnergyStorage();

    private ModEnergyStorage createEnergyStorage() {
        return new ModEnergyStorage(64000, 320) {
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
    public void receiveVoltage(int voltage) {

    }

    @Override
    public int getBufferCharge() {
        return 0;
    }

    @Override
    public void setBufferCharge(int charge) {

    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {

    }

    @Override
    public boolean isFaceAllowed(BlockState state, Direction faceAccessed) {
        return false;
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
