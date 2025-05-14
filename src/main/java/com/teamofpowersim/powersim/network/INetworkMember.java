package com.teamofpowersim.powersim.network;

import net.minecraft.core.BlockPos;

import java.util.UUID;

public interface INetworkMember {
    UUID getNetworkId();
    void setNetworkId(UUID networkId);
    BlockPos getPos();
}
