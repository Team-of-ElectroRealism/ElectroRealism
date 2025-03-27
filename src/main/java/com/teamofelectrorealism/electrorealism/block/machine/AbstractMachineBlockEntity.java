package com.teamofelectrorealism.electrorealism.block.machine;

import com.teamofelectrorealism.electrorealism.network.INetworkMember;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public abstract class AbstractMachineBlockEntity extends BlockEntity implements INetworkMember {
    private UUID networkId;

    public AbstractMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public abstract void tick(Level level, BlockPos pos, BlockState state);

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
