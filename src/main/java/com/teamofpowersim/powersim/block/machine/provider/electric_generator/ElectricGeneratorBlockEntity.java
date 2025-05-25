package com.teamofpowersim.powersim.block.machine.provider.electric_generator;

import com.teamofpowersim.powersim.block.ModBlockEntityTypes;
import com.teamofpowersim.powersim.block.machine.provider.AbstractPowerProviderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ElectricGeneratorBlockEntity extends AbstractPowerProviderBlockEntity {
    public final int nominalVoltage = 230; // Renamed for clarity

    public ElectricGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.ELECTRIC_GENERATOR_BE.get(), pos, blockState);
    }

    @Override
    public int getNominalVoltage() {
        return this.nominalVoltage;
    }
    // The getVoltage() method is inherited from AbstractPowerProviderBlockEntity.
    // Since this class doesn't implement IActiveVoltageProvider,
    // the inherited getVoltage() will always call this.getNominalVoltage().

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        // If this generator is truly always on, there's no active state change to monitor
        // and thus no need to call markNetworkDirty.
        // If it had an on/off switch block state, then it would need IActiveVoltageProvider.

        // The direct transferVoltage call is likely redundant if ngspice handles power flow.
        // this.transferVoltage(level, pos); // Consider removing if ngspice is authoritative
    }

    @Override
    public boolean isFaceAllowed(BlockState state, Direction faceAccessed) {
        return true;
    }
}