package com.teamofpowersim.powersim.block.machine.consumer.arc_furnace;

import com.mojang.logging.LogUtils;
import com.teamofpowersim.powersim.block.ModBlockEntityTypes;
import com.teamofpowersim.powersim.block.machine.consumer.AbstractPowerConsumerBlockEntity;
import com.teamofpowersim.powersim.recipe.ModRecipes;
import com.teamofpowersim.powersim.recipe.arc_furnace.ArcFurnaceRecipe;
import com.teamofpowersim.powersim.recipe.arc_furnace.ArcFurnaceRecipeInput;
import com.teamofpowersim.powersim.screen.arc_furnace.ArcFurnaceMenu;
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

public class ArcFurnaceBlockEntity extends AbstractPowerConsumerBlockEntity implements MenuProvider {
    private static final Logger LOGGER = LogUtils.getLogger();

    public final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            if (level != null && !level.isClientSide()) {
                setChanged();
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3); // Use 3 from example
            }
        }
    };

    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;

    // NBT Keys
    private static final String INVENTORY_KEY = "inventory";
    private static final String HEAT_LEVEL_KEY = "arc_furnace.heat_level";
    private static final String HEAT_TOTAL_LEVEL_KEY = "arc_furnace.heat_total_level";
    private static final String SMELTING_PROGRESS_KEY = "arc_furnace.smelting_progress";
    private static final String SMELTING_TOTAL_TIME_KEY = "arc_furnace.smelting_total_time";
    private static final String INTERNAL_RESISTANCE_KEY = "arc_furnace.internal_resistance";

    // Electrical Configuration
    private static final double FURNACE_MIN_OPERATING_VOLTAGE = 100.0;
    private static final double FURNACE_NOMINAL_OPERATING_CURRENT = 15.0;
    private static final double FURNACE_MAX_SAFE_CURRENT = 50.0;

    // Operational Parameters
    private static final int DEFAULT_TOTAL_SMELTING_TIME = 80;
    private static final int DEFAULT_INTERNAL_RESISTANCE = 20;

    private int heatLevel;
    private int heatTotalLevel = 7;
    private int smeltingProgress;
    private int smeltingTotalTime = DEFAULT_TOTAL_SMELTING_TIME;
    private int internalResistance = DEFAULT_INTERNAL_RESISTANCE;
    private final ContainerData data;

    // Timer for heating and cooling
    private int heatActivityTimer = 0;
    private static final int HEAT_TICK_INTERVAL = 10;

    public ArcFurnaceBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.ARC_FURNACE_BE.get(), pos, blockState);
        this.data = new ContainerData() {
            @Override
            public int get(int i) {
                return switch (i) {
                    case 0 -> ArcFurnaceBlockEntity.this.smeltingProgress;
                    case 1 -> ArcFurnaceBlockEntity.this.smeltingTotalTime;
                    case 2 -> ArcFurnaceBlockEntity.this.heatLevel;
                    case 3 -> ArcFurnaceBlockEntity.this.heatTotalLevel;
                    case 4 -> ArcFurnaceBlockEntity.this.getBufferCharge();
                    case 5 -> ArcFurnaceBlockEntity.this.internalResistance;
                    default -> 0;
                };
            }

            @Override
            public void set(int i, int value) {
                switch (i) {
                    case 0: ArcFurnaceBlockEntity.this.smeltingProgress = value; break;
                    case 1: ArcFurnaceBlockEntity.this.smeltingTotalTime = value; break;
                    case 2: ArcFurnaceBlockEntity.this.heatLevel = value; break;
                    case 3: ArcFurnaceBlockEntity.this.heatTotalLevel = value; break;
                    case 5: ArcFurnaceBlockEntity.this.internalResistance = value; break;
                }
            }

            @Override
            public int getCount() {
                return 6;
            }
        };
    }

    // Override electrical parameters
    @Override
    protected double getMinOperatingVoltage() { return FURNACE_MIN_OPERATING_VOLTAGE; }
    @Override
    protected double getNominalOperatingCurrent() { return FURNACE_NOMINAL_OPERATING_CURRENT; }
    @Override
    protected double getMaxSafeCurrent() { return FURNACE_MAX_SAFE_CURRENT; }
    @Override
    public int getResistance() { return this.internalResistance; }


    @Override
    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        boolean wasLit = blockState.getValue(ArcFurnaceBlock.LIT);
        boolean isLit = isLit();

        if (isPowered()) {
            heatUp();
        if (level.isClientSide) {
            return;
        }

        double actualCurrent = getSimCurrent();
        double actualVoltage = getSimVoltage();

        if (Math.abs(actualCurrent) > getMaxSafeCurrent()) {
            LOGGER.warn("Arc Furnace at {} OVERCURRENT! I: {:.2f}A > {:.2f}A. Destroying.", blockPos, actualCurrent, getMaxSafeCurrent());
            level.destroyBlock(blockPos, true);
            return;
        }

        boolean isPoweredThisTick = isConsideredPoweredByNetwork();

        if (isPoweredThisTick) {
            double powerInputWatts = Math.abs(actualVoltage * actualCurrent);
            heatUp(powerInputWatts);
        } else {
            coolDown();
        }

        if (hasRecipe() && isOutputSlotEmptyOrReceivable()) {
            if (isHeatedSufficientlyForSmelting() && isPoweredThisTick) {
                increaseSmeltingProgress();
                if (hasSmeltingFinished()) {
                    smeltItem();
                    resetProgress();
                }
            } else if (!isPoweredThisTick && this.smeltingProgress > 0) {
                resetProgress();
            }
        } else {
            resetProgress();
        }

        if (wasLit != isLit) {
            level.setBlockAndUpdate(blockPos, blockState.setValue(ArcFurnaceBlock.LIT, isLit));
            setChanged();
        }
    }

    private boolean isLit() {
        return isHeated() && isPowered();
    }

    private void increaseSmeltingProgress() {
        this.smeltingProgress++;
    }

    private boolean hasSmeltingFinished() {
        return this.smeltingProgress >= this.smeltingTotalTime;
    }

    private void smeltItem() {
        Optional<RecipeHolder<ArcFurnaceRecipe>> recipeOpt = getCurrentRecipe();
        if (recipeOpt.isEmpty()) {
            return;
        }
        RecipeHolder<ArcFurnaceRecipe> recipe = recipeOpt.get();
        ItemStack output = recipe.value().getResultItem(level.registryAccess());
        itemHandler.extractItem(SLOT_INPUT, 1, false);
        itemHandler.setStackInSlot(SLOT_OUTPUT, new ItemStack(output.getItem(),
                itemHandler.getStackInSlot(SLOT_OUTPUT).getCount() + output.getCount()));
    }

    private void resetProgress() {
        this.smeltingProgress = 0;
    }

    private boolean hasRecipe() {
        Optional<RecipeHolder<ArcFurnaceRecipe>> recipe = getCurrentRecipe();

        if (recipe.isEmpty()) {
            return false;
        }

        ItemStack output = recipe.get().value().getResultItem(null);

        return canInsertAmountIntoOutputSlot(output.getCount()) && canInsertItemIntoOutputSlot(output);
    }

    private boolean canInsertItemIntoOutputSlot(ItemStack output) { // From example structure
        return itemHandler.getStackInSlot(SLOT_OUTPUT).isEmpty() ||
                (ItemStack.isSameItem(itemHandler.getStackInSlot(SLOT_OUTPUT), output) && ItemStack.isSameItemSameComponents(itemHandler.getStackInSlot(SLOT_OUTPUT), output));
    }

    private boolean canInsertAmountIntoOutputSlot(int count) { // From example structure
        if (itemHandler.getStackInSlot(SLOT_OUTPUT).isEmpty()) return true;
        return itemHandler.getStackInSlot(SLOT_OUTPUT).getCount() + count <= itemHandler.getStackInSlot(SLOT_OUTPUT).getMaxStackSize();
    }

    private boolean isHeatedSufficientlyForSmelting() {
        return this.heatLevel >= this.heatTotalLevel - 2;
    }

    private void heatUp(double powerWatts) {
        heatActivityTimer++;
        if (heatActivityTimer >= HEAT_TICK_INTERVAL) {
            heatActivityTimer = 0;
            if (heatLevel < heatTotalLevel) {
                int heatIncrease = 1;
                if (Math.abs(getSimCurrent()) > getNominalOperatingCurrent() * 0.75) {
                    heatIncrease = 2;
                }
                heatLevel = Math.min(heatTotalLevel, heatLevel + heatIncrease);
            }
        }
    }

    private void coolDown() {
        heatActivityTimer++;
        if (heatActivityTimer >= HEAT_TICK_INTERVAL * 2) {
            heatActivityTimer = 0;
            if (heatLevel > 0) {
                heatLevel--;
            }
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound(INVENTORY_KEY));
        this.heatLevel = tag.getInt(HEAT_LEVEL_KEY);
        this.heatTotalLevel = tag.contains(HEAT_TOTAL_LEVEL_KEY) ? tag.getInt(HEAT_TOTAL_LEVEL_KEY) : 7;
        this.smeltingProgress = tag.getInt(SMELTING_PROGRESS_KEY);
        this.smeltingTotalTime = tag.contains(SMELTING_TOTAL_TIME_KEY) ? tag.getInt(SMELTING_TOTAL_TIME_KEY) : DEFAULT_TOTAL_SMELTING_TIME;
        this.internalResistance = tag.contains(INTERNAL_RESISTANCE_KEY) ? tag.getInt(INTERNAL_RESISTANCE_KEY) : DEFAULT_INTERNAL_RESISTANCE;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(INVENTORY_KEY, itemHandler.serializeNBT(registries));
        tag.putInt(HEAT_LEVEL_KEY, this.heatLevel);
        tag.putInt(HEAT_TOTAL_LEVEL_KEY, this.heatTotalLevel);
        tag.putInt(SMELTING_PROGRESS_KEY, this.smeltingProgress);
        tag.putInt(SMELTING_TOTAL_TIME_KEY, this.smeltingTotalTime);
        tag.putInt(INTERNAL_RESISTANCE_KEY, this.internalResistance); //
    }

    private Optional<RecipeHolder<ArcFurnaceRecipe>> getCurrentRecipe() {
        if (this.level == null || itemHandler.getStackInSlot(SLOT_INPUT).isEmpty()) {
            return Optional.empty();
        }
        return this.level.getRecipeManager().getRecipeFor(
                ModRecipes.ARC_FURNACE_TYPE.get(),
                new ArcFurnaceRecipeInput(itemHandler.getStackInSlot(SLOT_INPUT)),
                this.level
        );
    }

    private boolean isOutputSlotEmptyOrReceivable() {
        ItemStack currentOutput = itemHandler.getStackInSlot(SLOT_OUTPUT);
        if (currentOutput.isEmpty()) return true;
        return currentOutput.getCount() < currentOutput.getMaxStackSize() && currentOutput.getCount() < itemHandler.getSlotLimit(SLOT_OUTPUT);
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
        return Component.translatable("block.powersim.arc_furnace");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ArcFurnaceMenu(containerId, playerInventory, this, this.data);
    }

    public void clearContents() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
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
        if (state.hasProperty(ArcFurnaceBlock.FACING)) {
            Direction facing = state.getValue(ArcFurnaceBlock.FACING);
            return faceAccessed != facing && faceAccessed != facing.getOpposite();
        }
        return true;
    }
}