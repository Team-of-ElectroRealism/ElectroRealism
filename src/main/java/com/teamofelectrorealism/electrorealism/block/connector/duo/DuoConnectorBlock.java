package com.teamofelectrorealism.electrorealism.block.connector.duo;

import com.mojang.serialization.MapCodec;
import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.block.connector.AbstractConnectorBlock;
import com.teamofelectrorealism.electrorealism.block.connector.TerminalType;
import com.teamofelectrorealism.electrorealism.block.machine.AbstractMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition; // Import missing
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DuoConnectorBlock extends AbstractConnectorBlock {
    public static final MapCodec<DuoConnectorBlock> CODEC = simpleCodec(DuoConnectorBlock::new);

    // --- Voxel Shapes (Keep as they were) ---
    public static final VoxelShape UP_SHAPE = Block.box(2, 0, 6, 14, 5, 10);
    public static final VoxelShape DOWN_SHAPE = Block.box(2, 11, 6, 14, 16, 10); // Adjusted based on model height
    public static final VoxelShape NORTH_SHAPE = Block.box(2, 6, 11, 14, 10, 16);
    public static final VoxelShape SOUTH_SHAPE = Block.box(2, 6, 0, 14, 10, 5);
    public static final VoxelShape WEST_SHAPE = Block.box(11, 6, 2, 16, 10, 14);
    public static final VoxelShape EAST_SHAPE = Block.box(0, 6, 2, 5, 10, 14);

    // --- NEW BLOCKSTATE PROPERTIES ---
    public static final EnumProperty<TerminalType> TERMINAL_TYPE_0 = EnumProperty.create("terminal_type_0", TerminalType.class);
    public static final EnumProperty<TerminalType> TERMINAL_TYPE_1 = EnumProperty.create("terminal_type_1", TerminalType.class);

    public DuoConnectorBlock(Properties properties) {
        super(properties);
        // Register default state with FACING and BOTH terminal types
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH) // Default facing
                .setValue(TERMINAL_TYPE_0, TerminalType.None)
                .setValue(TERMINAL_TYPE_1, TerminalType.None)
        );
    }

    // --- REGISTER PROPERTIES ---
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); // Adds FACING
        builder.add(TERMINAL_TYPE_0, TERMINAL_TYPE_1); // Add our two new properties
    }

    // --- Set defaults on placement ---
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        // Get the facing direction as before
        Direction facing = context.getClickedFace();
        BlockState stateToPlace = this.defaultBlockState().setValue(FACING, facing)
                .setValue(TERMINAL_TYPE_0, TerminalType.None)
                .setValue(TERMINAL_TYPE_1, TerminalType.None);

        // Keep the placement validation logic if needed
        Level level = context.getLevel();
        BlockPos adjacentPos = context.getClickedPos().relative(facing.getOpposite());
        BlockEntity adjacentBE = level.getBlockEntity(adjacentPos);
        BlockState adjacentState = level.getBlockState(adjacentPos);

        if (adjacentBE instanceof AbstractMachineBlockEntity machineBE) {
            if (machineBE.isFaceAllowed(adjacentState, facing)) {
                return stateToPlace; // Place with default polarities
            }
        } else {
            // Allow placement even if not attached to a machine initially?
            // If you ONLY want it attachable to machines, return null here.
            // If it can be placed freely, return stateToPlace.
            // Assuming free placement for now:
            return stateToPlace;
        }
        // If only attachable and validation failed:
        return null;
    }

    // --- Interaction Logic ---
    // In DuoConnectorBlock.java

    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DuoConnectorBlockEntity duoBE) {
                Vec3 hitLocation = hitResult.getLocation();
                int targetNode = duoBE.getAvailableNode(hitLocation); // Should return 0 or 1

                // --- ORIGINAL LOGIC ---
                if (targetNode == 0) { // Clicked closer to physical node 0 (ring0 location)
                    // Cycle Terminal Type 0
                    TerminalType currentType = state.getValue(TERMINAL_TYPE_0);
                    TerminalType nextType = currentType.getNext();
                    level.setBlock(pos, state.setValue(TERMINAL_TYPE_0, nextType), 3);

                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.sendSystemMessage(Component.translatable("statusbar.electrorealism.connector_set_node", 0, nextType.getSerializedName()), true);
                    }
                    return InteractionResult.SUCCESS;

                } else if (targetNode == 1) { // Clicked closer to physical node 1 (ring1 location)
                    // Cycle Terminal Type 1
                    TerminalType currentType = state.getValue(TERMINAL_TYPE_1);
                    TerminalType nextType = currentType.getNext();
                    level.setBlock(pos, state.setValue(TERMINAL_TYPE_1, nextType), 3);

                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.sendSystemMessage(Component.translatable("statusbar.electrorealism.connector_set_node", 1, nextType.getSerializedName()), true);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // --- Keep Voxel Shapes, Ticker, Block Entity Creation, Codec ---
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        // Use the adjusted shapes
        return switch (facing) {
            case DOWN -> DOWN_SHAPE;
            case UP -> UP_SHAPE;
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
            // default -> UP_SHAPE; // Should always have a facing
        };
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new DuoConnectorBlockEntity(blockPos, blockState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null; // No client ticks usually needed for connectors unless animating
        }
        // Provide the ticker function reference
        return createTickerHelper(type, ModBlockEntityTypes.DUO_CONNECTOR_BE.get(), DuoConnectorBlockEntity::serverTick);
    }


    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}