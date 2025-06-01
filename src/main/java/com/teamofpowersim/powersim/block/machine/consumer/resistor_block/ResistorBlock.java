package com.teamofpowersim.powersim.block.machine.consumer.resistor_block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.teamofpowersim.powersim.block.ModBlockEntityTypes;
import com.teamofpowersim.powersim.block.machine.AbstractMachineBlockEntity;
import com.teamofpowersim.powersim.block.machine.consumer.AbstractPowerConsumerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class ResistorBlock extends AbstractPowerConsumerBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final MapCodec<ResistorBlock> CODEC = simpleCodec(ResistorBlock::new);
    public static final VoxelShape SHAPE = Block.box(3, 3, 0, 13, 13, 16);

    private static final int[] VALUES = {1, 5, 10, 50, 100, 500, 1000, 1000000};

    private static final long DOUBLE_CLICK_WINDOW = 50L;

    public ResistorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new ResistorBlockEntity(blockPos, blockState);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if(level.isClientSide()) {
            return null;
        }

        return createTickerHelper(blockEntityType, ModBlockEntityTypes.RESISTOR_BLOCK_BE.get(), this::tick);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState blockState, Level level, BlockPos position,
                                                        Player player, BlockHitResult hit) {

        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;

        if (!level.isClientSide()) {
            BlockEntity maybeEntity = level.getBlockEntity(position);
            if (maybeEntity instanceof ResistorBlockEntity resistorEntity) {
                if (player.isShiftKeyDown()) {
                    cycleResistance(resistorEntity, -1);
                    sendStatus(player, "statusbar.powersim.resistor_set", resistorEntity.getResistance());
                    return InteractionResult.CONSUME;
                }

                long now = level.getGameTime();

                if (now - resistorEntity.getLastClickTick() > DOUBLE_CLICK_WINDOW) {
                    resistorEntity.setLastClickTick(now);
                    sendStatus(player, "statusbar.powersim.resistor_value", resistorEntity.getResistance());
                } else {
                    resistorEntity.setLastClickTick(now);
                    cycleResistance(resistorEntity, +1);
                    sendStatus(player, "statusbar.powersim.resistor_set", resistorEntity.getResistance());
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static void cycleResistance(ResistorBlockEntity entity, int delta) {
        int current = entity.getResistance();
        int index = 0;
        for (int i = 0; i < VALUES.length; i++) if (VALUES[i] == current) { index = i; break; }
        index = (index + delta + VALUES.length) % VALUES.length;
        entity.setResistanceFromClient(VALUES[index]);
    }

    private static void sendStatus(Player player, String langKey, int value) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.translatable(langKey, value), true);
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return SHAPE;
    }

    @Override
    protected void tick(Level level1, BlockPos pos, BlockState state1, AbstractMachineBlockEntity blockEntity) {
        blockEntity.tick(level1, pos, state1);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
