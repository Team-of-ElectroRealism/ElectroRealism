package com.teamofelectrorealism.electrorealism.block.machine.provider.power_importer;

import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.block.machine.consumer.AbstractPowerUserBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class PowerImporterBlockEntity extends AbstractPowerUserBlockEntity {
    public PowerImporterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.POWER_IMPORTER_BE.get(), pos, blockState);
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
