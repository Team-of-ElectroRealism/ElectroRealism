package com.teamofelectrorealism.electrorealism.block.components.ORGate;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class OrGateBlockEntity extends BlockEntity implements ILogicGate, IWireNode {

    private UUID networkId;
    private boolean outputState = false;
    private boolean[] inputStates = new boolean[2];
    private ConnectionPoint[] connectionPoints = new ConnectionPoint[2];


    public OrGateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.OR_GATE_BE.get(), pos, state);
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) return;

        boolean inputA = level.hasNeighborSignal(worldPosition.relative(Direction.WEST));
        boolean inputB = level.hasNeighborSignal(worldPosition.relative(Direction.EAST));

        updateInputState(new boolean[]{inputA, inputB});
    }

    @Override
    public void setPowered(boolean powered) {

    }

    @Override
    public void joinNetwork() {

    }

    @Override public UUID getNetworkId() { return networkId; }
    @Override public void setNetworkId(UUID id) { this.networkId = id; }
    @Override public BlockPos getPos() { return worldPosition; }

    @Override
    public void setConnectionPoint(int pointIndex, int connectingPointIndex, WireType wireType, BlockPos pos) {

    }

    @Override
    public ConnectorType getConnectorType() {
        return null;
    }

    @Override
    public int getMaxWireLength() {
        return 0;
    }

    @Override
    public @Nullable ConnectionPoint getConnectionPoint(int index) {
        return null;
    }

    @Override
    public Vec3 getConnectionPointOffset(int node) {
        return null;
    }

    @Override
    public @Nullable IWireNode getWireNode(int index) {
        return null;
    }

    @Override
    public void removeConnectionPoint(int index, boolean dropWire) {

    }

    @Override public LogicGateType getLogicType() { return LogicGateType.OR; }
    @Override public boolean getOutputState() { return outputState; }

    @Override
    public void updateInputState(boolean[] inputs) {
        boolean oldOutput = outputState;
        this.inputStates = inputs;
        this.outputState = getLogicType().compute(inputs);
        propagateOutputToWires();

        if (oldOutput != this.outputState) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    private void propagateOutputToWires() {
        if (level == null || level.isClientSide) return;

        for (ConnectionPoint cp : connectionPoints) {
            if (cp == null) continue;

            BlockEntity target = level.getBlockEntity(cp.getPos());
            if (target instanceof CopperWireBlockEntity wire) {
                wire.propagateSignal(outputState); // sends power to the wire
            }
        }
    }

}
