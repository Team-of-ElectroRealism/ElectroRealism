package com.teamofelectrorealism.electrorealism.block.connector;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.network.INetworkMember;
import com.teamofelectrorealism.electrorealism.network.NetworkManager;
import com.teamofelectrorealism.electrorealism.power.ConnectionPoint;
import com.teamofelectrorealism.electrorealism.power.IWireNode;
import com.teamofelectrorealism.electrorealism.power.WireType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public abstract class AbstractConnectorBlockEntity extends BlockEntity implements IWireNode, INetworkMember {

    private UUID networkId;
    private final ConnectionPoint[] connectionPoints;
    private final IWireNode[] iWireNodeCache;

    private final Set<ConnectionPoint> connectionPointCache = new HashSet<>();

    private static final String NETWORK_KEY = "networkid";

    public AbstractConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);

        this.connectionPoints = new ConnectionPoint[getConnectionPointCount()];
        this.iWireNodeCache = new IWireNode[getConnectionPointCount()];
    }

    @Nullable
    public INetworkMember findNetworkMember() {
        Level level = this.getLevel();
        if (level == null) return null;

        BlockState state = this.getBlockState();
        if (!(state.getBlock() instanceof AbstractConnectorBlock connectorBlock)) return null;

        Direction facing = state.getValue(connectorBlock.FACING);
        Direction oppositeFacing = facing.getOpposite();
        BlockPos targetPos = this.getBlockPos().relative(oppositeFacing);

        BlockEntity targetBlockEntity = level.getBlockEntity(targetPos);
        if (targetBlockEntity instanceof INetworkMember networkMember) {
            return networkMember;
        }

        return null;
    }

    public abstract TerminalType getTerminalType(int index);

    public @Nullable ConnectionPoint getConnectionPoint(int index) {
        return this.connectionPoints[index];
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public @Nullable IWireNode getWireNode(int index) {
        return IWireNode.getWireNodeFrom(index, this, this.connectionPoints, this.iWireNodeCache, level);
    }

    @Override
    public UUID getNetworkId() {
        return this.networkId;
    }

    @Override
    public void setNetworkId(UUID networkId) {
        if (this.networkId == null) {
            INetworkMember networkMember = findNetworkMember();
            if (networkMember != null) {
                UUID neighborNetworkId = networkMember.getNetworkId();
                if (neighborNetworkId != null) {
                    networkId = neighborNetworkId;
                } else {
                    if (networkId != null) {
                        networkMember.setNetworkId(networkId);
                        ElectroRealism.NETWORK_MANAGER.registerINetworkMemberInNetwork(networkId, networkMember);
                    }
                }
            }
        }

        if (networkId != null) {
            this.networkId = networkId;
            setChanged();
        }
    }

    @Override
    public void setConnectionPoint(int pointIndex, int connectingPointIndex, WireType wireType, BlockPos pos) {
        this.connectionPoints[pointIndex] = new ConnectionPoint(this, pointIndex, connectingPointIndex, wireType, pos);
        setChanged();

        // Invalidate network? //todo
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide && this.networkId == null) {
            INetworkMember adjacentMember = findNetworkMember();
            if (adjacentMember != null) {
                UUID adjacentMemberNetworkId = adjacentMember.getNetworkId();
                if (adjacentMemberNetworkId != null) {
                    this.networkId = adjacentMemberNetworkId;
                    ElectroRealism.NETWORK_MANAGER.registerINetworkMemberInNetwork(this.networkId, this);
                    setChanged();
                }
            }
        }
    }

    @Override
    public void setRemoved() {
        NetworkManager networkManager = ElectroRealism.NETWORK_MANAGER;
        for (int i = 0; connectionPoints.length > i; i++) {
            removeConnectionPoint(i, true);
            networkManager.removeConnectorFromNetwork(networkId, this, getPos());
        }
        super.setRemoved();
    }

    @Override
    public void removeConnectionPoint(int index, boolean dropWire) {
        this.connectionPoints[index] = null;
        setChanged();
    }

    //Serializing
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(NETWORK_KEY)) this.networkId = tag.getUUID(NETWORK_KEY);
        disconnectAllConnections();
        ListTag connection_points = tag.getList(ConnectionPoint.CONNECTION_POINTS, ListTag.TAG_COMPOUND);
        connection_points.forEach(localNodeTag -> {
            ConnectionPoint connectionPoint = new ConnectionPoint(this, (CompoundTag) localNodeTag);
            this.connectionPoints[connectionPoint.getConnectionPointIndex()] = connectionPoint;
        });
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (this.networkId != null) tag.putUUID(NETWORK_KEY, this.networkId);
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

    //Helpers
    public void disconnectAllConnections() {
        if (this.level == null || this.level.isClientSide()) return;

        for (int i = 0; i < getConnectionPointCount(); i++) {
            BlockPos other = getConnectorPos(i);
            if (other != null) {
                IWireNode.disconnect(this.level, this.getBlockPos(), other);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof INetworkMember other)) return false;
        return this.getPos().equals(other.getPos());
    }

    @Override
    public int hashCode() {
        return this.getPos().hashCode();
    }
}
