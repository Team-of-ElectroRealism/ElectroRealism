package com.teamofelectrorealism.electrorealism.block.user;

import com.teamofelectrorealism.electrorealism.block.IPowerReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractPowerUserBlockEntity extends BlockEntity implements IPowerReceiver {
    public AbstractPowerUserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
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

    public abstract void tick(Level level, BlockPos pos, BlockState state);
}
