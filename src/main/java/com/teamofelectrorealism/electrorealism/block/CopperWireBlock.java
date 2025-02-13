package com.teamofelectrorealism.electrorealism.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CopperWireBlock extends Block {
    public static final EnumProperty<RedstoneSide> NORTH = EnumProperty.create("north", RedstoneSide.class);
    public static final EnumProperty<RedstoneSide> EAST = EnumProperty.create("east", RedstoneSide.class);
    public static final EnumProperty<RedstoneSide> SOUTH = EnumProperty.create("south", RedstoneSide.class);
    public static final EnumProperty<RedstoneSide> WEST = EnumProperty.create("west", RedstoneSide.class);


    private static final VoxelShape WIRE_SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public CopperWireBlock() {
        super(BlockBehaviour.Properties.of()
                .noCollission()
                .strength(0.2F)
                .sound(SoundType.METAL));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, RedstoneSide.NONE)
                .setValue(EAST, RedstoneSide.NONE)
                .setValue(SOUTH, RedstoneSide.NONE)
                .setValue(WEST, RedstoneSide.NONE));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return WIRE_SHAPE;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (direction.getAxis().isHorizontal()) {
            RedstoneSide connection = getConnectionType(neighborState, world, neighborPos);
            System.out.println("Updating Copper Wire at " + pos + " Direction: " + direction + " Connection: " + connection);
            return state.setValue(getPropertyByDirection(direction), connection);
        }
        return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }


    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            updateConnections(world, pos);
        }
        super.onRemove(state, world, pos, newState, moved);
    }

    private void updateConnections(Level world, BlockPos pos) {
        RedstoneSide north = getConnectionType(world.getBlockState(pos.north()), world, pos.north());
        RedstoneSide south = getConnectionType(world.getBlockState(pos.south()), world, pos.south());
        RedstoneSide east = getConnectionType(world.getBlockState(pos.east()), world, pos.east());
        RedstoneSide west = getConnectionType(world.getBlockState(pos.west()), world, pos.west());

        BlockState newState = world.getBlockState(pos)
                .setValue(NORTH, north)
                .setValue(SOUTH, south)
                .setValue(EAST, east)
                .setValue(WEST, west);

        if (!world.getBlockState(pos).equals(newState)) {
            world.setBlockAndUpdate(pos, newState);
        }
    }

    private EnumProperty<RedstoneSide> getPropertyByDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> throw new IllegalArgumentException("Unexpected direction: " + direction);
        };
    }

    private RedstoneSide getConnectionType(BlockState neighborState, LevelAccessor world, BlockPos neighborPos) {
        if (neighborState.getBlock() instanceof CopperWireBlock) {
            return RedstoneSide.SIDE;
        } else if (neighborState.isRedstoneConductor(world, neighborPos)) {
            return RedstoneSide.UP;
        } else {
            return RedstoneSide.NONE;
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean moved) {
        world.scheduleTick(pos, this, 1);
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST);
    }
}
