package com.teamofpowersim.powersim.block.machine.provider.solarpanel;

import com.teamofpowersim.powersim.Config;
import com.teamofpowersim.powersim.PowerSim; // For NetworkManager
import com.teamofpowersim.powersim.block.IActiveVoltageProvider; // Import this
import com.teamofpowersim.powersim.block.IVoltageConsumer;
import com.teamofpowersim.powersim.block.ModBlockEntityTypes;
import com.teamofpowersim.powersim.block.machine.provider.AbstractPowerProviderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger; // For logging
import com.mojang.logging.LogUtils; // For logging

public class SolarPanelBlockEntity extends AbstractPowerProviderBlockEntity implements IActiveVoltageProvider { // Implement IActiveVoltageProvider
    private static final Logger LOGGER = LogUtils.getLogger();

    private final int nominalVoltage = (int) Config.solarPanelOutputVoltage; // TODO should be double
    private boolean currentActiveState = false; // Cache the active state to detect changes

    public SolarPanelBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.SOLAR_PANEL_BE.get(), pos, blockState);
    }

    @Override
    public int getNominalVoltage() {
        return this.nominalVoltage;
    }

    @Override
    public boolean isActive() {
        if (this.level == null) return false;
        BlockPos skyPos = getBlockPos().above();
        return this.level.canSeeSky(skyPos) && this.level.isDay() && !this.level.isRaining() && !this.level.isThundering();
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }

        boolean wasPreviouslyActive = this.currentActiveState; // Use the cached state
        boolean isCurrentlyActive = isActive(); // Calculate current state

        if (isCurrentlyActive) {
            // The direct transferVoltage call is likely redundant if ngspice handles power flow.
            // this.transferVoltage(level, pos); // Consider removing
        }

        if (wasPreviouslyActive != isCurrentlyActive) {
            this.currentActiveState = isCurrentlyActive; // Update cached state
            LOGGER.debug("Solar Panel at {} active state changed: {} -> {}. Requesting network dirty status.",
                    pos.toShortString(), wasPreviouslyActive, isCurrentlyActive);
            if (PowerSim.NETWORK_MANAGER != null) {
                PowerSim.NETWORK_MANAGER.markNetworkDirty(this);
            } else {
                LOGGER.error("Solar Panel at {} changed active state, but NetworkManager is NULL!", pos.toShortString());
            }
            setChanged(); // State relevant to rendering/saving might have changed
        }
    }

    @Override
    public boolean isFaceAllowed(BlockState state, Direction faceAccessed) {
        // Solar panels usually output from bottom or specific sides, not typically from the top face.
        return faceAccessed == Direction.DOWN; // Example: Power comes out the bottom
    }
}