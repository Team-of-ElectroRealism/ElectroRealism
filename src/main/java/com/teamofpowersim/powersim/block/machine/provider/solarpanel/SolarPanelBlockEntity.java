package com.teamofpowersim.powersim.block.machine.provider.solarpanel;

import com.teamofpowersim.powersim.block.IVoltageConsumer;
import com.teamofpowersim.powersim.block.ModBlockEntityTypes;
import com.teamofpowersim.powersim.block.machine.provider.AbstractPowerProviderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SolarPanelBlockEntity extends AbstractPowerProviderBlockEntity {
    private final int voltage = 230;

    public SolarPanelBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.SOLAR_PANEL_BE.get(), pos, blockState);
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        BlockPos skyPos = pos.above();
        if (level.canSeeSky(skyPos)) {
            this.transferVoltage(level, pos);
        }
    }

    @Override
    public boolean isFaceAllowed(BlockState state, Direction faceAccessed) {
        return false;
    }

    @Override
    public int getVoltage() {
        return voltage;
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
