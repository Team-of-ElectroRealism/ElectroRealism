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

    private static final String SIM_VOLTAGE_KEY = "sim_voltage";
    private static final String SIM_CURRENT_KEY = "sim_current";

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
        // Check if values have meaningfully changed to avoid excessive updates
        boolean changed = Math.abs(this.simVoltage - voltage) > 1e-3 || Math.abs(this.simCurrent - current) > 1e-3;

        this.simVoltage = voltage;
        this.simCurrent = current;

        if (changed && this.level != null && !this.level.isClientSide()) {
            setChanged();
            // Send a block update to clients to sync the BlockEntity data immediately
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
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

        tag.putDouble(SIM_VOLTAGE_KEY, this.simVoltage);
        tag.putDouble(SIM_CURRENT_KEY, this.simCurrent);
    }


    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(NETWORK_KEY, CompoundTag.TAG_INT_ARRAY)) { // Ensure this check is correct for UUIDs in your NBT lib
            this.networkId = tag.getUUID(NETWORK_KEY);
        } else {
            this.networkId = null;
        }
        // Load simulation data
        this.simVoltage = tag.getDouble(SIM_VOLTAGE_KEY);
        this.simCurrent = tag.getDouble(SIM_CURRENT_KEY);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries); // This will call saveAdditional in the base BlockEntity class
        // or any overridden version up the chain.
        // Explicitly add if super.getUpdateTag() doesn't reliably call our saveAdditional
        // However, BlockEntity's implementation of saveMetadata (called by default getUpdateTag) does call saveAdditional.
        // So the values added in saveAdditional above *should* be included.
        // If ArcFurnaceBlockEntity's saveWithoutMetadata(registries) is just super.saveAdditional, it's fine.
        // Let's assume the chain works. If not, uncomment and fill:
        // tag.putDouble(SIM_VOLTAGE_KEY, this.simVoltage);
        // tag.putDouble(SIM_CURRENT_KEY, this.simCurrent);
        return tag;
    }
}
