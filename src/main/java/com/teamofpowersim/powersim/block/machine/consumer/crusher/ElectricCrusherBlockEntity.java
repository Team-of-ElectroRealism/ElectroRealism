package com.teamofpowersim.powersim.block.machine.consumer.crusher;

import com.mojang.logging.LogUtils;
import com.teamofpowersim.powersim.block.ModBlockEntityTypes;
import com.teamofpowersim.powersim.block.machine.consumer.AbstractPowerConsumerBlockEntity;
import com.teamofpowersim.powersim.recipe.ModRecipes;
import com.teamofpowersim.powersim.recipe.crusher.ElectricCrusherRecipe;
import com.teamofpowersim.powersim.recipe.crusher.ElectricCrusherRecipeInput;
import com.teamofpowersim.powersim.screen.crusher.ElectricCrusherMenu;
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

public class ElectricCrusherBlockEntity extends AbstractPowerConsumerBlockEntity implements MenuProvider {
    private static final Logger LOGGER = LogUtils.getLogger();

    public final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            if (level != null && !level.isClientSide()) {
                setChanged();
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;

    // NBT Keys
    private static final String INVENTORY_KEY = "inventory";
    private static final String CRUSHING_PROGRESS_KEY = "electric_crusher.crushing_progress";
    private static final String CRUSHING_TOTAL_TIME_KEY = "electric_crusher.crushing_total_time";
    private static final String INTERNAL_RESISTANCE_KEY = "electric_crusher.internal_resistance";

    private static final double CRUSHER_MIN_OPERATING_VOLTAGE = 70.0;
    private static final double CRUSHER_NOMINAL_OPERATING_CURRENT = 8.0;
    private static final double CRUSHER_MAX_SAFE_CURRENT = 20.0;

    private static final int DEFAULT_TOTAL_CRUSHING_TIME = 100;
    private static final int DEFAULT_INTERNAL_RESISTANCE = 25;

    private int crushingProgress;
    private int crushingTotalTime = DEFAULT_TOTAL_CRUSHING_TIME;
    private int internalResistance = DEFAULT_INTERNAL_RESISTANCE;
    private final ContainerData data;

    public ElectricCrusherBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.ELECTRIC_CRUSHER_BE.get(), pos, blockState);
        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> ElectricCrusherBlockEntity.this.crushingProgress;
                    case 1 -> ElectricCrusherBlockEntity.this.crushingTotalTime;
                    case 2 -> ElectricCrusherBlockEntity.this.getBufferCharge();
                    case 3 -> ElectricCrusherBlockEntity.this.internalResistance;
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0: ElectricCrusherBlockEntity.this.crushingProgress = pValue; break;
                    case 1: ElectricCrusherBlockEntity.this.crushingTotalTime = pValue; break;
                    case 3: ElectricCrusherBlockEntity.this.internalResistance = pValue; break;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    @Override
    protected double getMinOperatingVoltage() { return CRUSHER_MIN_OPERATING_VOLTAGE; }
    @Override
    protected double getNominalOperatingCurrent() { return CRUSHER_NOMINAL_OPERATING_CURRENT; }
    @Override
    protected double getMaxSafeCurrent() { return CRUSHER_MAX_SAFE_CURRENT; }
    @Override
    public int getResistance() { return this.internalResistance; }

    @Override
    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        if (level.isClientSide) {
            return;
        }

        boolean wasCrushing = blockState.getValue(ElectricCrusherBlock.CRUSHING);
        boolean isCrushing = isCrushing();

        // Overcurrent check
        double actualCurrent = getSimCurrent();
        if (Math.abs(actualCurrent) > getMaxSafeCurrent()) {
            LOGGER.warn("Electric Crusher at {} OVERCURRENT! I: {:.2f}A > {:.2f}A. Destroying.", blockPos, actualCurrent, getMaxSafeCurrent());
            level.destroyBlock(blockPos, true);
            return;
        }

        if (hasRecipe() && isOutputSlotEmptyOrReceivable()) {
            if (isConsideredPoweredByNetwork()) {
                increaseCrushingProgress();
                if (hasCrushingFinished()) {
                    crushItem();
                    resetProgress();
                }
            } else {
                resetProgress();
            }
        } else {
            resetProgress();
        }

