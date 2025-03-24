package com.teamofelectrorealism.electrorealism.block.generator.test;

import com.mojang.serialization.MapCodec;
import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class VoltageSourceBlock extends BaseEntityBlock {
    public static final MapCodec<VoltageSourceBlock> CODEC = simpleCodec(VoltageSourceBlock::new);
    public VoltageSourceBlock(Properties properties) {
        super(properties);
    }

    private static void tick(Level level1, BlockPos pos, BlockState state1, VoltageSourceBlockEntity blockEntity) {
        blockEntity.tick(level1, pos, state1);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new VoltageSourceBlockEntity(blockPos, blockState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if(level.isClientSide()) {
            return null;
        }

        return createTickerHelper(blockEntityType, ModBlockEntityTypes.VOLTAGE_SOURCE_BE.get(), VoltageSourceBlock::tick);
    }
}
