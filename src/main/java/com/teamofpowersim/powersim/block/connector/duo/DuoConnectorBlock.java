package com.teamofpowersim.powersim.block.connector.duo;

import com.mojang.serialization.MapCodec;
import com.teamofpowersim.powersim.block.ModBlockEntityTypes;
import com.teamofpowersim.powersim.block.connector.AbstractConnectorBlock;
import com.teamofpowersim.powersim.block.connector.ConnectorPolarity;
import com.teamofpowersim.powersim.block.machine.AbstractMachineBlockEntity;
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

    public static final VoxelShape UP_SHAPE = Block.box(2, 0, 6, 14, 5, 10);
    public static final VoxelShape DOWN_SHAPE = Block.box(2, 11, 6, 14, 16, 10); // Adjusted based on model height
    public static final VoxelShape NORTH_SHAPE = Block.box(2, 6, 11, 14, 10, 16);
    public static final VoxelShape SOUTH_SHAPE = Block.box(2, 6, 0, 14, 10, 5);
    public static final VoxelShape WEST_SHAPE = Block.box(11, 6, 2, 16, 10, 14);
    public static final VoxelShape EAST_SHAPE = Block.box(0, 6, 2, 5, 10, 14);

    public static final EnumProperty<ConnectorPolarity> TERMINAL_TYPE_0 = EnumProperty.create("terminal_type_0", ConnectorPolarity.class);
    public static final EnumProperty<ConnectorPolarity> TERMINAL_TYPE_1 = EnumProperty.create("terminal_type_1", ConnectorPolarity.class);

    public DuoConnectorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(TERMINAL_TYPE_0, ConnectorPolarity.NONE)
                .setValue(TERMINAL_TYPE_1, ConnectorPolarity.NONE)
        );
    }

    // --- REGISTER PROPERTIES ---
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TERMINAL_TYPE_0, TERMINAL_TYPE_1);
    }

    // --- Set defaults on placement ---
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockState stateToPlace = this.defaultBlockState().setValue(FACING, facing)
                .setValue(TERMINAL_TYPE_0, ConnectorPolarity.NONE)
                .setValue(TERMINAL_TYPE_1, ConnectorPolarity.NONE);

        Level level = context.getLevel();
        BlockPos adjacentPos = context.getClickedPos().relative(facing.getOpposite());
        BlockEntity adjacentBE = level.getBlockEntity(adjacentPos);
        BlockState adjacentState = level.getBlockState(adjacentPos);

        if (adjacentBE instanceof AbstractMachineBlockEntity machineBE) {
            if (machineBE.isFaceAllowed(adjacentState, facing)) {
                return stateToPlace;
            }
        } else {
            return stateToPlace;
        }
        return null;
    }

    // --- Interaction Logic ---

    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DuoConnectorBlockEntity duoBE) {
                Vec3 hitLocation = hitResult.getLocation();
                int targetNode = duoBE.getAvailableNode(hitLocation);

                if (targetNode == 0) {
                    ConnectorPolarity currentType = state.getValue(TERMINAL_TYPE_0);
                    ConnectorPolarity nextType = currentType.getNext();
                    level.setBlock(pos, state.setValue(TERMINAL_TYPE_0, nextType), 3);

                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.sendSystemMessage(Component.translatable("statusbar.powersim.connector_set", 0, nextType.getSerializedName()), true);
                    }
                    return InteractionResult.SUCCESS;

                } else if (targetNode == 1) {
                    ConnectorPolarity currentType = state.getValue(TERMINAL_TYPE_1);
                    ConnectorPolarity nextType = currentType.getNext();
                    level.setBlock(pos, state.setValue(TERMINAL_TYPE_1, nextType), 3);

                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.sendSystemMessage(Component.translatable("statusbar.powersim.connector_set", 1, nextType.getSerializedName()), true);
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
        return switch (facing) {
            case DOWN -> DOWN_SHAPE;
            case UP -> UP_SHAPE;
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
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
            return null;
        }
        return createTickerHelper(type, ModBlockEntityTypes.DUO_CONNECTOR_BE.get(), DuoConnectorBlockEntity::serverTick);
    }


    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}