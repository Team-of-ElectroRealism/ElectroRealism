package com.teamofelectrorealism.electrorealism.block.machine.consumer.power_exporter;

import com.mojang.serialization.MapCodec;
import com.teamofelectrorealism.electrorealism.block.machine.AbstractMachineBlockEntity;
import com.teamofelectrorealism.electrorealism.block.machine.consumer.AbstractPowerUserBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class PowerExporterBlock extends AbstractPowerUserBlock {
    public PowerExporterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return null;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return null;
    }

    @Override
    protected void tick(Level level1, BlockPos pos, BlockState state1, AbstractMachineBlockEntity blockEntity) {

    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }
}
