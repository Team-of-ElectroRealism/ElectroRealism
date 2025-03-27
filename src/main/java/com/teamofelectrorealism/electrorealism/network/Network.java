package com.teamofelectrorealism.electrorealism.network;

import com.teamofelectrorealism.electrorealism.ElectroRealism;

import java.util.*;

class Network {
    private final UUID networkId;
    private boolean isValid;

    private Set<INetworkMember> networkMembers;

    Network() {
        this.networkId = UUID.randomUUID();
        this.isValid = true;

        this.networkMembers = new HashSet<>();
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

    Set<INetworkMember> getNetworkMembers() {
        return networkMembers;
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

    void registerINetworkMember(INetworkMember networkMember) {
        networkMembers.add(networkMember);
    }

    void registerAllINetworkMembers(Set<INetworkMember> networkMembers) {
        this.networkMembers.addAll(networkMembers);
    }
}
