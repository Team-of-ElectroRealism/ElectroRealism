package com.teamofelectrorealism.electrorealism.block.connector.small;

import com.mojang.serialization.MapCodec;
import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.block.connector.AbstractConnectorBlock;
import com.teamofelectrorealism.electrorealism.block.connector.TerminalType;
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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SmallConnectorBlock extends AbstractConnectorBlock {
    public static final MapCodec<SmallConnectorBlock> CODEC = simpleCodec(SmallConnectorBlock::new);
    public static final VoxelShape BASE_SHAPE = Block.box(6, 0, 6, 10, 5, 10);
    public static final VoxelShape UP_SHAPE = BASE_SHAPE;
    public static final VoxelShape DOWN_SHAPE = BASE_SHAPE.move(0, 11/16f, 0);
    public static final VoxelShape NORTH_SHAPE = Block.box(6, 6, 11, 10, 10, 16);
    public static final VoxelShape SOUTH_SHAPE = Block.box(6, 6, 0, 10, 10, 5);
    public static final VoxelShape WEST_SHAPE = Block.box(11, 6, 6, 16, 10, 10);
    public static final VoxelShape EAST_SHAPE = Block.box(0, 6, 6, 5, 10, 10);
    public static final EnumProperty<TerminalType> TERMINAL_TYPE = EnumProperty.create("terminal_type", TerminalType.class);

    public SmallConnectorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(TERMINAL_TYPE, TerminalType.None));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TERMINAL_TYPE);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context).setValue(TERMINAL_TYPE, TerminalType.None);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.getMainHandItem().isEmpty()) {
            if (!level.isClientSide()) {
                TerminalType currentType = state.getValue(TERMINAL_TYPE);
                TerminalType nextType = currentType.getNext();
                level.setBlock(pos, state.setValue(TERMINAL_TYPE, nextType), 3); // 3 = Send update to client

                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(Component.translatable("statusbar.electrorealism.connector_set", nextType.getSerializedName()), true); // Use overlay (true)
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

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
            default -> BASE_SHAPE;
        };
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
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new SmallConnectorBlockEntity(blockPos, blockState);
    }


    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
