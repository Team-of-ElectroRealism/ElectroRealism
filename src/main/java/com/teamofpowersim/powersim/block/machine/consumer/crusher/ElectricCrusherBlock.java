package com.teamofpowersim.powersim.block.machine.consumer.crusher;

import com.mojang.serialization.MapCodec;
import com.teamofpowersim.powersim.block.ModBlockEntityTypes;
import com.teamofpowersim.powersim.block.machine.AbstractMachineBlockEntity;
import com.teamofpowersim.powersim.block.machine.consumer.AbstractPowerConsumerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class ElectricCrusherBlock extends AbstractPowerConsumerBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty CRUSHING = BlockStateProperties.CRAFTING;
    public static final MapCodec<ElectricCrusherBlock> CODEC = simpleCodec(ElectricCrusherBlock::new);

    public ElectricCrusherBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void tick(Level level1, BlockPos pos, BlockState state1, AbstractMachineBlockEntity blockEntity) {
        blockEntity.tick(level1, pos, state1);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(CRUSHING)) {
            double xPos = pos.getX() + 0.5;
            double yPos = pos.getY() + 1;
            double zPos = pos.getZ() + 0.5;
            double offset = random.nextDouble() * 0.6 - 0.3;

            level.addParticle(ParticleTypes.CRIT, xPos + offset, yPos, zPos + offset, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(CRUSHING, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CRUSHING);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new ElectricCrusherBlockEntity(blockPos, blockState);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ElectricCrusherBlockEntity electricCrusherBlockEntity) {
                electricCrusherBlockEntity.drops();
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ElectricCrusherBlockEntity electricCrusherBlockEntity) {
                player.openMenu(new SimpleMenuProvider(electricCrusherBlockEntity, Component.literal("Electric Crusher")), pos);
            } else {
                throw new IllegalStateException("Our Container provider is missing!");
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if(level.isClientSide()) {
            return null;
        }

        return createTickerHelper(blockEntityType, ModBlockEntityTypes.ELECTRIC_CRUSHER_BE.get(), this::tick);
    }
}
