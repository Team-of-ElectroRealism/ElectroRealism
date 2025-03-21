package com.teamofelectrorealism.electrorealism.network;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.power.ConnectionPoint;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.*;

class Network {
    private final UUID networkId;
    private boolean isValid;

    private Set<BlockPos> memberPos;

    Network() {
        this.networkId = UUID.randomUUID();
        this.isValid = true;

        this.memberPos = new HashSet<>();
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

    Set<BlockPos> getMemberPos() {
        return memberPos;
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

    void registerBlockEntityPos(BlockPos blockPos) {
        memberPos.add(blockPos);
    }

    void registerAllBlockEntityPos(Set<BlockPos> posSet) {
        memberPos.addAll(posSet);
    }
}
