package com.teamofelectrorealism.electrorealism.block.connector;

import com.teamofelectrorealism.electrorealism.block.IPowerProvider;
import com.teamofelectrorealism.electrorealism.block.IPowerReceiver;
import com.teamofelectrorealism.electrorealism.network.Network;
import com.teamofelectrorealism.electrorealism.network.NetworkManager;
import com.teamofelectrorealism.electrorealism.power.ConnectionPoint;
import com.teamofelectrorealism.electrorealism.power.IWireNode;
import com.teamofelectrorealism.electrorealism.power.WireType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public abstract class AbstractConnectorBlockEntity extends BlockEntity implements IWireNode{

    private Network network;
    private final ConnectionPoint[] connectionPoints;
    private final IWireNode[] nodeCache;

    public AbstractConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);

        this.connectionPoints = new ConnectionPoint[getConnectionPointCount()];
        this.nodeCache = new IWireNode[getConnectionPointCount()];
    }

    // getters/setters
    public @Nullable IWireNode getWireNode(int index) {
        return IWireNode.getWireNodeFrom(index, this, this.connectionPoints, this.nodeCache, level);
    }

    public @Nullable ConnectionPoint getConnectionPoint(int index) {
        return this.connectionPoints[index];
    }

    public BlockPos getPos() {
        return getBlockPos();
    }

    @Override
    public void setConnectionPoint(int index, int connectingIndex, WireType wireType, BlockPos pos) {
        this.connectionPoints[index] = new ConnectionPoint(this, index, connectingIndex, wireType, pos);
        if (network != null) NetworkManager.instances.get(level).invalidateNetwork(network);
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    @Override
    public BlockEntity getMachine() {
        for (Direction facing: Direction.values()) {
            BlockPos neighborPos = this.getPos().offset(facing.getNormal());
            BlockEntity blockEntity = level.getBlockEntity(neighborPos);
            if (blockEntity instanceof IPowerReceiver || blockEntity instanceof IPowerProvider) {
                return blockEntity;
            }
        }
        return null;
    }

    // End getters/setters

    //Serializing
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        invalidateLocalNodes();
        invalidateNodeCache();
        ListTag connection_points = tag.getList(ConnectionPoint.CONNECTION_POINTS, ListTag.TAG_COMPOUND);
        connection_points.forEach(localNodeTag -> {
            ConnectionPoint connectionPoint = new ConnectionPoint(this, (CompoundTag) localNodeTag);
            this.connectionPoints[connectionPoint.getPointIndex()] = connectionPoint;
        });
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag connection_points = new ListTag();
        for(int i = 0; i < getConnectionPointCount(); i++) {
            ConnectionPoint connectionPoint = this.connectionPoints[i];
            if(connectionPoint == null) continue;
            CompoundTag localNodeTag = new CompoundTag();
            connectionPoint.write(localNodeTag);
            connection_points.add(localNodeTag);
        }
        tag.put(ConnectionPoint.CONNECTION_POINTS, connection_points);
        super.saveAdditional(tag, registries);
    }
    //End serializing


    @Override
    public void removeConnectionPoint(int index, boolean dropWire) {
        this.connectionPoints[index] = null;
        this.nodeCache[index] = null;

        invalidateNodeCache();
        if (network == null) network.invalidate();
    }

    //Helpers
    public void invalidateLocalNodes() {
        for(int i = 0; i < getConnectionPointCount(); i++)
            this.connectionPoints[i] = null;
    }

    public void invalidateNodeCache() {
        for(int i = 0; i < getConnectionPointCount(); i++)
            this.nodeCache[i] = null;
    }
}
