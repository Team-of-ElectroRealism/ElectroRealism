package com.teamofelectrorealism.electrorealism.block.machine.consumer.power_exporter;

import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.block.machine.consumer.AbstractPowerUserBlockEntity;
import com.teamofelectrorealism.electrorealism.energy.ModEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class PowerExporterBlockEntity extends AbstractPowerUserBlockEntity {
    private static final int ENERGY_TRANSFER_AMOUNT = 320;

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
}
