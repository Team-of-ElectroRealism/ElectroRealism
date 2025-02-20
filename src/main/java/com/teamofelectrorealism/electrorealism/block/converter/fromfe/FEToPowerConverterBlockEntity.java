package com.teamofelectrorealism.electrorealism.block.converter.fromfe;

import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.block.converter.EnergyConverterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class FEToPowerConverterBlockEntity extends EnergyConverterBlockEntity {
    // todo
    private static final int MAX_ENERGY_STORED = 10000;
    private static final int MAX_OUTPUT = 1000;

    private int energyStored;
    private final IEnergyStorage energyStorage;

    public FEToPowerConverterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.FE_CONVERTER_BE.get(), pos, blockState);
        this.energyStored = 0;
        this.energyStorage = new EnergyStorage(MAX_ENERGY_STORED, MAX_OUTPUT, 0);
    }

    @Override
    public int getEnergyToConvert() {
        int energyToConvert = Math.min(energyStored, MAX_OUTPUT);
        return energyToConvert;
    }

    @Override
    public void convertEnergy() {

    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide() || level == null) {
            return;
        }
        convertEnergy();
        sendVoltageToReciever();
    }
}
