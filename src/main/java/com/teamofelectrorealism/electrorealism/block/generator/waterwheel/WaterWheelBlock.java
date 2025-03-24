package com.teamofelectrorealism.electrorealism.block.generator.waterwheel;

import com.mojang.serialization.MapCodec;
import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.block.generator.GeneratorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class WaterWheelBlock extends GeneratorBlock {
    public static final MapCodec<WaterWheelBlock> CODEC = simpleCodec(WaterWheelBlock::new);
    public WaterWheelBlock(Properties properties) {
        super(properties);
    }

    private static void tick(Level level1, BlockPos pos, BlockState state1, WaterWheelBlockEntity blockEntity) {
        blockEntity.tick(level1, pos, state1);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new WaterWheelBlockEntity(blockPos, blockState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if(level.isClientSide()) {
            return null;
        }

        return createTickerHelper(blockEntityType, ModBlockEntityTypes.WATER_WHEEL_BE.get(), WaterWheelBlock::tick);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
