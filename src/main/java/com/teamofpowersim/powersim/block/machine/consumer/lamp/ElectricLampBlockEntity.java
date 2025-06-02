package com.teamofpowersim.powersim.block.machine.consumer.lamp;

import com.mojang.logging.LogUtils;
import com.teamofpowersim.powersim.Config;
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

    public static final double LAMP_MIN_OPERATING_VOLTAGE       = Config.electricLampMinOperatingVoltage;
    public static final double LAMP_NOMINAL_OPERATING_CURRENT   = Config.electricLampNominalOperatingCurrent;
    public static final double LAMP_MAX_SAFE_CURRENT            = Config.electricLampMaxSafeCurrent;
    public static final double LAMP_MIN_LIGHT_POWER_WATTS       = Config.electricLampMinLightPowerWatts;

    private static final String INTERNAL_RESISTANCE_KEY         = "electric_lamp.internal_resistance";
    private static final int DEFAULT_INTERNAL_RESISTANCE        = Config.electricLampInternalResistance;

    private int internalResistance = DEFAULT_INTERNAL_RESISTANCE;

    /** Creates a new lamp block‑entity. */
    public ElectricLampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.ELECTRIC_LAMP_BE.get(), pos, state);
    }

    // ───── Electrical characteristics (API overrides) ─────

    @Override protected double getMinOperatingVoltage()   { return LAMP_MIN_OPERATING_VOLTAGE; }
    @Override protected double getNominalOperatingCurrent(){ return LAMP_NOMINAL_OPERATING_CURRENT; }
    @Override protected double getMaxSafeCurrent()         { return LAMP_MAX_SAFE_CURRENT; }
    @Override public    int    getResistance()             { return internalResistance; }

    // ───── Per‑tick behaviour ─────

    /** Updates light level and over‑current protection each server tick. */
    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;

        double voltage = getSimVoltage();
        double current = getSimCurrent();
        double watts   = Math.abs(voltage * current);

        // Blow the bulb if current exceeds safe limit.
        if (Math.abs(current) > getMaxSafeCurrent()) {
            LOGGER.warn("Lamp at {} over‑current ({}A > {}A). Destroying.", pos, current, getMaxSafeCurrent());
            level.destroyBlock(pos, true);
            return;
        }

        int oldLvl = state.getValue(ElectricLampBlock.LIGHT_LEVEL);
        int newLvl;
        if (watts < LAMP_MIN_LIGHT_POWER_WATTS || !isConsideredPoweredByNetwork()) {
            newLvl = 0;
        } else {
            double maxW = getMinOperatingVoltage() * getMaxSafeCurrent();
            double ratio = (watts - LAMP_MIN_LIGHT_POWER_WATTS) / Math.max(1.0, maxW - LAMP_MIN_LIGHT_POWER_WATTS);
            newLvl = Math.max(1, (int)Math.ceil(Math.min(ratio, 1.0) * 15.0));
        }
        if (newLvl != oldLvl) {
            level.setBlockAndUpdate(pos, state.setValue(ElectricLampBlock.LIGHT_LEVEL, newLvl)
                    .setValue(ElectricLampBlock.LIT, newLvl > 0));
            setChanged();
        }
    }

    // ───── Capability access ─────

    @Override public boolean isFaceAllowed(BlockState state, Direction face) { return true; }

    // ───── Persistence ─────

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.putInt(INTERNAL_RESISTANCE_KEY, internalResistance);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        internalResistance = tag.getInt(INTERNAL_RESISTANCE_KEY);
    }
}

