package com.teamofelectrorealism.electrorealism.block.crusher;

import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ElectricCrusherBlockEntity extends BlockEntity {
    private int testValue;

    public ElectricCrusherBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.ELECTRIC_CRUSHER_BE.get(), pos, blockState);
    }

    @Override
    public void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
        super.loadAdditional(compoundTag, registries);
        this.testValue = compoundTag.getInt("value");
    }
    @Override
    public void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
        super.saveAdditional(compoundTag, registries);
        compoundTag.putInt("value", this.testValue);
    }
}
