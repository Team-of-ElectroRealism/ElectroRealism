package com.teamofpowersim.powersim.worldgen.tree;

import com.teamofpowersim.powersim.PowerSim;
import com.teamofpowersim.powersim.worldgen.ModConfiguredFeatures;
import net.minecraft.world.level.block.grower.TreeGrower;

import java.util.Optional;

public class ModTreeGrowers {
    public static final TreeGrower RUBBER = new TreeGrower(PowerSim.MODID + ":rubber",
            Optional.empty(), Optional.of(ModConfiguredFeatures.RUBBER_KEY), Optional.empty());
}
