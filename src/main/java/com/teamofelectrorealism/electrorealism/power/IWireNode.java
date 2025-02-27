package com.teamofelectrorealism.electrorealism.power;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

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
}
