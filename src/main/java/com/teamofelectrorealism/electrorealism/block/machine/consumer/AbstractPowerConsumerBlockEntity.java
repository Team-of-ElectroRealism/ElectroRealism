package com.teamofelectrorealism.electrorealism.block.machine.consumer;

import com.teamofelectrorealism.electrorealism.block.IPowerReceiver;
import com.teamofelectrorealism.electrorealism.block.machine.AbstractMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractPowerConsumerBlockEntity extends AbstractMachineBlockEntity implements IPowerReceiver {
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
