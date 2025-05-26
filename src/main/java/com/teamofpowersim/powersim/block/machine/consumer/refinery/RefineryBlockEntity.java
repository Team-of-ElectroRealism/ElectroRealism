package com.teamofpowersim.powersim.block.machine.consumer.refinery;

import com.mojang.logging.LogUtils;
import com.teamofpowersim.powersim.block.ModBlockEntityTypes;
import com.teamofpowersim.powersim.block.machine.consumer.AbstractPowerConsumerBlockEntity;
import com.teamofpowersim.powersim.recipe.ModRecipes;
import com.teamofpowersim.powersim.recipe.refinery.RefineryRecipe;
import com.teamofpowersim.powersim.recipe.refinery.RefineryRecipeInput;
import com.teamofpowersim.powersim.screen.refinery.RefineryMenu;
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
import org.slf4j.Logger;

import java.util.Optional;

public class RefineryBlockEntity extends AbstractPowerConsumerBlockEntity implements MenuProvider {
    private static final Logger LOGGER = LogUtils.getLogger();

    public final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            if (level != null && !level.isClientSide()) { // Adapted from example
                setChanged();
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;

    // NBT Keys
    private static final String INVENTORY_KEY = "inventory";
    private static final String REFINING_PROGRESS_KEY = "refinery.refining_progress";
    private static final String REFINING_TOTAL_TIME_KEY = "refinery.refining_total_time";
    private static final String INTERNAL_RESISTANCE_KEY = "refinery.internal_resistance";

    // Electrical Configuration
    private static final double REFINERY_MIN_OPERATING_VOLTAGE = 90.0;
    private static final double REFINERY_NOMINAL_OPERATING_CURRENT = 12.0;
    private static final double REFINERY_MAX_SAFE_CURRENT = 25.0;

    // Operational Parameters
    private static final int DEFAULT_TOTAL_REFINING_TIME = 150;
    private static final int DEFAULT_INTERNAL_RESISTANCE = 22;

    private int refiningProgress;
    private int refiningTotalTime = DEFAULT_TOTAL_REFINING_TIME;
    private int internalResistance = DEFAULT_INTERNAL_RESISTANCE;
    private final ContainerData data;

    public RefineryBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.REFINERY_BE.get(), pos, blockState);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> RefineryBlockEntity.this.refiningProgress;
                    case 1 -> RefineryBlockEntity.this.refiningTotalTime;
                    case 2 -> RefineryBlockEntity.this.getBufferCharge();
                    case 3 -> RefineryBlockEntity.this.internalResistance;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0: RefineryBlockEntity.this.refiningProgress = value; break;
                    case 1: RefineryBlockEntity.this.refiningTotalTime = value; break;
                    case 3: RefineryBlockEntity.this.internalResistance = value; break;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    // Override electrical parameters
    @Override
    protected double getMinOperatingVoltage() { return REFINERY_MIN_OPERATING_VOLTAGE; }
    @Override
    protected double getNominalOperatingCurrent() { return REFINERY_NOMINAL_OPERATING_CURRENT; }
    @Override
    protected double getMaxSafeCurrent() { return REFINERY_MAX_SAFE_CURRENT; }
    @Override
    public int getResistance() { return this.internalResistance; }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) { // Arguments from uploaded file
        if (level.isClientSide()) {
            return;
        }

        boolean wasRefining = state.getValue(RefineryBlock.REFINING);
        boolean isRefining = isRefining();

        double actualCurrent = getSimCurrent();
        if (Math.abs(actualCurrent) > getMaxSafeCurrent()) {
            LOGGER.warn("Refinery at {} OVERCURRENT! I: {:.2f}A > {:.2f}A. Destroying.", pos, actualCurrent, getMaxSafeCurrent());
            level.destroyBlock(pos, true);
            return;
        }

        if (hasRecipe() && isOutputSlotEmptyOrReceivable()) {
            if (isConsideredPoweredByNetwork()) {
                increaseRefiningProgress();
                if (hasRefiningFinished()) {
                    refineItem();
                    resetProgress();
                }
            } else if (this.refiningProgress > 0) {
                resetProgress();
            }
        } else {
            resetProgress();
        }

        if (wasRefining != isRefining) {
            level.setBlockAndUpdate(pos, state.setValue(RefineryBlock.REFINING, isRefining));
            setChanged();
        }
    }

    private boolean isRefining() {
        return this.refiningProgress > 0;
    }

    public void clearContents() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    private void increaseRefiningProgress() {
        this.refiningProgress++;
    }

    private boolean hasRefiningFinished() {
        return this.refiningProgress >= this.refiningTotalTime;
    }

    private void refineItem() {
        Optional<RecipeHolder<RefineryRecipe>> recipeOpt = getCurrentRecipe();
        if (recipeOpt.isEmpty()){
            return;
        }
        RecipeHolder<RefineryRecipe> recipe = recipeOpt.get();
        ItemStack output = recipe.value().getResultItem(level.registryAccess());
        itemHandler.extractItem(SLOT_INPUT, 1, false);
        itemHandler.setStackInSlot(SLOT_OUTPUT, new ItemStack(output.getItem(),
                itemHandler.getStackInSlot(SLOT_OUTPUT).getCount() + output.getCount()));
    }

    private void resetProgress() {
        this.refiningProgress = 0;
    }

    private boolean hasRecipe() {
        Optional<RecipeHolder<RefineryRecipe>> recipe = getCurrentRecipe();

        if (recipe.isEmpty()) {
            return false;
        }

        ItemStack output = recipe.get().value().getResultItem(null);

        return canInsertAmountIntoOutputSlot(output.getCount()) && canInsertItemIntoOutputSlot(output);
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

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound(INVENTORY_KEY));
        this.refiningProgress = tag.getInt(REFINING_PROGRESS_KEY);
        this.refiningTotalTime = tag.contains(REFINING_TOTAL_TIME_KEY) ? tag.getInt(REFINING_TOTAL_TIME_KEY) : DEFAULT_TOTAL_REFINING_TIME;
        this.internalResistance = tag.contains(INTERNAL_RESISTANCE_KEY) ? tag.getInt(INTERNAL_RESISTANCE_KEY) : DEFAULT_INTERNAL_RESISTANCE;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(INVENTORY_KEY, itemHandler.serializeNBT(registries));
        tag.putInt(REFINING_PROGRESS_KEY, this.refiningProgress);
        tag.putInt(REFINING_TOTAL_TIME_KEY, this.refiningTotalTime);
        tag.putInt(INTERNAL_RESISTANCE_KEY, this.internalResistance);
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
        return Component.translatable("block.powersim.refinery");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RefineryMenu(containerId, playerInventory, this, this.data);
    }

    public void drops() {
        if (this.level == null) return;
        SimpleContainer inv = new SimpleContainer(itemHandler.getSlots());
        for(int i = 0; i < itemHandler.getSlots(); i++) {
            inv.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inv);
    }

    @Override
    public boolean isFaceAllowed(BlockState state, Direction faceAccessed) {
        return true;
    }
}