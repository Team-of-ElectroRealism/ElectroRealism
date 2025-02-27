package com.teamofelectrorealism.electrorealism.block.connector.small;

import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.block.connector.AbstractConnectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class SmallConnectorBlockEntity extends AbstractConnectorBlockEntity {
    public SmallConnectorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.SMALL_CONNECTOR_BE.get(), pos, blockState);
    }

    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        System.out.println("Ticking!");
    }

    
}
