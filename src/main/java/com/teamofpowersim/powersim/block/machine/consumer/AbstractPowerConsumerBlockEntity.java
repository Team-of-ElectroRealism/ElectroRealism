package com.teamofpowersim.powersim.block.machine.consumer;

import com.teamofpowersim.powersim.block.IVoltageConsumer;
import com.teamofpowersim.powersim.block.machine.AbstractMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractPowerConsumerBlockEntity extends AbstractMachineBlockEntity implements IVoltageConsumer {
    public AbstractPowerConsumerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public int getResistance() {
        return 200;
    }

    @Override
    public abstract void receiveVoltage(int voltage);

    @Override
    public abstract int getBufferCharge();

    @Override
    public abstract void setBufferCharge(int charge);
}
