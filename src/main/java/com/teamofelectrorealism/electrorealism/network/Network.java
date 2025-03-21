package com.teamofelectrorealism.electrorealism.network;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.power.IWireNode;

import java.util.*;

class Network {
    private final UUID networkId;
    private boolean isValid;

    private Set<IWireNode> iWireNodes;

    Network() {
        this.networkId = UUID.randomUUID();
        this.isValid = true;

        this.iWireNodes = new HashSet<>();
        ElectroRealism.NETWORK_MANAGER.addNetwork(this);
    }

    void setInvalid() {
        isValid = false;
    }

    boolean isValid() {
        return isValid;
    }

    UUID getNetworkId() {
        return networkId;
    }

    Set<IWireNode> getIWireNodes() {
        return iWireNodes;
    }

//    public CompoundTag write() {
//        CompoundTag tag = new CompoundTag();
//
//        tag.putUUID("network_id", networkId);
//        tag.putBoolean("isvalid", isValid);
//
//        return tag;
//    }
//
//    public static Network read(CompoundTag tag) {
//        Network network = new Network(tag.getUUID("network_id")); //todo hmgde
//
//        network.isValid = tag.getBoolean("isvalid");
//
//        return network;
//    }

    void tick() {
    }

    void registerIWireNode(IWireNode iWireNode) {
        iWireNodes.add(iWireNode);
    }

    void registerAllIWireNodes(Set<IWireNode> iWireNodes) {
        this.iWireNodes.addAll(iWireNodes);
    }
}
