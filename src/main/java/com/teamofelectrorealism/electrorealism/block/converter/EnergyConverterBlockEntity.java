package com.teamofelectrorealism.electrorealism.block.converter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class EnergyConverterBlockEntity extends BlockEntity implements IEnergyConverter{
    public EnergyConverterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public abstract int getEnergyToConvert();

    @Override
    public abstract void convertEnergy();


}
