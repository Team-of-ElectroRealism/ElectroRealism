package com.teamofelectrorealism.electrorealism.block.generator.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

public class FEGeneratorBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final int energyOutput;

    public FEGeneratorBlock(Properties properties, int energyOutput) {
        super(properties.randomTicks());
        this.energyOutput = energyOutput;
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
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            BlockPos targetPos = pos.relative(facing);

            if (level.isLoaded(targetPos)) {
                System.out.println("FEGenerator tick() called, facing: " + facing); // Print facing direction

                // Create an EnergyStorage instance
                IEnergyStorage energyStorage = new EnergyStorage(energyOutput);
                System.out.println("EnergyStorage created with capacity: " + energyOutput); // Print capacity

                // Get the IEnergyStorage from the level using Capabilities.EnergyStorage.BLOCK
                IEnergyStorage targetStorage = level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, facing.getOpposite());
                if (targetStorage!= null) {
                    System.out.println("Target IEnergyStorage found"); // Print if target found
                    if (targetStorage.canReceive()) {
                        // Transfer energy
                        int transferred = energyStorage.extractEnergy(targetStorage.receiveEnergy(energyOutput, true), false);
                        System.out.println("Energy transferred: " + transferred); // Print transferred amount
                        targetStorage.receiveEnergy(transferred, false);
                    } else {
                        System.out.println("Target cannot receive energy"); // Print if target cannot receive
                    }
                } else {
                    System.out.println("Target IEnergyStorage not found"); // Print if no target
                }
            }
        }
    }
}
