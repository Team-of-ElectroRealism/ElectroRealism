package com.teamofpowersim.powersim.block.machine.provider.power_importer;

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

import javax.annotation.Nullable;

public class PowerImporterBlockEntity extends AbstractPowerProviderBlockEntity {
    public final int voltage = 230;
    private static final int ENERGY_TRANSFER_AMOUNT = 320;
    private static final int ENERGY_PER_TICK_AMOUNT = 100;

    private static final String ENERGY_KEY = "power_importer.energy";

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

    public PowerImporterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.POWER_IMPORTER_BE.get(), pos, blockState);
    }

    public IEnergyStorage getEnergyStorage(@Nullable Direction direction) {
        return this.ENERGY_STORAGE;
    }

    @Override
    public int getVoltage() {
        return voltage;
    }

    @Override
    protected void transferVoltage(Level level, BlockPos pos) {
        for (Direction facing: Direction.values()) {
            BlockPos neighborPos = pos.offset(facing.getNormal());
            BlockEntity blockEntity = level.getBlockEntity(neighborPos);
            if (blockEntity instanceof IVoltageConsumer receiver) {
                receiver.receiveVoltage(voltage);
            }
        }
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (hasEnoughEnergyToTransfer()) {
            transferEnergy();
            transferVoltage(level, pos);
        }
    }

    private void transferEnergy() {
        this.ENERGY_STORAGE.extractEnergy(ENERGY_PER_TICK_AMOUNT, false);
    }

    private boolean hasEnoughEnergyToTransfer() {
        return this.ENERGY_STORAGE.getEnergyStored() >= ENERGY_PER_TICK_AMOUNT;
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
