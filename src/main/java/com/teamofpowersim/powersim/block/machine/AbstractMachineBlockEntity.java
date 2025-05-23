package com.teamofpowersim.powersim.block.machine;

import com.mojang.logging.LogUtils;
import com.teamofpowersim.powersim.PowerSim;
import com.teamofpowersim.powersim.network.INetworkMember;
import com.teamofpowersim.powersim.network.NetworkManager;
import com.teamofpowersim.powersim.simulation.ISimulatable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.UUID;

public abstract class AbstractMachineBlockEntity extends BlockEntity implements INetworkMember, ISimulatable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private UUID networkId;

    private double simVoltage;
    private double simCurrent;

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
    public void setNetworkId(UUID newNetworkId) {
        if (!Objects.equals(this.networkId, newNetworkId)) {
            LOGGER.trace("Machine at {} changing network ID from {} to {}", getPos(), this.networkId, newNetworkId);
            this.networkId = newNetworkId;

            if (this.networkId != null && this.level != null && !this.level.isClientSide()) {
                PowerSim.NETWORK_MANAGER.registerINetworkMemberInNetwork(this.networkId, this);
            }

            setChanged();
        } else {
            LOGGER.trace("Machine at {} setNetworkId called with same ID {}", getPos(), newNetworkId);
        }
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
            NetworkManager networkManager = PowerSim.NETWORK_MANAGER;
            networkManager.removeNetworkMember(this);
        }
        super.setRemoved();
    }

    @Override
    public void applySimulation(double voltage, double current) {
        this.simVoltage = voltage;
        this.simCurrent = current;
    }

    public double getSimVoltage() { return simVoltage; }
    public double getSimCurrent() { return simCurrent; }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.networkId != null && this.level != null && !this.level.isClientSide()) {
            LOGGER.trace("Machine at {} loaded with ID {}. Registering.", getPos(), this.networkId);
            PowerSim.NETWORK_MANAGER.registerINetworkMemberInNetwork(this.networkId, this);
        } else if (this.level != null && !this.level.isClientSide()) {
            LOGGER.trace("Machine at {} loaded without network ID.", getPos());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.networkId != null) {
            tag.putUUID(NETWORK_KEY, this.networkId);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(NETWORK_KEY, CompoundTag.TAG_INT_ARRAY)) {
            this.networkId = tag.getUUID(NETWORK_KEY);
        } else {
            this.networkId = null;
        }
    }
}
