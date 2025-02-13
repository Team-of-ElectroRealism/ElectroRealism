package com.teamofelectrorealism.electrorealism.block.generator;

import com.teamofelectrorealism.electrorealism.block.IPowerProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class GeneratorBlockEntity extends BlockEntity implements IPowerProvider {

    public GeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public abstract int getVoltage();

    public void tick(Level level, BlockPos pos, BlockState state) {
        this.transferVoltage(level, pos);
    }

    protected abstract void transferVoltage(Level level, BlockPos pos);
}
