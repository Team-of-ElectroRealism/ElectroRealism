package com.teamofelectrorealism.electrorealism.block.converter.fromfe;

import com.mojang.serialization.MapCodec;
import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.block.converter.EnergyConverterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class FEToPowerConverterBlock extends EnergyConverterBlock {
    public static final MapCodec<FEToPowerConverterBlock> CODEC = simpleCodec(FEToPowerConverterBlock::new);

    public FEToPowerConverterBlock(Properties properties) {
        super(properties);
    }

    private static void tick(Level level1, BlockPos pos, BlockState blockState1, FEToPowerConverterBlockEntity blockEntity) {
        blockEntity.tick(level1, pos, blockState1);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new FEToPowerConverterBlockEntity(blockPos, blockState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if(level.isClientSide()) {
            return null;
        }

        return createTickerHelper(blockEntityType, ModBlockEntityTypes.FE_CONVERTER_BE.get(), FEToPowerConverterBlock::tick);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
