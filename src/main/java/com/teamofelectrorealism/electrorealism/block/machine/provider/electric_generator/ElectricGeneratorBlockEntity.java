package com.teamofelectrorealism.electrorealism.block.machine.provider.electric_generator;

import com.teamofelectrorealism.electrorealism.block.IVoltageConsumer;
import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.block.machine.provider.AbstractPowerProviderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ElectricGeneratorBlockEntity extends AbstractPowerProviderBlockEntity {
    public final int voltage = 230;

    public ElectricGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.ELECTRIC_GENERATOR_BE.get(), pos, blockState);
    }

    @Override
    public int getVoltage() {
        return voltage;
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {

    }

    @Override
    public boolean isFaceAllowed(BlockState state, Direction faceAccessed) {
        return true;
    }

    @Override
    protected void transferVoltage(Level level, BlockPos pos) {
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
