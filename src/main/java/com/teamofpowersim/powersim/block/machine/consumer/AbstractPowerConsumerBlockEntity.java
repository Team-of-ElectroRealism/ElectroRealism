package com.teamofpowersim.powersim.block.machine.consumer;

import com.teamofpowersim.powersim.block.IVoltageConsumer;
import com.teamofpowersim.powersim.block.machine.AbstractMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractPowerConsumerBlockEntity extends AbstractMachineBlockEntity implements IVoltageConsumer {
    // private static final Logger LOGGER = LogUtils.getLogger();

    // Common defaults, can be overridden by subclasses
    protected static final double DEFAULT_MIN_OPERATING_VOLTAGE = 50.0;
    // NOMINAL_OPERATING_CURRENT might be less relevant without a buffer charge rate,
    // but can still define a "sweet spot" for efficient operation if you add efficiency mechanics.
    protected static final double DEFAULT_NOMINAL_OPERATING_CURRENT = 5.0;
    protected static final double DEFAULT_MAX_SAFE_CURRENT = 30.0;
    // ENERGY_COST_PER_TICK is now more like a flag: if powered, it can do one "unit" of work.
    // The actual energy "consumed" is what the simulation dictates (V*I*dt).

    public AbstractPowerConsumerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public int getResistance() {
        return 200; // Default, override in specific machines
    }

    /**
     * Represents the powered state for GUI and compatibility.
     * @return 1 if considered powered by the network this tick, 0 otherwise.
     */
    @Override
    public int getBufferCharge() {
        return isConsideredPoweredByNetwork() ? 1 : 0;
    }

    /**
     * This method is part of IVoltageConsumer. Since there's no actual buffer,
     * this method effectively does nothing in this "no buffer" model.
     * It's here to satisfy the interface.
     */

    /**
     * Checks if the machine is receiving sufficient power from the simulation in the current tick.
     * This is the primary condition for operation in a "no buffer" model.
     * @return true if simVoltage and simCurrent meet operating criteria.
     */
    protected boolean isConsideredPoweredByNetwork() {
        double simV = getSimVoltage();
        double simI = getSimCurrent();
        // Add a small threshold for current to ensure it's actually drawing power
        // and not just floating at a voltage with no current.
        boolean hasMinimalCurrent = Math.abs(simI) > (getNominalOperatingCurrent() * 0.05);

        return (Math.abs(simV) >= getMinOperatingVoltage()) &&
                (Math.signum(simV) == Math.signum(simI) && hasMinimalCurrent);
    }

    // Methods for subclasses to override to define their specific parameters
    protected double getMinOperatingVoltage() {
        return DEFAULT_MIN_OPERATING_VOLTAGE;
    }

    protected double getNominalOperatingCurrent() {
        return DEFAULT_NOMINAL_OPERATING_CURRENT;
    }

    protected double getMaxSafeCurrent() {
        return DEFAULT_MAX_SAFE_CURRENT;
    }
}