package com.teamofelectrorealism.electrorealism.power;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.block.connector.ConnectorType;
import com.teamofelectrorealism.electrorealism.network.NetworkManager;
import com.teamofelectrorealism.electrorealism.network.NetworkSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public interface IWireNode {
    static WireConnectResult disconnect(Level level, BlockPos pos1, BlockPos pos2) {
        BlockEntity blockEntity1 = level.getBlockEntity(pos1);
        BlockEntity blockEntity2 = level.getBlockEntity(pos2);
        if (blockEntity1 == null || blockEntity2 == null || blockEntity1 == blockEntity2) {
            return WireConnectResult.INVALID;
        }
        if (!(blockEntity1 instanceof IWireNode iWireNode1) || !(blockEntity2 instanceof IWireNode iWireNode2)) {
            return WireConnectResult.INVALID;
        }
        if (!iWireNode1.hasConnectionTo(pos2)) {
            return WireConnectResult.NO_CONNECTION;
        }

        ConnectionPoint connectionPoint1 = iWireNode1.getConnectionTo(pos2);
        ConnectionPoint connectionPoint2 = iWireNode2.getConnectionTo(pos1);
        if (connectionPoint1 == null || connectionPoint2 == null) {
            return WireConnectResult.NO_CONNECTION;
        }
        iWireNode1.removeConnectionPoint(connectionPoint1);
        iWireNode2.removeConnectionPoint(connectionPoint2);
        return WireConnectResult.REMOVED;
    }

    static WireConnectResult connect(Level level, BlockPos pos1, int connectionPointIndex1, BlockPos pos2, int connectionPointIndex2, WireType wireType) {
        BlockEntity blockEntity1 = level.getBlockEntity(pos1);
        BlockEntity blockEntity2 = level.getBlockEntity(pos2);
        if (blockEntity1 == null || blockEntity2 == null || blockEntity1 == blockEntity2) {
            return WireConnectResult.INVALID;
        }
        if (!(blockEntity1 instanceof IWireNode iWireNode1) || !(blockEntity2 instanceof IWireNode iWireNode2)) {
            return WireConnectResult.INVALID;
        }
        if (connectionPointIndex1 < 0 || connectionPointIndex2 < 0) {
            return WireConnectResult.COUNT;
        }
        
        int maxLength = Math.min(iWireNode1.getMaxWireLength(), iWireNode2.getMaxWireLength());
        if (pos1.distSqr(pos2) > maxLength * maxLength) return WireConnectResult.LONG;
        if (iWireNode1.hasConnectionTo(pos2)) return WireConnectResult.EXISTS;
        if (iWireNode1.getConnectorType() == ConnectorType.Large && iWireNode2.getConnectorType() == ConnectorType.Large) {
            if (wireType == WireType.COPPER) return WireConnectResult.REQUIRES_HIGH_CURRENT;
        }

        iWireNode1.setConnectionPoint(connectionPointIndex1, connectionPointIndex2, wireType, iWireNode2.getPos());
        iWireNode2.setConnectionPoint(connectionPointIndex2, connectionPointIndex1, wireType, iWireNode1.getPos());

        System.out.println(level);
        System.out.println(level.dimension());

        ElectroRealism.NETWORK_MANAGER.createConnection(
                level,
                iWireNode1.getConnectionPoint(connectionPointIndex1),
                iWireNode2.getConnectionPoint(connectionPointIndex2)
        );
        return WireConnectResult.getLink(iWireNode2.isNodeInput(connectionPointIndex2), iWireNode2.isNodeOutput(connectionPointIndex2));
    }

    default boolean isNodeInput(int index) {
        return true;
    }

    default boolean isNodeOutput(int node) {
        return true;
    }

    void setConnectionPoint(int pointIndex, int connectingPointIndex, WireType wireType, BlockPos pos);

    ConnectorType getConnectorType();

    int getMaxWireLength();

    default int getConnectionPointCount() {
        return 1;
    };

    default boolean hasConnection(int index) {
        return getConnectionPoint(index) != null;
    }

    default boolean hasConnectionTo(BlockPos pos) {
        if (pos == null) return false;
        for (int i = 0; i < getConnectionPointCount(); i++) {
            ConnectionPoint connectionPoint = getConnectionPoint(i);
            if (connectionPoint == null) continue;
            if (connectionPoint.getPos().equals(pos)) return true;
        }
        return false;
    }

    @Nullable
    ConnectionPoint getConnectionPoint(int index);

    Vec3 getConnectionPointOffset(int node);

    @Nullable
    IWireNode getWireNode(int index);

    static IWireNode getWireNode(Level level, BlockPos pos) {
        if(pos == null)
            return null;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if(blockEntity == null)
            return null;
        if(!(blockEntity instanceof IWireNode))
            return null;
        return (IWireNode) blockEntity;
    }

    @Nullable
    static IWireNode getWireNodeFrom(int index, IWireNode wireNode, ConnectionPoint[] connectionPoints, IWireNode[] nodeCache, Level level) {
        if (!wireNode.hasConnection(index)) return null;
        // Cache the node if it isn't already.
        if (nodeCache[index] == null)
            nodeCache[index] = IWireNode.getWireNode(level, connectionPoints[index].getPos());
        // If the node is still null, remove it.
        if (nodeCache[index] == null) wireNode.removeConnectionPoint(index);
        return nodeCache[index];
    }

    // Remove Node methods
    void removeConnectionPoint(int index, boolean dropWire);

    default void removeConnectionPoint(int index) {
        removeConnectionPoint(index, false);
    }

    default void removeConnectionPoint(ConnectionPoint node, boolean dropWire) {
        removeConnectionPoint(node.getPointIndex(), dropWire);
    }

    default void removeConnectionPoint(@NotNull ConnectionPoint node) {
        removeConnectionPoint(node.getPointIndex());
    }
    // End remove Node methods

    default int getConnectingNodeIndex(int index) {
        ConnectionPoint node = getConnectionPoint(index);
        int connectingNodeIndex = (node == null) ? -1 : node.getConnectingPointIndex();
        return connectingNodeIndex;
    }

    @Nullable
    default BlockPos getNodePos(int index) {
        ConnectionPoint node = getConnectionPoint(index);
        BlockPos nodePos = (node == null) ? null : node.getPos();
        return nodePos;
    }

    BlockPos getPos();

    @Nullable
    default WireType getWireType(int index) {
        ConnectionPoint node = getConnectionPoint(index);
        WireType wireType = (node == null) ? null : node.getWireType();
        return wireType;
    }

    @Nullable
    static WireType getTypeOfConnection(Level world, BlockPos pos1, BlockPos pos2) {
        BlockEntity blockEntity = world.getBlockEntity(pos1);
        if (blockEntity == null) return null;
        if (!(blockEntity instanceof IWireNode wireNode)) return null;
        ConnectionPoint node = wireNode.getConnectionTo(pos2);
        if (node == null) return null;
        return node.getWireType();
    }

    @Nullable
    default ConnectionPoint getConnectionTo(BlockPos pos) {
        if (pos == null) return null;
        for (int i = 0; i < getConnectionPointCount(); i++) {
            ConnectionPoint node = getConnectionPoint(i);
            if (node == null) continue;
            if (node.getPos().equals(pos)) return node;
        }
        return null;
    }

    default int getAvailableNode(Vec3 pos) {
        return getAvailableNode();
    }

    default int getAvailableNode() {
        for (int i = 0; i < getConnectionPointCount(); i++) {
            if (hasConnection(i)) continue;
            return i;
        }
        return -1;
    }

    default boolean hasAnyConnection() {
        for (int i = 0; i < getConnectionPointCount(); i++) {
            if(hasConnection(i)) return true;
        }
        return false;
    }

    BlockEntity getMachine();
}
