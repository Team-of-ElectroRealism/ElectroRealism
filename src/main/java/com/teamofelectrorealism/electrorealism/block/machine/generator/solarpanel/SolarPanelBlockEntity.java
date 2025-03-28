package com.teamofelectrorealism.electrorealism.block.machine.generator.solarpanel;

import com.teamofelectrorealism.electrorealism.block.IPowerReceiver;
import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.block.machine.generator.AbstractGeneratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SolarPanelBlockEntity extends AbstractGeneratorBlockEntity {
    private final int voltage = 230;

    public SolarPanelBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.SOLAR_PANEL_BE.get(), pos, blockState);
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        BlockPos skyPos = pos.above();
        if (level.canSeeSky(skyPos)) {
            this.transferVoltage(level, pos);
        }
    }

    // needs changing when block gets FACING value todo
    @Override
    public boolean isFacePositiveTerminal(BlockState state, Direction faceAccessed) {
        return true;
    }

    @Override
    public boolean isFaceNegativeTerminal(BlockState state, Direction faceAccessed) {
        return true;
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
            if (blockEntity instanceof IPowerReceiver) {
                IPowerReceiver receiver = (IPowerReceiver) blockEntity;

                receiver.receiveVoltage(voltage);
            }
        }
    }
}
