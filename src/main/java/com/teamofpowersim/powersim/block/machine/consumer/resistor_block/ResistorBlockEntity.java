package com.teamofpowersim.powersim.block.machine.consumer.resistor_block;

import com.teamofpowersim.powersim.block.ModBlockEntityTypes;
import com.teamofpowersim.powersim.block.machine.consumer.AbstractPowerConsumerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ResistorBlockEntity extends AbstractPowerConsumerBlockEntity {
    private static final String INVENTORY_KEY = "inventory";
    private static final int DEFAULT_RESISTANCE = 100;

    public final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            if (level != null && !level.isClientSide()) {
                setChanged();
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    /** synced to the client via the menu’s ContainerData slot 0 */
    private final ContainerData data = new SimpleContainerData(1) {
        @Override public int get(int index)          { return resistance; }
        @Override public void set(int index, int val){ resistance = Math.max(1, val); }   // clamp at 1 Ω
        @Override public int getCount()              { return 1; }
    };

    private int resistance = DEFAULT_RESISTANCE;

    public ResistorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.RESISTOR_BLOCK_BE.get(), pos, blockState);
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState blockState) {
        if(level == null) return;

        double voltage = getSimVoltage();
        double current = getSimCurrent();
        double watts = Math.abs(voltage * current);


    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound(INVENTORY_KEY));
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(INVENTORY_KEY, itemHandler.serializeNBT(registries));
    }

    @Override
    public boolean isFaceAllowed(BlockState blockState, Direction faceAccessed) {
        if (blockState.hasProperty(ResistorBlock.FACING)) {
            Direction facing = blockState.getValue(ResistorBlock.FACING);
            return faceAccessed != facing && faceAccessed != facing.getOpposite();
        }
        return true;
    }

    public int getResistance() {
        return resistance;
    }

    private long lastClickTick;

    public long getLastClickTick() {
        return lastClickTick;
    }

    public void setLastClickTick(long tick) {
        lastClickTick = tick;
    }

    /* convenience accessor used by the screen */
    public void setResistanceFromClient(int value){
        resistance = Math.max(1, value);
        setChanged();
    }
}
