package com.teamofpowersim.powersim.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ConnectionPoint {

    public static final String CONNECTION_POINTS = "connection_points";
    public static final String POINT_INDEX = "point_index";
    public static final String CONNECTING_POINT_INDEX = "connecting_point_index";
    public static final String WIRE_TYPE = "wire_type";
    public static final String X = "x";
    public static final String Y = "y";
    public static final String Z = "z";

    private final BlockEntity blockEntity;
    private final int pointIndex;
    private final int connectingPointIndex;
    private final WireType wireType;
    private final Vec3i relativePos;
    private boolean invalid = false;

    public ConnectionPoint(BlockEntity blockEntity, int pointIndex, int connectingPointIndex, WireType wireType, BlockPos blockPos) {
        this.blockEntity = blockEntity;
        this.pointIndex = pointIndex;
        this.connectingPointIndex = connectingPointIndex;
        this.wireType = wireType;
        this.relativePos = blockPos.subtract(blockEntity.getBlockPos());
    }

    public ConnectionPoint(BlockEntity blockEntity, CompoundTag compoundTag) {
        this.blockEntity = blockEntity;
        this.pointIndex = compoundTag.getInt(POINT_INDEX);
        this.connectingPointIndex = compoundTag.getInt(CONNECTING_POINT_INDEX);
        this.wireType = WireType.fromIndex(compoundTag.getInt(WIRE_TYPE));
        this.relativePos = new Vec3i(compoundTag.getInt(X), compoundTag.getInt(Y), compoundTag.getInt(Z));
    }

    public void write(CompoundTag compoundTag) {
        compoundTag.putInt(POINT_INDEX, this.pointIndex);
        compoundTag.putInt(CONNECTING_POINT_INDEX, this.connectingPointIndex);
        compoundTag.putInt(WIRE_TYPE, this.wireType.getID());
        compoundTag.putInt(X, this.relativePos.getX());
        compoundTag.putInt(Y, this.relativePos.getY());
        compoundTag.putInt(Z, this.relativePos.getZ());
    }

    public int getConnectionPointIndex() {
        return pointIndex;
    }

    public int getConnectingPointIndex() {
        return connectingPointIndex;
    }

    public WireType getWireType() {
        return wireType;
    }

    public Vec3i getRelativePos() {
        return relativePos;
    }

    public BlockPos getPos() {
        return blockEntity.getBlockPos().offset(this.relativePos);
    }

    public boolean isInvalid() {
        return invalid;
    }

    public void setInvalid() {
        this.invalid = true;
    }
}
