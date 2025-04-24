package com.teamofelectrorealism.electrorealism.block;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopperWireBlock extends Block {

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP_NORTH = BooleanProperty.create("up_north");
    public static final BooleanProperty UP_SOUTH = BooleanProperty.create("up_south");
    public static final BooleanProperty UP_EAST = BooleanProperty.create("up_east");
    public static final BooleanProperty UP_WEST = BooleanProperty.create("up_west");
    public static final BooleanProperty DOWN_NORTH = BooleanProperty.create("down_north");
    public static final BooleanProperty DOWN_SOUTH = BooleanProperty.create("down_south");
    public static final BooleanProperty DOWN_EAST = BooleanProperty.create("down_east");
    public static final BooleanProperty DOWN_WEST = BooleanProperty.create("down_west");

    private static final Logger LOGGER = LoggerFactory.getLogger(CopperWireBlock.class);

    private static final VoxelShape WIRE_SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public CopperWireBlock(Properties sound) {
        super(BlockBehaviour.Properties.of()
                .noCollission()
                .instabreak()
                .strength(0.2F)
                .sound(SoundType.METAL));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP_NORTH, false)
                .setValue(UP_SOUTH, false)
                .setValue(UP_EAST, false)
                .setValue(UP_WEST, false)
                .setValue(DOWN_NORTH, false)
                .setValue(DOWN_SOUTH, false)
                .setValue(DOWN_EAST, false)
                .setValue(DOWN_WEST, false));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        boolean north = world.getBlockState(pos.north()).getBlock() instanceof CopperWireBlock;
        boolean south = world.getBlockState(pos.south()).getBlock() instanceof CopperWireBlock;
        boolean east = world.getBlockState(pos.east()).getBlock() instanceof CopperWireBlock;
        boolean west = world.getBlockState(pos.west()).getBlock() instanceof CopperWireBlock;

        // Diagonal checks for inclines (one block to the side AND up/down)
        boolean upNorth = world.getBlockState(pos.north().above()).getBlock() instanceof CopperWireBlock;
        boolean upSouth = world.getBlockState(pos.south().above()).getBlock() instanceof CopperWireBlock;
        boolean upEast = world.getBlockState(pos.east().above()).getBlock() instanceof CopperWireBlock;
        boolean upWest = world.getBlockState(pos.west().above()).getBlock() instanceof CopperWireBlock;

        boolean downNorth = world.getBlockState(pos.north().below()).getBlock() instanceof CopperWireBlock;
        boolean downSouth = world.getBlockState(pos.south().below()).getBlock() instanceof CopperWireBlock;
        boolean downEast = world.getBlockState(pos.east().below()).getBlock() instanceof CopperWireBlock;
        boolean downWest = world.getBlockState(pos.west().below()).getBlock() instanceof CopperWireBlock;

        BlockState newState = state.setValue(NORTH, north)
                .setValue(SOUTH, south)
                .setValue(EAST, east)
                .setValue(WEST, west)
                .setValue(UP_NORTH, upNorth)
                .setValue(UP_SOUTH, upSouth)
                .setValue(UP_EAST, upEast)
                .setValue(UP_WEST, upWest)
                .setValue(DOWN_NORTH, downNorth)
                .setValue(DOWN_SOUTH, downSouth)
                .setValue(DOWN_EAST, downEast)
                .setValue(DOWN_WEST, downWest);

        LOGGER.info("updateShape called at {} | North: {} | South: {} | East: {} | West: {}", pos, north, south, east, west);

        // Force update using sendBlockUpdated instead of manual render update
        if (!state.equals(newState) && world instanceof Level level) {
            level.sendBlockUpdated(pos, newState, newState, 3);
        }

        return newState;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            LOGGER.info("onRemove called at {} | Block removed: {}", pos, state);
            super.onRemove(state, world, pos, newState, moved);

            // Force neighbors to update their connections
            world.updateNeighborsAt(pos.north(), this);
            world.updateNeighborsAt(pos.south(), this);
            world.updateNeighborsAt(pos.east(), this);
            world.updateNeighborsAt(pos.west(), this);
            world.updateNeighborsAt(pos.above(), this);
            world.updateNeighborsAt(pos.below(), this);

            // Force client rendering update
            forceRenderUpdate(world, pos);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean flag) {
        LOGGER.info("neighborChanged at {} | From: {} | Block: {}", pos, fromPos, block);

        BlockState newState = world.getBlockState(pos);
        if (state == newState) return; // Prevent redundant updates

        super.neighborChanged(state, world, pos, block, fromPos, flag);

        // Update surrounding blocks, including above and below
        world.updateNeighborsAt(pos.north(), this);
        world.updateNeighborsAt(pos.south(), this);
        world.updateNeighborsAt(pos.east(), this);
        world.updateNeighborsAt(pos.west(), this);
        world.updateNeighborsAt(pos.above(), this); // <-- Ensure above updates
        world.updateNeighborsAt(pos.below(), this); // <-- Ensure below updates

        forceRenderUpdate(world, pos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST,
                UP_NORTH, UP_SOUTH, UP_EAST, UP_WEST,
                DOWN_NORTH, DOWN_SOUTH, DOWN_EAST, DOWN_WEST);
    }


    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();

        boolean north = !world.getBlockState(pos.north()).isAir() && world.getBlockState(pos.north()).getBlock() instanceof CopperWireBlock;
        boolean south = !world.getBlockState(pos.south()).isAir() && world.getBlockState(pos.south()).getBlock() instanceof CopperWireBlock;
        boolean east = !world.getBlockState(pos.east()).isAir() && world.getBlockState(pos.east()).getBlock() instanceof CopperWireBlock;
        boolean west = !world.getBlockState(pos.west()).isAir() && world.getBlockState(pos.west()).getBlock() instanceof CopperWireBlock;

        boolean up_north = !world.getBlockState(pos.north().above()).isAir() && world.getBlockState(pos.north().above()).getBlock() instanceof CopperWireBlock;
        boolean up_south = !world.getBlockState(pos.south().above()).isAir() && world.getBlockState(pos.south().above()).getBlock() instanceof CopperWireBlock;
        boolean up_east = !world.getBlockState(pos.east().above()).isAir() && world.getBlockState(pos.east().above()).getBlock() instanceof CopperWireBlock;
        boolean up_west = !world.getBlockState(pos.west().above()).isAir() && world.getBlockState(pos.west().above()).getBlock() instanceof CopperWireBlock;

        boolean down_north = !world.getBlockState(pos.north().below()).isAir() && world.getBlockState(pos.north().below()).getBlock() instanceof CopperWireBlock;
        boolean down_south = !world.getBlockState(pos.south().below()).isAir() && world.getBlockState(pos.south().below()).getBlock() instanceof CopperWireBlock;
        boolean down_east = !world.getBlockState(pos.east().below()).isAir() && world.getBlockState(pos.east().below()).getBlock() instanceof CopperWireBlock;
        boolean down_west = !world.getBlockState(pos.west().below()).isAir() && world.getBlockState(pos.west().below()).getBlock() instanceof CopperWireBlock;

        LOGGER.info("getStateForPlacement called at {} | North: {} | South: {} | East: {} | West: {}", pos, north, south, east, west);

        return this.defaultBlockState()
                .setValue(NORTH, north)
                .setValue(SOUTH, south)
                .setValue(EAST, east)
                .setValue(WEST, west)
                .setValue(UP_NORTH, up_north)
                .setValue(UP_SOUTH, up_south)
                .setValue(UP_EAST, up_east)
                .setValue(UP_WEST, up_west)
                .setValue(DOWN_NORTH, down_north)
                .setValue(DOWN_SOUTH, down_south)
                .setValue(DOWN_EAST, down_east)
                .setValue(DOWN_WEST, down_west);
    }

    private void forceRenderUpdate(Level world, BlockPos pos) {
        if (world instanceof Level level) {
            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, 3);// Flags: 3 means client update + rendering update
            LOGGER.info("Forced render update at {}", pos);
        }
    }
}
