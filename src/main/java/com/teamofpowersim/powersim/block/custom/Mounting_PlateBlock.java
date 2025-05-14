package com.teamofpowersim.powersim.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class Mounting_PlateBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Mounting_PlateBlock> CODEC = simpleCodec(Mounting_PlateBlock::new);
    private static final VoxelShape SHAPE_NORTH = Block.box(5, 0, 12, 11, 8, 22);
    private static final VoxelShape SHAPE_SOUTH = Block.box(5, 0, -6, 11, 8, 4);
    private static final VoxelShape SHAPE_WEST  = Block.box(12, 0, 5, 22, 8, 11);
    private static final VoxelShape SHAPE_EAST  = Block.box(-6, 0, 5, 4, 8, 11);

    public Mounting_PlateBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case EAST  -> SHAPE_EAST;
            case SOUTH -> SHAPE_SOUTH;
            case WEST  -> SHAPE_WEST;
            default    -> SHAPE_NORTH;
        };
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

}
