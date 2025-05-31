package com.teamofpowersim.powersim.block.connector;

import com.mojang.logging.LogUtils;
import com.teamofpowersim.powersim.PowerSim;
import com.teamofpowersim.powersim.network.INetworkMember;
import com.teamofpowersim.powersim.network.NetworkManager;
import com.teamofpowersim.powersim.power.ConnectionPoint;
import com.teamofpowersim.powersim.power.IWireNode;
import com.teamofpowersim.powersim.power.WireType;
import com.teamofpowersim.powersim.simulation.ISimulatable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.*;

public abstract class AbstractConnectorBlockEntity extends BlockEntity implements IWireNode, INetworkMember, ISimulatable {
    private static final Logger LOGGER = LogUtils.getLogger();

    private UUID networkId;
    private final ConnectionPoint[] connectionPoints;
    private final IWireNode[] iWireNodeCache;

    private double simVoltage;
    private double simCurrent;

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

    public abstract ConnectorPolarity getTerminalType(int index);

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
    public void applySimulation(double voltage, double current) {
        this.simVoltage = voltage;
        this.simCurrent = current;
    }

    public double getSimVoltage() { return simVoltage; }
    public double getSimCurrent() { return simCurrent; }

    @Override
    public UUID getNetworkId() {
        return this.networkId;
    }

    @Override
    public void setNetworkId(UUID newNetworkId) {
        if (!Objects.equals(this.networkId, newNetworkId)) {
            LOGGER.trace("Connector at {} changing network ID from {} to {}", getPos(), this.networkId, newNetworkId);
            this.networkId = newNetworkId;

            if (this.networkId != null && this.level != null && !this.level.isClientSide()) {
                PowerSim.NETWORK_MANAGER.registerINetworkMemberInNetwork(this.networkId, this);
            }

            setChanged();
        } else {
            LOGGER.trace("Connector at {} setNetworkId called with same ID {}", getPos(), newNetworkId);
        }
    }

    @Override
    public void setConnectionPoint(int pointIndex, int connectingPointIndex, WireType wireType, BlockPos pos) {
        this.connectionPoints[pointIndex] = new ConnectionPoint(this, pointIndex, connectingPointIndex, wireType, pos);
        setChanged();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.networkId != null && this.level != null && !this.level.isClientSide()) {
            LOGGER.trace("Connector at {} loaded with ID {}. Registering.", getPos(), this.networkId);
            PowerSim.NETWORK_MANAGER.registerINetworkMemberInNetwork(this.networkId, this);
        } else if (this.level != null && !this.level.isClientSide()){
            LOGGER.trace("Connector at {} loaded without network ID.", getPos());
        }
    }

    /**
     * Removes the block entity from the network when it is removed from the world.
     * Also calls the super method to perform default removal actions.
     */
    @Override
    public void setRemoved() {
        if (this.level != null && !this.level.isClientSide) {
            NetworkManager networkManager = PowerSim.NETWORK_MANAGER;
            networkManager.removeNetworkMember(this);
        }
        super.setRemoved();
    }

    /**
     * Removes a connection point at the specified index.
     *
     * @param index    The index of the connection point to remove.
     * @param dropWire Whether to drop the wire connected to this point.
     */
    @Override
    public void removeConnectionPoint(int index, boolean dropWire) {
        if (this.connectionPoints[index] != null) {
            this.connectionPoints[index] = null;
            if (index < this.iWireNodeCache.length) {
                this.iWireNodeCache[index] = null;
            }
            setChanged();

            if (this.level != null && !this.level.isClientSide()) {
                this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        BlockState oldState = this.getBlockState();
        loadAdditional(tag, registries);
        if (this.level != null && this.level.isClientSide) {
            Minecraft mc = Minecraft.getInstance();
            mc.levelRenderer.setBlockDirty(getBlockPos(), oldState, getBlockState());
        }
    }

    @Override
    public @org.jetbrains.annotations.Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    //Serializing
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(NETWORK_KEY, CompoundTag.TAG_INT_ARRAY)) { // Check type for UUID
            this.networkId = tag.getUUID(NETWORK_KEY);
        } else {
            this.networkId = null;
        }
        Arrays.fill(this.connectionPoints, null);
        Arrays.fill(this.iWireNodeCache, null);
        ListTag connection_points = tag.getList(ConnectionPoint.CONNECTION_POINTS, ListTag.TAG_COMPOUND);
        connection_points.forEach(connectionPointTag -> {
            ConnectionPoint connectionPoint = new ConnectionPoint(this, (CompoundTag) connectionPointTag);
            this.connectionPoints[connectionPoint.getConnectionPointIndex()] = connectionPoint;
        });
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.networkId != null) {
            tag.putUUID(NETWORK_KEY, this.networkId);
        }
        ListTag connection_points = new ListTag();
        for(int i = 0; i < getConnectionPointCount(); i++) {
            ConnectionPoint connectionPoint = this.connectionPoints[i];
            if (connectionPoint == null) continue;
            CompoundTag localNodeTag = new CompoundTag();
            connectionPoint.write(localNodeTag);
            connection_points.add(localNodeTag);
        }
    }
    //End serializing

    //Helpers

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
