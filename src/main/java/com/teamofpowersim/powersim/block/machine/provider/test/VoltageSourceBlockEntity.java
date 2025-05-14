package com.teamofpowersim.powersim.block.machine.provider.test;

import com.teamofpowersim.powersim.block.IVoltageProvider;
import com.teamofpowersim.powersim.block.IVoltageConsumer;
import com.teamofpowersim.powersim.block.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class VoltageSourceBlockEntity extends BlockEntity implements IVoltageProvider {
    private final int voltage = 230;

    public VoltageSourceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.VOLTAGE_SOURCE_BE.get(), pos, state);
    }

    @Override
    public int getVoltage() {
        return voltage;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        this.transferVoltage(level, pos);
    }

    private void transferVoltage(Level level, BlockPos pos) {
        for (Direction facing: Direction.values()) {
            BlockPos neighborPos = pos.offset(facing.getNormal());
            BlockEntity blockEntity = level.getBlockEntity(neighborPos);
            if (blockEntity instanceof IVoltageConsumer) {
                IVoltageConsumer receiver = (IVoltageConsumer) blockEntity;

                receiver.receiveVoltage(voltage);
            }
        }
    }
}
