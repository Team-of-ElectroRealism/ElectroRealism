package com.teamofelectrorealism.electrorealism.block.components.CopperWire;

import com.teamofelectrorealism.electrorealism.block.connector.ConnectorType;
import com.teamofelectrorealism.electrorealism.network.INetworkMember;
import com.teamofelectrorealism.electrorealism.power.ConnectionPoint;
import com.teamofelectrorealism.electrorealism.power.IWireNode;
import com.teamofelectrorealism.electrorealism.power.WireType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;
import java.util.UUID;

public class CopperWireBlockEntity extends BlockEntity implements IWireNode, INetworkMember {
    private UUID networkId = null;
    private ConnectionPoint[] connectionPoints = new ConnectionPoint[2];
    private boolean powered = false;
    public boolean externallyPowered = false;


    public CopperWireBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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
    public int getConnectionPointCount() {
        return 2;
    }

    @Override
    public ConnectionPoint getConnectionPoint(int index) {
        return (index >= 0 && index < connectionPoints.length) ? connectionPoints[index] : null;
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
    public ConnectorType getConnectorType() {
        return ConnectorType.Small;
    }

    @Override
    public int getMaxWireLength() {
        return 16;
    }

    @Override
    public Vec3 getConnectionPointOffset(int index) {
        return (index == 0) ? new Vec3(-0.25, 0.5, 0) : new Vec3(0.25, 0.5, 0);
    }

    @Override
    @Nullable
    public IWireNode getWireNode(int index) {
        return null; // not needed yet
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide) {
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
        }
    }

    @Override
    public BlockPos getPos() {
        return this.worldPosition;
    }

    public boolean isPowered() {
        return powered;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CopperWireBlockEntity wire) {
        if (level.isClientSide) return;

        if (wire.externallyPowered) {
            wire.propagateSignal(true);
        }

        if (wire.isPowered()) {
            IWireNode.deliverVoltageToAdjacentMachines(level, pos, 230);
        }
    }

    public void setPowered(boolean powered) {

        if (this.powered != powered) {
            this.powered = powered;
            LOGGER.info("SET POWERED called on {} with value {}", worldPosition, powered);

            // Only deliver voltage if it came from an external source
            if (powered && externallyPowered) {
                LOGGER.warn("Wire at {} was set to powered WITHOUT external source!", worldPosition, new RuntimeException("Power trace"));
                IWireNode.deliverVoltageToAdjacentMachines(getLevel(), getPos(), 230);
            }

            setChanged();

            if (level != null && !level.isClientSide) {
                LOGGER.info("Wire at {} is now {}", worldPosition, powered ? "ON" : "OFF");
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    @Override
    public void joinNetwork() {

    }

    @Override
    public int getAvailableNode(Vec3 clickLocation) {
        return !hasConnectionAt(0) ? 0 : !hasConnectionAt(1) ? 1 : -1;
    }

    public boolean hasConnectionAt(int index) {
        return connectionPoints[index] != null;
    }

    @Override
    public boolean hasAnyConnection() {
        return connectionPoints[0] != null || connectionPoints[1] != null;
    }

    public void propagateSignal(boolean incomingPower) {
        propagateSignalRecursive(level, worldPosition, incomingPower, new HashSet<>());
    }

    public static void propagateSignalRecursive(Level level, BlockPos origin, boolean incomingPower, Set<BlockPos> visited) {
        if (level == null || level.isClientSide || origin == null) return;
        if (!visited.add(origin)) return; // already visited

        BlockEntity be = level.getBlockEntity(origin);
        if (!(be instanceof IWireNode node)) return;

        node.setPowered(incomingPower);
        node.joinNetwork();

        for (int i = 0; i < node.getConnectionPointCount(); i++) {
            ConnectionPoint cp = node.getConnectionPoint(i);
            if (cp == null) continue;

            BlockPos targetPos = cp.getPos();
            propagateSignalRecursive(level, targetPos, incomingPower, visited);
        }
    }
}
