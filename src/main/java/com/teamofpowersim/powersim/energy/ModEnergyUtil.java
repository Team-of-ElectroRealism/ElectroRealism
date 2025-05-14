package com.teamofpowersim.powersim.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class ModEnergyUtil {
    public static boolean move(BlockPos fromPos, BlockPos toPos, int amount, Level level) {
        IEnergyStorage fromStorage = level.getCapability(Capabilities.EnergyStorage.BLOCK, fromPos, null);
        IEnergyStorage toStorage = level.getCapability(Capabilities.EnergyStorage.BLOCK, toPos, null);

        if (isEnergyStorageEmptyOrCannotExtract(fromStorage, amount)) {
            return false;
        }
        if (isEnergyStorageFullOrCannotReceive(toStorage)) {
            return false;
        }

        int maxAmountToReceive = toStorage.receiveEnergy(amount, true);
        int extractedEnergy = fromStorage.extractEnergy(maxAmountToReceive, false);
        toStorage.receiveEnergy(extractedEnergy, false);

        return true;
    }

    private static boolean isEnergyStorageFullOrCannotReceive(IEnergyStorage toStorage) {
        return toStorage.getEnergyStored() >= toStorage.getMaxEnergyStored() || !toStorage.canReceive();
    }

    private static boolean isEnergyStorageEmptyOrCannotExtract(IEnergyStorage fromStorage, int amount) {
        return fromStorage.getEnergyStored() <= 0 || fromStorage.getEnergyStored() < amount || !fromStorage.canExtract();
    }

    public static boolean doesBlockHaveEnergyStorage(BlockPos blockPosToCheck, Level level) {
        return level.getBlockEntity(blockPosToCheck) != null && level.getCapability(Capabilities.EnergyStorage.BLOCK, blockPosToCheck, null) != null;
    }
}
