package com.teamofpowersim.powersim.block.machine.provider;

import com.teamofpowersim.powersim.block.IVoltageProvider;
import com.teamofpowersim.powersim.block.machine.AbstractMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractPowerProviderBlockEntity extends AbstractMachineBlockEntity implements IVoltageProvider {

    public AbstractPowerProviderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public abstract int getVoltage();

    protected abstract void transferVoltage(Level level, BlockPos pos);
}
