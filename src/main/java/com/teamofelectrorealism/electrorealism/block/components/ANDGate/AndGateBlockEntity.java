package com.teamofelectrorealism.electrorealism.block.components.ANDGate;

import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.block.components.CopperWire.CopperWireBlockEntity;
import com.teamofelectrorealism.electrorealism.block.connector.ConnectorType;
import com.teamofelectrorealism.electrorealism.logic.ILogicGate;
import com.teamofelectrorealism.electrorealism.logic.LogicGateType;
import com.teamofelectrorealism.electrorealism.power.ConnectionPoint;
import com.teamofelectrorealism.electrorealism.power.IWireNode;
import com.teamofelectrorealism.electrorealism.power.WireType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

public class AndGateBlockEntity extends BlockEntity implements ILogicGate, IWireNode {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public AndGateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.AND_GATE_BE.get(), pos, state);
    }

    private UUID networkId = null;
    private boolean outputState = false;
    private boolean[] inputStates = new boolean[2];
    private ConnectionPoint[] connectionPoints = new ConnectionPoint[2];


    @Override
    public void setPowered(boolean powered) {

    }

    @Override
    public void joinNetwork() {

    }

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
        boolean oldOutput = this.outputState;

        this.inputStates = inputs;
        this.outputState = LogicGateType.AND.compute(inputs);
        propagateOutputToWires();

        if (oldOutput != this.outputState) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) return;

        boolean inputA = level.hasNeighborSignal(worldPosition.relative(Direction.WEST));
        boolean inputB = level.hasNeighborSignal(worldPosition.relative(Direction.EAST));

        updateInputState(new boolean[]{inputA, inputB});

        System.out.println("AND Gate output: " + getOutputState());
    }

    @Override
    public int getConnectionPointCount() {
        return 2;
    }

    @Override
    @Nullable
    public ConnectionPoint getConnectionPoint(int index) {
        return index >= 0 && index < connectionPoints.length ? connectionPoints[index] : null;
    }

    @Override
    public void setConnectionPoint(int pointIndex, int connectingPointIndex, WireType wireType, BlockPos targetPos) {
        connectionPoints[pointIndex] = new ConnectionPoint(this, pointIndex, connectingPointIndex, wireType, targetPos);
    }

    @Override
    public void removeConnectionPoint(int index, boolean dropWire) {
        if (index >= 0 && index < connectionPoints.length) {
            connectionPoints[index] = null;
        }
    }

    @Override
    public ConnectorType getConnectorType() {
        return ConnectorType.Small;
    }

    @Override
    public int getMaxWireLength() {
        return 10;
    }

    @Override
    public Vec3 getConnectionPointOffset(int index) {
        if (index == 0) return new Vec3(-0.25, 0.5, 0);
        if (index == 1) return new Vec3(0.25, 0.5, 0);
        return Vec3.ZERO;
    }

    @Override
    @Nullable
    public IWireNode getWireNode(int index) {
        return null;
    }

    private void propagateOutputToWires() {
        if (level == null || level.isClientSide) return;

        for (ConnectionPoint cp : connectionPoints) {
            if (cp == null) continue;

            BlockEntity target = level.getBlockEntity(cp.getPos());
            if (target instanceof CopperWireBlockEntity wire) {
                wire.propagateSignal(outputState);
            }
        }
    }

}
