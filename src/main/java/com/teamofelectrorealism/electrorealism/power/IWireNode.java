package com.teamofelectrorealism.electrorealism.power;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.block.connector.ConnectorType;
import com.teamofelectrorealism.electrorealism.network.INetworkMember;
import com.teamofelectrorealism.electrorealism.network.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.UUID;





public interface IWireNode {
    static final Logger LOGGER = LogUtils.getLogger();
    Level getLevelFromSelf();

    static WireConnectResult disconnect(Level level, BlockPos pos1, BlockPos pos2) {
        System.out.println("[WIRE] Disconnect called between " + pos1 + " and " + pos2);

        BlockEntity be1 = level.getBlockEntity(pos1);
        BlockEntity be2 = level.getBlockEntity(pos2);

        IWireNode node1 = (be1 instanceof IWireNode) ? (IWireNode) be1 : null;
        IWireNode node2 = (be2 instanceof IWireNode) ? (IWireNode) be2 : null;

        ConnectionPoint cp1 = (node1 != null) ? node1.getConnectionPointIn(pos2) : null;
        ConnectionPoint cp2 = (node2 != null) ? node2.getConnectionPointIn(pos1) : null;

        // Get wire type early in case one node is missing
        WireType wireType = (cp1 != null && node1 != null)
                ? node1.getWireType(cp1.getConnectionPointIndex())
                : (cp2 != null && node2 != null)
                ? node2.getWireType(cp2.getConnectionPointIndex())
                : null;

        // Remove connection on both ends if present
        if (node1 != null && cp1 != null) {
            node1.removeConnectionPoint(cp1.getConnectionPointIndex(), true); // drop wire
        }
        if (node2 != null && cp2 != null) {
            node2.removeConnectionPoint(cp2.getConnectionPointIndex(), true); // drop wire
        }

        // Drop wire at midpoint
        if (!level.isClientSide && wireType != null) {
            Vec3 dropPos = Vec3.atCenterOf(pos1).add(Vec3.atCenterOf(pos2)).scale(0.5);
            ItemStack droppedWire = wireType.getSourceDrop();
            level.addFreshEntity(new ItemEntity(level, dropPos.x, dropPos.y, dropPos.z, droppedWire));
        }

        // Remove node(s) from network
        if (!level.isClientSide) {
            BlockEntity sourceEntity = level.getBlockEntity(pos1);
            if (sourceEntity instanceof INetworkMember sourceMember) {
                ElectroRealism.NETWORK_MANAGER.removeNode(sourceMember);
            }
        }

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

        if (!level.isClientSide()) {
            NetworkManager networkManager = ElectroRealism.NETWORK_MANAGER;
            INetworkMember networkMember1 = getNetworkMemberFromBlockEntity(blockEntity1);
            INetworkMember networkMember2 = getNetworkMemberFromBlockEntity(blockEntity2);
            if (networkMember1 == null || networkMember2 == null) return WireConnectResult.ERROR;
            UUID networkId = networkManager.createOrMergeNetworks(networkMember1, networkMember2);

            networkMember1.setNetworkId(networkId);
            networkMember2.setNetworkId(networkId);

            networkManager.registerINetworkMemberInNetwork(networkId, networkMember1);
            networkManager.registerINetworkMemberInNetwork(networkId, networkMember2);
        }

        return WireConnectResult.getLink(iWireNode2.isConnectorInput(connectionPointIndex2), iWireNode2.isConnectorOutput(connectionPointIndex2));
    }

    static INetworkMember getNetworkMemberFromBlockEntity(BlockEntity blockEntity) {
        if (blockEntity == null) return null;
        if (!(blockEntity instanceof INetworkMember)) return null;
        return (INetworkMember) blockEntity;
    }

    BlockPos getPos();

    default boolean isConnectorInput(int index) {
        return true;
    }

    default boolean isConnectorOutput(int node) {
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
        LOGGER.info("getPos called, returning {}", getPos());
        return false;
    }

    @Nullable
    ConnectionPoint getConnectionPoint(int index);

    Vec3 getConnectionPointOffset(int node);

    /**
     * Get the {@link IWireNode} at the given index.
     *
     * @param   index
     *          The index of the node.
     *
     * @return  The {@link IWireNode} at the given index, or null if the node
     *          doesn't exist.
     */
    @Nullable
    IWireNode getWireNode(int index);

    /**
     * Used by {@link IWireNode#getWireNode(int)} to get a cached
     * {@link IWireNode}.
     */
    @Nullable
    static IWireNode getWireNodeFrom(int index, IWireNode iWireNode, ConnectionPoint[] connectionPoints, IWireNode[] nodeCache, Level level) {
        if (!iWireNode.hasConnection(index)) return null;
        // Cache the node if it isn't already.
        if (nodeCache[index] == null)
            nodeCache[index] = IWireNode.getWireNode(level, connectionPoints[index].getPos());
        // If the node is still null, remove it.
        if (nodeCache[index] == null) iWireNode.removeConnectionPoint(index);
        return nodeCache[index];
    }

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

    void removeConnectionPoint(int index, boolean dropWire);

    default void removeConnectionPoint(int index) {
        BlockPos thisPos = getPos();
        BlockPos otherPos = getConnectorPos(index);

        if (thisPos != null && otherPos != null) {
            IWireNode.disconnect(getLevelFromSelf(), thisPos, otherPos);
        }
    }


    default void removeConnectionPoint(ConnectionPoint connectionPoint, boolean dropWire) {
        removeConnectionPoint(connectionPoint.getConnectionPointIndex(), dropWire);
    }

    default void removeConnectionPoint(@NotNull ConnectionPoint connectionPoint) {
        removeConnectionPoint(connectionPoint.getConnectionPointIndex());
    }

    default int getConnectingConnectionPointIndex(int index) {
        ConnectionPoint connectionPoint = getConnectionPoint(index);
        int connectingNodeIndex = (connectionPoint == null) ? -1 : connectionPoint.getConnectingPointIndex();
        return connectingNodeIndex;
    }

    @Nullable
    default BlockPos getConnectorPos(int index) {
        ConnectionPoint connectionPoint = getConnectionPoint(index);
        BlockPos connectorPos = (connectionPoint == null) ? null : connectionPoint.getPos();
        return connectorPos;
    }

    @Nullable
    default WireType getWireType(int index) {
        ConnectionPoint connectionPoint = getConnectionPoint(index);
        WireType wireType = (connectionPoint == null) ? null : connectionPoint.getWireType();
        return wireType;
    }

    @Nullable
    static WireType getWireTypeOfConnection(Level world, BlockPos pos1, BlockPos pos2) {
        BlockEntity blockEntity = world.getBlockEntity(pos1);
        if (blockEntity == null) return null;
        if (!(blockEntity instanceof IWireNode wireNode)) return null;
        ConnectionPoint connectionPoint = wireNode.getConnectionPointIn(pos2);
        if (connectionPoint == null) return null;
        return connectionPoint.getWireType();
    }

    @Nullable
    default ConnectionPoint getConnectionPointIn(BlockPos pos) {
        if (pos == null) return null;
        for (int i = 0; i < getConnectionPointCount(); i++) {
            ConnectionPoint connectionPoint = getConnectionPoint(i);
            if (connectionPoint == null) continue;
            if (connectionPoint.getPos().equals(pos)) return connectionPoint;
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
}
