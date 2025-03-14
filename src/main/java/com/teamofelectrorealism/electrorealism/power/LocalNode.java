package com.teamofelectrorealism.electrorealism.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class LocalNode {

    public static final String NODES = "nodes";
    public static final String ID = "id";
    public static final String CONNECTING_NODE = "connecting_node";
    public static final String WIRE_TYPE = "wire_type";
    public static final String X = "x";
    public static final String Y = "y";
    public static final String Z = "z";

    private final BlockEntity blockEntity;
    private final int index;
    private final int connectingIndex;
    private final WireType wireType;
    private Vec3i relativePos;
    private boolean invalid = false;

    public LocalNode(BlockEntity blockEntity, int index, int connectingIndex, WireType wireType, BlockPos blockPos) {
        this.blockEntity = blockEntity;
        this.index = index;
        this.connectingIndex = connectingIndex;
        this.wireType = wireType;
        this.relativePos = blockPos.subtract(blockEntity.getBlockPos());
    }

    public LocalNode(BlockEntity blockEntity, CompoundTag compoundTag) {
        this.blockEntity = blockEntity;
        this.index = compoundTag.getInt(ID);
        this.connectingIndex = compoundTag.getInt(CONNECTING_NODE);
        this.wireType = WireType.fromIndex(compoundTag.getInt(WIRE_TYPE));
        this.relativePos = new Vec3i(compoundTag.getInt(X), compoundTag.getInt(Y), compoundTag.getInt(Z));
    }

    public void write(CompoundTag compoundTag) {
        compoundTag.putInt(ID, this.index);
        compoundTag.putInt(CONNECTING_NODE, this.connectingIndex);
        compoundTag.putInt(WIRE_TYPE, this.wireType.getID());
        compoundTag.putInt(X, this.relativePos.getX());
        compoundTag.putInt(Y, this.relativePos.getY());
        compoundTag.putInt(Z, this.relativePos.getZ());
    }

    public int getIndex() {
        return index;
    }

    public int getConnectingIndex() {
        return connectingIndex;
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
