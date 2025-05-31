package com.teamofpowersim.powersim.block.machine.provider.waterwheel;

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
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.slf4j.Logger; // For logging
import com.mojang.logging.LogUtils; // For logging

public class WaterWheelBlockEntity extends AbstractPowerProviderBlockEntity implements IActiveVoltageProvider { // Implement IActiveVoltageProvider
    private static final Logger LOGGER = LogUtils.getLogger();

    public final int nominalVoltage = 110; // Water wheels might produce lower voltage, example
    private boolean currentActiveState = false; // Cache the active state

    public WaterWheelBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.WATER_WHEEL_BE.get(), pos, blockState);
    }

    @Override
    public int getNominalVoltage() {
        return this.nominalVoltage;
    }

    @Override
    public boolean isActive() {
        if (this.level == null) return false;
        return isMovingWaterBelow(this.level, getBlockPos());
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }

        boolean wasPreviouslyActive = this.currentActiveState; // Use cached
        boolean isCurrentlyActive = isActive();

        if (isCurrentlyActive) {
            // The direct transferVoltage call is likely redundant if ngspice handles power flow.
            // this.transferVoltage(level, pos); // Consider removing
        }

        if (wasPreviouslyActive != isCurrentlyActive) {
            this.currentActiveState = isCurrentlyActive; // Update cache
            LOGGER.debug("Water Wheel at {} active state changed: {} -> {}. Requesting network dirty status.",
                    pos.toShortString(), wasPreviouslyActive, isCurrentlyActive);
            if (PowerSim.NETWORK_MANAGER != null) {
                PowerSim.NETWORK_MANAGER.markNetworkDirty(this);
            } else {
                LOGGER.error("Water Wheel at {} changed active state, but NetworkManager is NULL!", pos.toShortString());
            }
            setChanged(); // State relevant to rendering/saving might have changed
        }
    }

    @Override
    public boolean isFaceAllowed(BlockState state, Direction faceAccessed) {
        // Water wheels might output from axle ends (e.g., sides relative to its orientation)
        // This depends on your block's model and intended connection points.
        // For now, let's assume it can output to sides (relative to some facing property if you add one)
        // or just all horizontal directions.
        return faceAccessed.getAxis().isHorizontal(); // Example
    }

    public static boolean isMovingWaterBelow(Level level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        // Check multiple blocks below and around if the wheel is large
        // For a simple 1x1 base, just belowPos is fine.
        FluidState fluidState = level.getFluidState(belowPos);

        // Check if it's water and if it's flowing (has a non-zero flow vector or isn't a source block at max level)
        if (fluidState.is(Fluids.WATER) || fluidState.is(Fluids.FLOWING_WATER)) {
            if (fluidState.isSource()) { // Source block
                return false; // Still water doesn't turn a typical wheel model, needs flow
            }
            // Check if it has a flow vector (more advanced) or simply if it's not empty
            return !fluidState.isEmpty();
        }
        return false;
    }
}