package com.teamofelectrorealism.electrorealism.block.components.CopperWire;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.network.INetworkMember;
import com.teamofelectrorealism.electrorealism.power.IWireNode;
import com.teamofelectrorealism.electrorealism.power.WireType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopperWireBlock extends Block implements EntityBlock {

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP_NORTH = BooleanProperty.create("up_north");
    public static final BooleanProperty UP_SOUTH = BooleanProperty.create("up_south");
    public static final BooleanProperty UP_EAST = BooleanProperty.create("up_east");
    public static final BooleanProperty UP_WEST = BooleanProperty.create("up_west");

    private static final Logger LOGGER = LoggerFactory.getLogger(CopperWireBlock.class);

    private static final VoxelShape WIRE_SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    private boolean shouldConnectTo(LevelAccessor world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        BlockEntity be = world.getBlockEntity(pos);

        boolean isConnectable = state.getBlock() instanceof CopperWireBlock;

        if (be != null) {
            if (be instanceof IWireNode) {
                isConnectable = true;
            }
        }

        LOGGER.info("shouldConnectTo at {}: block={} | blockEntity={} | connectable={}", pos, state.getBlock(), be, isConnectable);
        return isConnectable;
    }

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
                .setValue(UP_WEST, false));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        boolean north = shouldConnectTo(world, pos.north());
        boolean south = shouldConnectTo(world, pos.south());
        boolean east = shouldConnectTo(world, pos.east());
        boolean west = shouldConnectTo(world, pos.west());

        boolean upNorth = shouldConnectTo(world, pos.north().above());
        boolean upSouth = shouldConnectTo(world, pos.south().above());
        boolean upEast = shouldConnectTo(world, pos.east().above());
        boolean upWest = shouldConnectTo(world, pos.west().above());

        BlockState newState = state.setValue(NORTH, north)
                .setValue(SOUTH, south)
                .setValue(EAST, east)
                .setValue(WEST, west)
                .setValue(UP_NORTH, upNorth)
                .setValue(UP_SOUTH, upSouth)
                .setValue(UP_EAST, upEast)
                .setValue(UP_WEST, upWest);

        LOGGER.info("updateShape called at {} | North: {} | South: {} | East: {} | West: {}", pos, north, south, east, west);

        if (!state.equals(newState) && world instanceof Level level) {
            level.sendBlockUpdated(pos, newState, newState, 3);
        }

        return newState;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        LOGGER.info("onPlace triggered at {}", pos);
        super.onPlace(state, level, pos, oldState, moved);

        if (level.getBlockEntity(pos) instanceof CopperWireBlockEntity wire) {
            wire.setPowered(false);
        }

        if (level.isClientSide) return;

        BlockEntity selfBe = level.getBlockEntity(pos);
        LOGGER.info("  Self BlockEntity at {} = {}", pos, selfBe);
        if (!(selfBe instanceof IWireNode selfNode)) return;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            LOGGER.info("  Neighbor BlockEntity at {} = {}", pos.above(), neighborBe);
            if (neighborBe instanceof IWireNode neighborNode) {
                int selfIndex = selfNode.getAvailableNode();
                int neighborIndex = neighborNode.getAvailableNode();

                if (selfIndex != -1 && neighborIndex != -1) {
                    IWireNode.connect(level, pos, selfIndex, neighborPos, neighborIndex, WireType.COPPER);
                    LOGGER.info("Connected copper wires at {} <-> {}", pos, neighborPos);
                }
            }
        }
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

            forceRenderUpdate(world, pos);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean flag) {
        LOGGER.info("neighborChanged at {} | From: {} | Block: {}", pos, fromPos, block);

        BlockState newState = world.getBlockState(pos);
        if (state == newState) return;

        super.neighborChanged(state, world, pos, block, fromPos, flag);

        world.updateNeighborsAt(pos.north(), this);
        world.updateNeighborsAt(pos.south(), this);
        world.updateNeighborsAt(pos.east(), this);
        world.updateNeighborsAt(pos.west(), this);
        world.updateNeighborsAt(pos.above(), this);
        world.updateNeighborsAt(pos.below(), this);

        forceRenderUpdate(world, pos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST,
                UP_NORTH, UP_SOUTH, UP_EAST, UP_WEST);
    }


    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();

        boolean north = shouldConnectTo(world, pos.north());
        boolean south = shouldConnectTo(world, pos.south());
        boolean east = shouldConnectTo(world, pos.east());
        boolean west = shouldConnectTo(world, pos.west());

        boolean up_north = shouldConnectTo(world, pos.north().above());
        boolean up_south = shouldConnectTo(world, pos.south().above());
        boolean up_east = shouldConnectTo(world, pos.east().above());
        boolean up_west = shouldConnectTo(world, pos.west().above());

        LOGGER.info("getStateForPlacement called at {} | North: {} | South: {} | East: {} | West: {}", pos, north, south, east, west);

        return this.defaultBlockState()
                .setValue(NORTH, north)
                .setValue(SOUTH, south)
                .setValue(EAST, east)
                .setValue(WEST, west)
                .setValue(UP_NORTH, up_north)
                .setValue(UP_SOUTH, up_south)
                .setValue(UP_EAST, up_east)
                .setValue(UP_WEST, up_west);
    }

    private void forceRenderUpdate(Level world, BlockPos pos) {
        if (world instanceof Level level) {
            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, 3);// Flags: 3 means client update + rendering update
            LOGGER.info("Forced render update at {}", pos);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof INetworkMember member && member.getNetworkId() != null) {
            ElectroRealism.NETWORK_MANAGER.registerINetworkMemberInNetwork(member.getNetworkId(), member);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopperWireBlockEntity(ModBlockEntityTypes.COPPER_WIRE_BE.get(), pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof CopperWireBlockEntity wireBE) {
                CopperWireBlockEntity.tick(lvl, pos, st, wireBE);
            }
        };
    }
}
