package com.teamofelectrorealism.electrorealism.block.connector;

import com.llamalad7.mixinextras.sugar.Local;
import com.teamofelectrorealism.electrorealism.network.PowerNetwork;
import com.teamofelectrorealism.electrorealism.power.IWireNode;
import com.teamofelectrorealism.electrorealism.power.LocalNode;
import com.teamofelectrorealism.electrorealism.power.WireType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public abstract class AbstractConnectorBlockEntity extends BlockEntity implements IWireNode{
    private PowerNetwork powerNetwork;
    private final LocalNode[] localNodes;
    private final IWireNode[] nodeCache;

    public AbstractConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);

        this.localNodes = new LocalNode[getNodeCount()];
        this.nodeCache = new IWireNode[getNodeCount()];
    }

    // getters/setters
    public @Nullable IWireNode getWireNode(int index) {
        return IWireNode.getWireNodeFrom(index, this, this.localNodes, this.nodeCache, level);
    }

    public @Nullable LocalNode getLocalNode(int index) {
        return this.localNodes[index];
    }

    public BlockPos getPos() {
        return getBlockPos();
    }

    public PowerNetwork getNetwork(int node) {
        return powerNetwork;
    }

    public void setNetwork(int node, PowerNetwork powerNetwork) {
        this.powerNetwork = powerNetwork;
    }

    public void setNode(int index, int other, BlockPos pos, WireType type) {
        this.localNodes[index] = new LocalNode(this, index, other, type, pos);
        if (powerNetwork != null) powerNetwork.invalidate();
    }
    // End getters/setters

    //Serializing
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        invalidateLocalNodes();
        invalidateNodeCache();
        ListTag nodes = tag.getList(LocalNode.NODES, ListTag.TAG_COMPOUND);
        nodes.forEach(localNodeTag -> {
            LocalNode localNode = new LocalNode(this, (CompoundTag) localNodeTag);
            this.localNodes[localNode.getIndex()] = localNode;
        });
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag nodes = new ListTag();
        for(int i = 0; i < getNodeCount(); i++) {
            LocalNode localNode = this.localNodes[i];
            if(localNode == null) continue;
            CompoundTag localNodeTag = new CompoundTag();
            localNode.write(localNodeTag);
            nodes.add(localNodeTag);
        }
        tag.put(LocalNode.NODES, nodes);
        super.saveAdditional(tag, registries);
    }
    //End serializing

    //Helpers
    public void invalidateLocalNodes() {
        for(int i = 0; i < getNodeCount(); i++)
            this.localNodes[i] = null;
    }

    public void invalidateNodeCache() {
        for(int i = 0; i < getNodeCount(); i++)
            this.nodeCache[i] = null;
    }
}
