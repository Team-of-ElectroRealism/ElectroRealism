package com.teamofelectrorealism.electrorealism.power;

import com.teamofelectrorealism.electrorealism.block.connector.AbstractConnectorBlockEntity;
import com.teamofelectrorealism.electrorealism.block.connector.ConnectorType;
import com.teamofelectrorealism.electrorealism.network.NetworkManager;
import net.minecraft.core.BlockPos;
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

        LocalNode localNode1 = iWireNode1.getConnectionTo(pos2);
        LocalNode localNode2 = iWireNode2.getConnectionTo(pos1);
        if (localNode1 == null || localNode2 == null) {
            return WireConnectResult.NO_CONNECTION;
        }
        iWireNode1.removeNode(localNode1);
        iWireNode2.removeNode(localNode2);
        return WireConnectResult.REMOVED;
    }

    static WireConnectResult connect(Level level, BlockPos pos1, int node1, BlockPos pos2, int node2, WireType wireType) {
        BlockEntity blockEntity1 = level.getBlockEntity(pos1);
        BlockEntity blockEntity2 = level.getBlockEntity(pos2);
        if (blockEntity1 == null || blockEntity2 == null || blockEntity1 == blockEntity2) {
            return WireConnectResult.INVALID;
        }
        if (!(blockEntity1 instanceof IWireNode iWireNode1) || !(blockEntity2 instanceof IWireNode iWireNode2)) {
            return WireConnectResult.INVALID;
        }
        if (node1 < 0 || node2 < 0) {
            return WireConnectResult.COUNT;
        }
        
        int maxLength = Math.min(iWireNode1.getMaxWireLength(), iWireNode2.getMaxWireLength());
        if (pos1.distSqr(pos2) > maxLength * maxLength) return WireConnectResult.LONG;
        if (iWireNode1.hasConnectionTo(pos2)) return WireConnectResult.EXISTS;
        if (iWireNode1.getConnectorType() == ConnectorType.Large && iWireNode2.getConnectorType() == ConnectorType.Large) {
            if (wireType == WireType.COPPER) return WireConnectResult.REQUIRES_HIGH_CURRENT;
        }

        iWireNode1.setNode(node1, node2, iWireNode2.getPos(), wireType);
        iWireNode2.setNode(node2, node1, iWireNode1.getPos(), wireType);

        // Ensure NetworkManager is initialized
        if (!NetworkManager.instances.containsKey(level)) {
            new NetworkManager(level);
        }

        NetworkManager.instances.get(level).createConnection(
                level,
                iWireNode1.getLocalNode(node1),
                iWireNode2.getLocalNode(node2)
        );
        return WireConnectResult.getLink(iWireNode2.isNodeInput(node2), iWireNode2.isNodeOutput(node2));
    }

    default boolean isNodeInput(int index) {
        return true;
    }

    default boolean isNodeOutput(int node) {
        return true;
    }

    void setNode(int index, int connectingIndex, BlockPos pos, WireType wireType);

    ConnectorType getConnectorType();

    int getMaxWireLength();

    default int getNodeCount() {
        return 1;
    };

    default boolean hasConnection(int index) {
        return getLocalNode(index) != null;
    }

    default boolean hasConnectionTo(BlockPos pos) {
        if (pos == null) return false;
        for (int i = 0; i < getNodeCount(); i++) {
            LocalNode localNode = getLocalNode(i);
            if (localNode == null) continue;
            if (localNode.getPos().equals(pos)) return true;
        }
        return false;
    }

    @Nullable
    LocalNode getLocalNode(int index);

    Vec3 getNodeOffset(int node);

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
    static IWireNode getWireNodeFrom(int index, IWireNode wireNode, LocalNode[] localNodes, IWireNode[] nodeCache, Level level) {
        if (!wireNode.hasConnection(index)) return null;
        // Cache the node if it isn't already.
        if (nodeCache[index] == null)
            nodeCache[index] = IWireNode.getWireNode(level, localNodes[index].getPos());
        // If the node is still null, remove it.
        if (nodeCache[index] == null) wireNode.removeNode(index);
        return nodeCache[index];
    }

    // Remove Node methods
    void removeNode(int index, boolean dropWire);

    default void removeNode(int index) {
        removeNode(index, false);
    }

    default void removeNode(LocalNode node, boolean dropWire) {
        removeNode(node.getIndex(), dropWire);
    }

    default void removeNode(@NotNull LocalNode node) {
        removeNode(node.getIndex());
    }
    // End remove Node methods

    default int getConnectingNodeIndex(int index) {
        LocalNode node = getLocalNode(index);
        int connectingNodeIndex = (node == null) ? -1 : node.getConnectingIndex();
        return connectingNodeIndex;
    }

    @Nullable
    default BlockPos getNodePos(int index) {
        LocalNode node = getLocalNode(index);
        BlockPos nodePos = (node == null) ? null : node.getPos();
        return nodePos;
    }

    BlockPos getPos();

    @Nullable
    default WireType getNodeType(int index) {
        LocalNode node = getLocalNode(index);
        WireType nodeType = (node == null) ? null : node.getWireType();
        return nodeType;
    }

    @Nullable
    static WireType getTypeOfConnection(Level world, BlockPos pos1, BlockPos pos2) {
        BlockEntity blockEntity = world.getBlockEntity(pos1);
        if (blockEntity == null) return null;
        if (!(blockEntity instanceof IWireNode wireNode)) return null;
        LocalNode node = wireNode.getConnectionTo(pos2);
        if (node == null) return null;
        return node.getWireType();
    }

    @Nullable
    default LocalNode getConnectionTo(BlockPos pos) {
        if (pos == null) return null;
        for (int i = 0; i < getNodeCount(); i++) {
            LocalNode node = getLocalNode(i);
            if (node == null) continue;
            if (node.getPos().equals(pos)) return node;
        }
        return null;
    }

    default int getAvailableNode(Vec3 pos) {
        return getAvailableNode();
    }

    default int getAvailableNode() {
        for (int i = 0; i < getNodeCount(); i++) {
            if (hasConnection(i)) continue;
            return i;
        }
        return -1;
    }

    default boolean hasAnyConnection() {
        for (int i = 0; i < getNodeCount(); i++) {
            if(hasConnection(i)) return true;
        }
        return false;
    }

    BlockEntity getMachine();
}
