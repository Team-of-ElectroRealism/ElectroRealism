package com.teamofelectrorealism.electrorealism.block.converter.fromfe;

import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.block.converter.EnergyConverterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class FEToPowerConverterBlockEntity extends EnergyConverterBlockEntity {
    public FEToPowerConverterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.FE_CONVERTER_BE.get(), pos, blockState);
    }

    @Override
    public int getEnergyToConvert() {
        return 0;
    }

    @Override
    public void convertEnergy() {

    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {

    }
}
