package com.teamofelectrorealism.electrorealism.block.machine.consumer.refinery;

import com.teamofelectrorealism.electrorealism.api.ElectricalAPI;
import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.block.machine.consumer.AbstractPowerConsumerBlockEntity;
import com.teamofelectrorealism.electrorealism.recipe.ModRecipes;
import com.teamofelectrorealism.electrorealism.recipe.refinery.RefineryRecipe;
import com.teamofelectrorealism.electrorealism.recipe.refinery.RefineryRecipeInput;
import com.teamofelectrorealism.electrorealism.screen.refinery.RefineryMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class RefineryBlockEntity extends AbstractPowerConsumerBlockEntity implements MenuProvider {
    public final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            if (!level.isClientSide()) {
                setChanged();
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;

    private static final String INVENTORY_KEY = "inventory";
    private static final String BUFFER_LEVEL_KEY = "refinery.buffer_level";
    private static final String BUFFER_TOTAL_LEVEL_KEY = "refinery.buffer_total_level";
    private static final String REFINING_PROGRESS_KEY = "refinery.crushing_progress";
    private static final String REFINING_TOTAL_TIME_KEY = "refinery.crushing_total_time";
    private static final String INTERNAL_RESISTANCE_KEY = "refinery.internal_resistance";

    private static final int TOTAL_REFINING_TIME = 200;
    private static final int TOTAL_BUFFER_CAPACITY = 2000; // In mAh
    private static final int INTERNAL_RESISTANCE = 10; // In ohm

    private int bufferLevel;
    private int bufferTotalLevel = TOTAL_BUFFER_CAPACITY;
    private int refiningProgress;
    private int refiningTotalTime = TOTAL_REFINING_TIME;
    private int internalResistance = INTERNAL_RESISTANCE;
    private final ContainerData data;

    public RefineryBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.REFINERY_BE.get(), pos, blockState);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> RefineryBlockEntity.this.refiningProgress;
                    case 1 -> RefineryBlockEntity.this.refiningTotalTime;
                    case 2 -> RefineryBlockEntity.this.bufferLevel;
                    case 3 -> RefineryBlockEntity.this.bufferTotalLevel;
                    case 4 -> RefineryBlockEntity.this.internalResistance;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0: RefineryBlockEntity.this.refiningProgress = value;
                    case 1: RefineryBlockEntity.this.refiningTotalTime = value;
                    case 2: RefineryBlockEntity.this.bufferLevel = value;
                    case 3: RefineryBlockEntity.this.bufferTotalLevel = value;
                    case 4: RefineryBlockEntity.this.internalResistance = value;
                }
            }

            @Override
            public int getCount() {
                return 5;
            }
        };
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.electrorealism.refinery");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RefineryMenu(containerId, playerInventory, this, this.data);
    }

    @Override
    public void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
        super.loadAdditional(compoundTag, registries);
        itemHandler.deserializeNBT(registries, compoundTag.getCompound(INVENTORY_KEY));
        bufferLevel = compoundTag.getInt(BUFFER_LEVEL_KEY);
        bufferTotalLevel = compoundTag.getInt(BUFFER_TOTAL_LEVEL_KEY);
        refiningProgress = compoundTag.getInt(REFINING_PROGRESS_KEY);
        refiningTotalTime = compoundTag.getInt(REFINING_TOTAL_TIME_KEY);
        internalResistance = compoundTag.getInt(INTERNAL_RESISTANCE_KEY);
    }
    @Override
    public void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
        compoundTag.put(INVENTORY_KEY, itemHandler.serializeNBT(registries));
        compoundTag.putInt(BUFFER_LEVEL_KEY, bufferLevel);
        compoundTag.putInt(BUFFER_TOTAL_LEVEL_KEY, bufferTotalLevel);
        compoundTag.putInt(REFINING_PROGRESS_KEY, refiningProgress);
        compoundTag.putInt(REFINING_TOTAL_TIME_KEY, refiningTotalTime);
        compoundTag.putInt(INTERNAL_RESISTANCE_KEY, internalResistance);

        super.saveAdditional(compoundTag, registries);
    }

    @Override
    public void receiveVoltage(int voltage) {
        int totalResistance = this.internalResistance;

        int chargeIncrease = ElectricalAPI.getChargeIncreaseMah(voltage, totalResistance, (double) 1 / 20);

        setBufferCharge(bufferLevel + chargeIncrease);
    }

    @Override
    public int getBufferCharge() {
        return bufferLevel;
    }

    @Override
    public void setBufferCharge(int charge) {
        this.bufferLevel = Math.max(0, Math.min(charge, bufferTotalLevel));
    }

    @Override
    public int getResistance() {
        return internalResistance;
    }

    @Override
    public boolean isFaceAllowed(BlockState state, Direction faceAccessed) {
        return true;
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (hasRecipe() && isOutputSlotEmptyOrReceivable()) {
            if (isPowered() && hasBufferEnoughCharge()) {
                increaseRefiningProgress();
                decreaseBufferCharge();
                if (hasRefiningFinished()) {
                    refineItem();
                    resetProgress();
                }
            } else {
                refiningProgress = 0;
            }
        } else {
            refiningProgress = 0;
        }
    }

    public void clearContents() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    public void drops() {
        SimpleContainer inv = new SimpleContainer(itemHandler.getSlots());
        for(int i = 0; i < itemHandler.getSlots(); i++) {
            inv.setItem(i, itemHandler.getStackInSlot(i));
        }

        Containers.dropContents(this.level, this.worldPosition, inv);
    }

    private boolean hasRefiningFinished() {
        return this.refiningProgress >= this.refiningTotalTime;
    }

    private void increaseRefiningProgress() {
        refiningProgress++;
    }

    private boolean hasBufferEnoughCharge() {
        return bufferLevel >= 100;
    }

    private void decreaseBufferCharge() {
        bufferLevel = bufferLevel - 3;
    }

    private boolean isPowered() {
        return this.bufferLevel > 0;
    }

    private void resetProgress() {
        this.refiningProgress = 0;
        this.refiningTotalTime = TOTAL_REFINING_TIME;
    }

    private boolean canInsertItemIntoOutputSlot(ItemStack output) {
        return itemHandler.getStackInSlot(SLOT_OUTPUT).isEmpty() || itemHandler.getStackInSlot(SLOT_OUTPUT).getItem() == output.getItem();
    }

    private boolean canInsertAmountIntoOutputSlot(int count) {
        int maxCount = itemHandler.getStackInSlot(SLOT_OUTPUT).isEmpty() ? 64 : itemHandler.getStackInSlot(SLOT_OUTPUT).getMaxStackSize();
        int currentCount = itemHandler.getStackInSlot(SLOT_OUTPUT).getCount();

        return maxCount >= currentCount + count;
    }

    private boolean isOutputSlotEmptyOrReceivable() {
        return this.itemHandler.getStackInSlot(SLOT_OUTPUT).isEmpty() || this.itemHandler.getStackInSlot(SLOT_OUTPUT).getCount() < this.itemHandler.getStackInSlot(SLOT_OUTPUT).getMaxStackSize();
    }

    private Optional<RecipeHolder<RefineryRecipe>> getCurrentRecipe() {
        return this.level.getRecipeManager().getRecipeFor(ModRecipes.REFINERY_TYPE.get(), new RefineryRecipeInput(itemHandler.getStackInSlot(SLOT_INPUT)), level);
    }

    private void refineItem() {
        Optional<RecipeHolder<RefineryRecipe>> recipe = getCurrentRecipe();
        ItemStack output = recipe.get().value().getResultItem(null);

        itemHandler.extractItem(SLOT_INPUT, 1, false);
        itemHandler.setStackInSlot(SLOT_OUTPUT, new ItemStack(output.getItem(), itemHandler.getStackInSlot(SLOT_OUTPUT).getCount() + output.getCount()));
    }

    private boolean hasRecipe() {
        Optional<RecipeHolder<RefineryRecipe>> recipe = getCurrentRecipe();

        if (recipe.isEmpty()) {
            return false;
        }

        ItemStack output = recipe.get().value().getResultItem(null);

        return canInsertAmountIntoOutputSlot(output.getCount()) && canInsertItemIntoOutputSlot(output);
    }
}
