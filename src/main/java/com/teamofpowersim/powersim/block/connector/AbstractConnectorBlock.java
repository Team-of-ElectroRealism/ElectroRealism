package com.teamofpowersim.powersim.block.connector;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractConnectorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    protected AbstractConnectorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction dir = context.getClickedFace();
        return this.defaultBlockState().setValue(FACING, dir);
    }

    /**
     * Checks if this block can stay at the current position.
     *
     * @param state The current block state.
     * @param level The level the block is in.
     * @param pos   The position of the block.
     * @return True if the block can stay, false otherwise.
     */
    // https://github.com/Creators-of-Create/Create/blob/mc1.20.1/dev/src/main/java/com/simibubi/create/content/kinetics/crank/HandCrankBlock.java#L90
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING).getOpposite();
        BlockPos neighbourPos = pos.relative(facing);
        BlockState neighbour = level.getBlockState(neighbourPos);
        return !neighbour.getCollisionShape(level, neighbourPos).isEmpty();
    }

    /**
     * Called when a neighboring block changes.
     *
     * @param state          The current block state.
     * @param level          The level the block is in.
     * @param pos            The position of the block.
     * @param neighborBlock  The block that changed.
     * @param neighborPos    The position of the block that changed.
     * @param movedByPiston  Whether the change was caused by a piston.
     */

    // https://github.com/Creators-of-Create/Create/blob/mc1.20.1/dev/src/main/java/com/simibubi/create/content/kinetics/crank/HandCrankBlock.java#L100
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide)
            return;

        Direction blockFacing = state.getValue(FACING);
        if (neighborPos.equals(pos.relative(blockFacing.getOpposite()))) {
            if (!canSurvive(state, level, pos)) {
                level.destroyBlock(pos, true);
                return;
            }
        }
    }

    @Override
    public BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation direction) {
        return super.rotate(state, direction);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public abstract @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType);
}
