package com.teamofelectrorealism.electrorealism.block.components.ANDGate;

import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.logic.ILogicGate;
import com.teamofelectrorealism.electrorealism.logic.LogicGateType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import java.util.UUID;

public class AndGateBlockEntity extends BlockEntity implements ILogicGate {  // <-- add EntityBlock

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public AndGateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.AND_GATE_BE.get(), pos, state);
    }

    private UUID networkId = null;
    private boolean outputState = false;
    private boolean[] inputStates = new boolean[2]; // 2 inputs for AND

    @Override
    public UUID getNetworkId() {
        return networkId;
    }

    @Override
    public void setNetworkId(UUID networkId) {
        this.networkId = networkId;
    }

    @Override
    public BlockPos getPos() {
        return this.worldPosition;
    }

    @Override
    public LogicGateType getLogicType() {
        return LogicGateType.AND;
    }

    @Override
    public boolean getOutputState() {
        return outputState;
    }

    @Override
    public void updateInputState(boolean[] inputs) {
        this.inputStates = inputs;
        this.outputState = LogicGateType.AND.compute(inputs);
        // TODO: Update block state or trigger neighbor notification
    }

}
