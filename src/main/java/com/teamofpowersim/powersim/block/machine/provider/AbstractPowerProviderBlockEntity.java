package com.teamofpowersim.powersim.block.machine.provider;

import com.teamofpowersim.powersim.block.IActiveVoltageProvider; // Import this
import com.teamofpowersim.powersim.block.IVoltageProvider;
import com.teamofpowersim.powersim.block.machine.AbstractMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractPowerProviderBlockEntity extends AbstractMachineBlockEntity implements IVoltageProvider {

    public AbstractPowerProviderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * Gets the voltage this provider should contribute to the simulation.
     * If this provider is an IActiveVoltageProvider and is not currently active,
     * it will return 0. Otherwise, it returns its nominal operating voltage.
     *
     * @return The voltage for the simulation (0 if inactive IActiveVoltageProvider).
     */
    @Override
    public int getVoltage() {
        if (this instanceof IActiveVoltageProvider activeProvider) {
            if (!activeProvider.isActive()) {
                return 0; // Generator is active but not running, so 0V for simulation.
            }
        }
        // If it's not an IActiveVoltageProvider, or if it is and it's active,
        // return its nominal voltage.
        return getNominalVoltage();
    }

    /**
     * Gets the nominal (designed) output voltage of this provider when it is active.
     * Subclasses must implement this to define their standard output.
     * @return The nominal voltage.
     */
    public abstract int getNominalVoltage();
}