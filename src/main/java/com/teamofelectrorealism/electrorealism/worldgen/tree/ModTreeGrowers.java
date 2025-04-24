package com.teamofelectrorealism.electrorealism.worldgen.tree;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.worldgen.ModConfiguredFeatures;
import net.minecraft.world.level.block.grower.TreeGrower;

import java.util.Optional;

public class ModTreeGrowers {
    public static final TreeGrower RUBBER = new TreeGrower(ElectroRealism.MODID + ":rubber",
            Optional.empty(), Optional.of(ModConfiguredFeatures.RUBBER_KEY), Optional.empty());
}
