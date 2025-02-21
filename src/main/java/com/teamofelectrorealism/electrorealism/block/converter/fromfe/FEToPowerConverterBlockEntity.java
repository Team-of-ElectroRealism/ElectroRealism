package com.teamofelectrorealism.electrorealism.block.converter.fromfe;

import com.teamofelectrorealism.electrorealism.api.ElectricalAPI;
import com.teamofelectrorealism.electrorealism.block.IVoltageProvider;
import com.teamofelectrorealism.electrorealism.block.IVoltageReceiver;
import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.block.converter.EnergyConverterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;

public class FEToPowerConverterBlockEntity extends EnergyConverterBlockEntity implements IVoltageProvider {

    private static final String VOLTAGE_KEY = "fe_converter.voltage";
    private static final String ENERGY_KEY = "fe_converter.energy_stored";
    private static final String ENERGY_STORAGE_KEY = "fe_converter.energy_storage";

    private static final int MAX_ENERGY_STORED = 10000;
    private static final int MAX_OUTPUT = 1000;
    private static final int MAX_CURRENT = 100;
    private static final int CONVERSION_RATE = 1;

    private int voltage;
    private int energyStored;
    private final EnergyStorage energyStorage;

    public FEToPowerConverterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.FE_CONVERTER_BE.get(), pos, blockState);
        this.energyStored = 0;
        this.energyStorage = new EnergyStorage(MAX_ENERGY_STORED, MAX_OUTPUT, 0);
    }

    @Override
    public int getVoltage() {
        return voltage;
    }

    @Override
    public int getEnergyToConvert() {
        int energyToConvert = Math.min(energyStored, MAX_OUTPUT);
        return energyToConvert;
    }

    @Override
    public void convertEnergy() {
        int energyToConvert = getEnergyToConvert();

        if (energyToConvert > 0) {
            int power = energyToConvert * CONVERSION_RATE;
            updateVoltageOut(power, MAX_CURRENT);
        }
    }

    private void updateVoltageOut(int power, int maxCurrent) {
        voltage = ElectricalAPI.getVoltageFromPower(power, maxCurrent);
    }

    private BlockPos findPowerReceiver(Level level, BlockPos startPos) {
        for (Direction direction: Direction.values()) {
            BlockPos neighborPos = startPos.relative(direction);
            if (level.isLoaded(neighborPos)) {
                BlockEntity neighbor = level.getBlockEntity(neighborPos);
                if (neighbor instanceof IVoltageReceiver) {
                    return neighborPos;
                }
            }
        }
        return null;
    }

    private void transferVoltage(Level level, BlockPos pos) {
        for (Direction facing: Direction.values()) {
            BlockPos neighborPos = pos.offset(facing.getNormal());
            BlockEntity blockEntity = level.getBlockEntity(neighborPos);
            if (blockEntity instanceof IVoltageReceiver) {
                IVoltageReceiver receiver = (IVoltageReceiver) blockEntity;

                receiver.receiveVoltage(voltage);
            }
        }
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (!level.isClientSide()) {
            convertEnergy();
            BlockPos receiverPos = findPowerReceiver(level, pos);
            if (receiverPos != null) {
                transferVoltage(level, receiverPos);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
        compoundTag.putInt(VOLTAGE_KEY, voltage);
        compoundTag.putInt(ENERGY_KEY, energyStored);
        compoundTag.put(ENERGY_STORAGE_KEY, energyStorage.serializeNBT(registries));

        super.saveAdditional(compoundTag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
        super.loadAdditional(compoundTag, registries);
        energyStorage.deserializeNBT(registries, compoundTag.getCompound(ENERGY_STORAGE_KEY));
        energyStored = compoundTag.getInt(ENERGY_KEY);
        voltage = compoundTag.getInt(VOLTAGE_KEY);
    }
}
