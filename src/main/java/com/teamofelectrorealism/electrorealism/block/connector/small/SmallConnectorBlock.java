package com.teamofelectrorealism.electrorealism.block.connector.small;

import com.mojang.serialization.MapCodec;
import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.block.connector.AbstractConnectorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SmallConnectorBlock extends AbstractConnectorBlock {
    public static final MapCodec<SmallConnectorBlock> CODEC = simpleCodec(SmallConnectorBlock::new);
    public static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 5, 10);

    public SmallConnectorBlock(Properties properties) {
        super(properties);
    }

    private static void tick(Level level1, BlockPos blockPos, BlockState blockState1, SmallConnectorBlockEntity blockEntity) {
        blockEntity.tick(level1, blockPos, blockState1);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntityTypes.SMALL_CONNECTOR_BE.get(), SmallConnectorBlock::tick);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new SmallConnectorBlockEntity(blockPos, blockState);
    }


    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
