package com.teamofelectrorealism.electrorealism.block.generator.waterwheel;

import com.teamofelectrorealism.electrorealism.block.IPowerReceiver;
import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.block.generator.GeneratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class WaterWheelBlockEntity extends GeneratorBlockEntity {
    public final int voltage = 230;

    public WaterWheelBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.WATER_WHEEL_BE.get(), pos, blockState);
    }

    @Override
    public int getVoltage() {
        return voltage;
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if(isMovingWaterBelow(level, pos)) {
            this.transferVoltage(level, pos);
        }
    }

    @Override
    protected void transferVoltage(Level level, BlockPos pos) {
        for (Direction facing: Direction.values()) {
            BlockPos neighborPos = pos.offset(facing.getNormal());
            BlockEntity blockEntity = level.getBlockEntity(neighborPos);
            if (blockEntity instanceof IPowerReceiver) {
                IPowerReceiver receiver = (IPowerReceiver) blockEntity;

                receiver.receiveVoltage(voltage);
            }
        }
    }

    public static boolean isMovingWaterBelow(Level level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        FluidState fluidState = belowState.getFluidState();

        if (fluidState.getType() instanceof FlowingFluid) {
            FlowingFluid flowingFluid = (FlowingFluid) fluidState.getType();
            if (flowingFluid.isSame(Fluids.FLOWING_WATER)) {
                return fluidState.isSource() ? false : !fluidState.isEmpty();
            }
        }
        return false;
    }
}
