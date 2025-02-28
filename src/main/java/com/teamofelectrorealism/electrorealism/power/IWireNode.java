package com.teamofelectrorealism.electrorealism.power;

import com.teamofelectrorealism.electrorealism.block.connector.AbstractConnectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public interface IWireNode {
    default int getNodeCount() {
        return 1;
    };

    default boolean hasConnection(int index) {
        return getLocalNode(index) != null;
    }

    @Nullable
    LocalNode getLocalNode(int index);

    Vec3 getNodeOffset(int node);

    @Nullable
    IWireNode getWireNode(int index);

    static IWireNode getWireNode(Level world, BlockPos pos) {
        if(pos == null)
            return null;
        BlockEntity blockEntity = world.getBlockEntity(pos);
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
}
