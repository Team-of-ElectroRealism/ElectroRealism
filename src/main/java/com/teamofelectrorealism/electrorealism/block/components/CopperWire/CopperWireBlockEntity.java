package com.teamofelectrorealism.electrorealism.block.components.CopperWire;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.block.connector.ConnectorType;
import com.teamofelectrorealism.electrorealism.network.INetworkMember;
import com.teamofelectrorealism.electrorealism.power.ConnectionPoint;
import com.teamofelectrorealism.electrorealism.power.IWireNode;
import com.teamofelectrorealism.electrorealism.power.WireType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

public class CopperWireBlockEntity extends BlockEntity implements IWireNode, INetworkMember {
    private UUID networkId = null;
    private ConnectionPoint[] connectionPoints = new ConnectionPoint[2];

    public CopperWireBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public UUID getNetworkId() {
        return networkId;
    }

    @Override
    public void setNetworkId(UUID id) {
        this.networkId = id;
    }
    @Override
    public int getConnectionPointCount() {
        return 2;
    }

    @Override
    public ConnectionPoint getConnectionPoint(int index) {
        return (index >= 0 && index < connectionPoints.length) ? connectionPoints[index] : null;
    }

    @Override
    public void setConnectionPoint(int pointIndex, int connectingPointIndex, WireType wireType, BlockPos targetPos) {
        connectionPoints[pointIndex] = new ConnectionPoint(this, pointIndex, connectingPointIndex, wireType, targetPos);
    }

    @Override
    public void removeConnectionPoint(int index, boolean dropWire) {
        connectionPoints[index] = null;
    }

    @Override
    public ConnectorType getConnectorType() {
        return ConnectorType.Small;
    }

    @Override
    public int getMaxWireLength() {
        return 16; // adjust for gameplay
    }

    @Override
    public Vec3 getConnectionPointOffset(int index) {
        return (index == 0) ? new Vec3(-0.25, 0.5, 0) : new Vec3(0.25, 0.5, 0);
    }

    @Override
    @Nullable
    public IWireNode getWireNode(int index) {
        return null; // not needed yet
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide) {
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
        }
    }

    @Override
    public BlockPos getPos() {
        return this.worldPosition;
    }

}
