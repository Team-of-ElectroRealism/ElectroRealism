package com.teamofelectrorealism.electrorealism.block.machine;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.network.INetworkMember;
import com.teamofelectrorealism.electrorealism.network.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public abstract class AbstractMachineBlockEntity extends BlockEntity implements INetworkMember {
    private UUID networkId;

    private static final String NETWORK_KEY = "networkid";

    public AbstractMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public abstract void tick(Level level, BlockPos pos, BlockState state);


    public abstract boolean isFaceAllowed(BlockState state, Direction faceAccessed);

    // INetworkMember methods
    @Override
    public UUID getNetworkId() {
        return this.networkId;
    }

    @Override
    public void setNetworkId(UUID networkId) {
        this.networkId = networkId;
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    /**
     * Called when the BlockEntity is being removed from the level.
     * Notifies the NetworkManager to remove this machine from its network.
     */
    @Override
    public void setRemoved() {
        if (this.level != null && !this.level.isClientSide()) {
            NetworkManager networkManager = ElectroRealism.NETWORK_MANAGER;
            networkManager.removeNetworkMember(this);
        }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (this.networkId != null) tag.putUUID(NETWORK_KEY, this.networkId);
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(NETWORK_KEY)) this.networkId = tag.getUUID(NETWORK_KEY);
    }
}
