package com.teamofpowersim.powersim.block.machine.consumer.lamp;

import com.mojang.serialization.MapCodec;
import com.teamofpowersim.powersim.block.ModBlockEntityTypes;
import com.teamofpowersim.powersim.block.machine.AbstractMachineBlockEntity;
import com.teamofpowersim.powersim.block.machine.consumer.AbstractPowerConsumerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

public class ElectricLampBlock extends AbstractPowerConsumerBlock {
    public static final IntegerProperty LIGHT_LEVEL = IntegerProperty.create("light_level", 0, 15);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;   // keep for textures

    public static final MapCodec<ElectricLampBlock> CODEC = simpleCodec(ElectricLampBlock::new);

    public ElectricLampBlock(Properties properties) {
        super(properties.lightLevel(s -> 0));         // let us supply the value manually
        this.registerDefaultState(this.defaultBlockState()
                .setValue(LIT, false)
                .setValue(LIGHT_LEVEL, 0));
    }

    public int getLightEmission(BlockState state, BlockGetter world, BlockPos pos) {
        return state.getValue(LIGHT_LEVEL);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT, LIGHT_LEVEL);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new ElectricLampBlockEntity(blockPos, blockState);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(LIT, false);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if(level.isClientSide()) {
            return null;
        }

        return createTickerHelper(blockEntityType, ModBlockEntityTypes.ELECTRIC_LAMP_BE.get(), this::tick);
    }

    @Override
    protected void tick(Level level1, BlockPos pos, BlockState state1, AbstractMachineBlockEntity blockEntity) {
        blockEntity.tick(level1, pos, state1);
    }

    /**
     * Adds glow particles around the block if it is lit, simulating light emission.
     * Particles are spawned on faces that are not obstructed by solid blocks.
     *
     * @param state The current block state.
     * @param level The level the block is in.
     * @param pos The position of the block.
     * @param random A random source for particle positioning.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            double offsetToFace = 0.5625;

            for (Direction direction : Direction.values()) {
                BlockPos adjacentPos = pos.relative(direction);

                if (!level.getBlockState(adjacentPos).isSolidRender(level, adjacentPos)) {
                    Direction.Axis axis = direction.getAxis();
                    double particleRelX = (axis == Direction.Axis.X)
                            ? 0.5 + offsetToFace * direction.getStepX()
                            : random.nextDouble();
                    double particleRelY = (axis == Direction.Axis.Y)
                            ? 0.5 + offsetToFace * direction.getStepY()
                            : random.nextDouble();
                    double particleRelZ = (axis == Direction.Axis.Z)
                            ? 0.5 + offsetToFace * direction.getStepZ()
                            : random.nextDouble();

                    double worldX = (double)pos.getX() + particleRelX;
                    double worldY = (double)pos.getY() + particleRelY;
                    double worldZ = (double)pos.getZ() + particleRelZ;

                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, worldX, worldY, worldZ, 0.0, 0.0, 0.0);
                }
            }
        }
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
