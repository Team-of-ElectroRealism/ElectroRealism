package com.teamofelectrorealism.electrorealism.block.machine.generator.test;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.block.IPowerProvider;
import com.teamofelectrorealism.electrorealism.block.IPowerReceiver;
import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.block.connector.ConnectorType;
import com.teamofelectrorealism.electrorealism.network.INetworkMember;
import com.teamofelectrorealism.electrorealism.power.ConnectionPoint;
import com.teamofelectrorealism.electrorealism.power.IWireNode;
import com.teamofelectrorealism.electrorealism.power.WireType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class VoltageSourceBlockEntity extends BlockEntity implements IPowerProvider, IWireNode, INetworkMember {
    private final int voltage = 230;
    private UUID networkId = null;
    private boolean lastPowered = false;

    private final ConnectionPoint[] connectionPoints = new ConnectionPoint[1];
    private final IWireNode[] nodeCache = new IWireNode[1];


    public VoltageSourceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.VOLTAGE_SOURCE_BE.get(), pos, state);
    }

    @Override
    public int getVoltage() {
        return voltage;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        boolean currentlyPowered = true; // Always true since it's a static voltage source
        if (!level.isClientSide && currentlyPowered != lastPowered) {
            lastPowered = currentlyPowered;
            ElectroRealism.NETWORK_MANAGER.propagateSignal(level, worldPosition);
            LOGGER.info("VoltageSource at {} triggered propagateSignal", worldPosition);
        }

        if (currentlyPowered) {
            this.transferVoltage(level, pos);
        }
    }

    private void transferVoltage(Level level, BlockPos pos) {
        for (Direction facing: Direction.values()) {
            BlockPos neighborPos = pos.offset(facing.getNormal());
            BlockEntity blockEntity = level.getBlockEntity(neighborPos);
            if (blockEntity instanceof IPowerReceiver) {
                IPowerReceiver receiver = (IPowerReceiver) blockEntity;

                receiver.receiveVoltage(voltage);
            }
        }
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
    public void setNetworkId(UUID id) {
        this.networkId = id;
    }

    @Override
    public ConnectorType getConnectorType() {
        return ConnectorType.Small; // or whatever suits your logic
    }

    @Override
    public int getMaxWireLength() {
        return 16; // or other length based on design
    }

    @Override
    public int getConnectionPointCount() {
        return 1;
    }

    @Override
    public ConnectionPoint getConnectionPoint(int index) {
        return connectionPoints[index];
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public void setConnectionPoint(int pointIndex, int connectingPointIndex, WireType wireType, BlockPos targetPos) {
        connectionPoints[pointIndex] = new ConnectionPoint(this, pointIndex, connectingPointIndex, wireType, targetPos);
    }

    @Override
    public void removeConnectionPoint(int index, boolean dropWire) {
        connectionPoints[index] = null;
    }

    @Override
    public Vec3 getConnectionPointOffset(int node) {
        return new Vec3(0.5, 0.5, 0.5); // center of block or adjust as needed
    }

    @Override
    public @Nullable IWireNode getWireNode(int index) {
        return IWireNode.getWireNodeFrom(index, this, connectionPoints, nodeCache, level);
    }
}
