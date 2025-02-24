package com.teamofelectrorealism.electrorealism.block.connector;

import com.teamofelectrorealism.electrorealism.network.PowerNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractConnectorBlockEntity extends BlockEntity {
    private PowerNetwork powerNetwork;

    public AbstractConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }
}