        if (wasCrushing != isCrushing) {
            level.setBlockAndUpdate(blockPos, blockState.setValue(ElectricCrusherBlock.CRUSHING, isCrushing));
            setChanged();
        }
    }

    private boolean isCrushing() {
        return this.crushingProgress > 0;
    }

    private void increaseCrushingProgress() {
        crushingProgress++;
    }

    private boolean hasCrushingFinished() {
        return this.crushingProgress >= this.crushingTotalTime;
    }

    private void crushItem() {
        Optional<RecipeHolder<ElectricCrusherRecipe>> recipeOpt = getCurrentRecipe();
        if (recipeOpt.isEmpty()) {
            return;
        }
        RecipeHolder<ElectricCrusherRecipe> recipe = recipeOpt.get();
        ItemStack output = recipe.value().getResultItem(level.registryAccess());

        itemHandler.extractItem(SLOT_INPUT, 1, false);
        itemHandler.setStackInSlot(SLOT_OUTPUT, new ItemStack(output.getItem(),
                itemHandler.getStackInSlot(SLOT_OUTPUT).getCount() + output.getCount()));
    }

    private void resetProgress() {
        this.crushingProgress = 0;
    }

    private boolean hasRecipe() {
        Optional<RecipeHolder<ElectricCrusherRecipe>> recipe = getCurrentRecipe();

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
        if (itemHandler.getStackInSlot(SLOT_OUTPUT).isEmpty()) return true;
        return itemHandler.getStackInSlot(SLOT_OUTPUT).getCount() + count <= itemHandler.getStackInSlot(SLOT_OUTPUT).getMaxStackSize();
    }

    private Optional<RecipeHolder<ElectricCrusherRecipe>> getCurrentRecipe() {
        return this.level.getRecipeManager().getRecipeFor(ModRecipes.ELECTRIC_CRUSHER_TYPE.get(), new ElectricCrusherRecipeInput(itemHandler.getStackInSlot(SLOT_INPUT)), level);
    }

    private boolean isOutputSlotEmptyOrReceivable() {
        return this.itemHandler.getStackInSlot(SLOT_OUTPUT).isEmpty() || this.itemHandler.getStackInSlot(SLOT_OUTPUT).getCount() < this.itemHandler.getStackInSlot(SLOT_OUTPUT).getMaxStackSize();
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound(INVENTORY_KEY));
        // bufferLevel and bufferTotalLevel loading removed
        this.crushingProgress = tag.getInt(CRUSHING_PROGRESS_KEY);
        this.crushingTotalTime = tag.contains(CRUSHING_TOTAL_TIME_KEY) ? tag.getInt(CRUSHING_TOTAL_TIME_KEY) : DEFAULT_TOTAL_CRUSHING_TIME;
        this.internalResistance = tag.contains(INTERNAL_RESISTANCE_KEY) ? tag.getInt(INTERNAL_RESISTANCE_KEY) : DEFAULT_INTERNAL_RESISTANCE;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(INVENTORY_KEY, itemHandler.serializeNBT(registries));
        tag.putInt(CRUSHING_PROGRESS_KEY, this.crushingProgress);
        tag.putInt(CRUSHING_TOTAL_TIME_KEY, this.crushingTotalTime);
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
        return Component.translatable("block.powersim.electric_crusher");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ElectricCrusherMenu(containerId, playerInventory, this, this.data);
    }

    public void clearContents() { // Method from example
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    public void drops() { // Method from example, adapted from uploaded
        if (this.level == null) return;
        SimpleContainer inv = new SimpleContainer(itemHandler.getSlots());
        for(int i = 0; i < itemHandler.getSlots(); i++) {
            inv.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inv);
    }

    @Override
    public boolean isFaceAllowed(BlockState state, Direction faceAccessed) { // Method from example
        // Only allow horizontal faces.
        return faceAccessed.getAxis().isHorizontal();
    }

    // IVoltageConsumer methods receiveVoltage, setBufferCharge, getBufferCharge (actual buffer one) removed
    // isPowered(), hasBufferEnoughCharge(), decreaseBufferCharge() removed
}