package com.teamofelectrorealism.electrorealism.block.components.NOTGate;

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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class NotGateBlockEntity extends BlockEntity implements ILogicGate, IWireNode {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private UUID networkId = null;
    private boolean outputState = false;
    private boolean[] inputStates = new boolean[1];
    private ConnectionPoint[] connectionPoints = new ConnectionPoint[2];


    public NotGateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.NOT_GATE_BE.get(), pos, state);
    }

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

    @Override
    public LogicGateType getLogicType() {
        return LogicGateType.NOT;
    }

    @Override
    public boolean getOutputState() {
        return outputState;
    }

    @Override
    public void updateInputState(boolean[] inputs) {
        if (inputs.length != 1) return;

        boolean oldOutput = this.outputState;
        this.inputStates = inputs;
        this.outputState = getLogicType().compute(inputs);
        propagateOutputToWires();

        if (oldOutput != this.outputState) {
            // Notify neighbors when output changes
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) return;

        // Example: reads input from the WEST
        boolean input = level.hasNeighborSignal(worldPosition.relative(Direction.WEST));
        updateInputState(new boolean[]{input});

        System.out.println("NOT Gate output: " + getOutputState());
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
