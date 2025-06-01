package com.teamofpowersim.powersim.block.machine.consumer.lamp;

import com.mojang.logging.LogUtils;
import com.teamofpowersim.powersim.block.ModBlockEntityTypes;
import com.teamofpowersim.powersim.block.machine.consumer.AbstractPowerConsumerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public class ElectricLampBlockEntity extends AbstractPowerConsumerBlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    // NBT Keys
    private static final String INTERNAL_RESISTANCE_KEY = "electric_lamp.internal_resistance";

    private static final double LAMP_MIN_OPERATING_VOLTAGE = 24;
    private static final double LAMP_NOMINAL_OPERATING_CURRENT = 8.0;
    private static final double LAMP_MAX_SAFE_CURRENT = 20.0;

    private static final int DEFAULT_INTERNAL_RESISTANCE = 25;

    private int internalResistance = DEFAULT_INTERNAL_RESISTANCE;

    public ElectricLampBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.ELECTRIC_LAMP_BE.get(), pos, blockState);
    }

    @Override
    protected double getMinOperatingVoltage() { return LAMP_MIN_OPERATING_VOLTAGE; }
    @Override
    protected double getNominalOperatingCurrent() { return LAMP_NOMINAL_OPERATING_CURRENT; }
    @Override
    protected double getMaxSafeCurrent() { return LAMP_MAX_SAFE_CURRENT; }
    @Override
    public int getResistance() { return this.internalResistance; }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }

        boolean wasLit = state.getValue(ElectricLampBlock.LIT);
        boolean isLit = isLit();

        // Overcurrent check
        double actualCurrent = getSimCurrent();
        if (Math.abs(actualCurrent) > getMaxSafeCurrent()) {
            LOGGER.warn("Electric Lamp at {} OVERCURRENT! I: {:.2f}A > {:.2f}A. Destroying.", pos, actualCurrent, getMaxSafeCurrent());
            level.destroyBlock(pos, true);
            return;
        }
        if (isLit != wasLit) {
            level.setBlockAndUpdate(pos, state.setValue(ElectricLampBlock.LIT, isLit));
            setChanged();
        }
    }

    protected boolean isLit() {
        return isConsideredPoweredByNetwork();
    }

    @Override
    public boolean isFaceAllowed(BlockState state, Direction faceAccessed) {
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(INTERNAL_RESISTANCE_KEY, this.internalResistance);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.internalResistance = tag.contains(INTERNAL_RESISTANCE_KEY) ? tag.getInt(INTERNAL_RESISTANCE_KEY) : DEFAULT_INTERNAL_RESISTANCE;
    }
}
