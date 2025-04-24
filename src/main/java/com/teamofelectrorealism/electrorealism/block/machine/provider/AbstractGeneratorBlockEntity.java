package com.teamofelectrorealism.electrorealism.block.machine.provider;

import com.teamofelectrorealism.electrorealism.block.IVoltageProvider;
import com.teamofelectrorealism.electrorealism.block.machine.AbstractMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractGeneratorBlockEntity extends AbstractMachineBlockEntity implements IVoltageProvider {

    public AbstractGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public abstract int getVoltage();

    protected abstract void transferVoltage(Level level, BlockPos pos);
}
