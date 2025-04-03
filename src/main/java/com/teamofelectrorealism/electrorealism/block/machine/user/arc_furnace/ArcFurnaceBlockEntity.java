package com.teamofelectrorealism.electrorealism.block.machine.user.arc_furnace;

import com.teamofelectrorealism.electrorealism.api.ElectricalAPI;
import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.block.machine.user.AbstractPowerUserBlockEntity;
import com.teamofelectrorealism.electrorealism.recipe.ModRecipes;
import com.teamofelectrorealism.electrorealism.recipe.arc_furnace.ArcFurnaceRecipe;
import com.teamofelectrorealism.electrorealism.recipe.arc_furnace.ArcFurnaceRecipeInput;
import com.teamofelectrorealism.electrorealism.screen.arc_furnace.ArcFurnaceMenu;
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

public class ArcFurnaceBlockEntity extends AbstractPowerUserBlockEntity implements MenuProvider {
    public final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            if (!level.isClientSide()) {
                setChanged();
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2);
            }
        }
    };

    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;

    private static final String INVENTORY_KEY = "inventory";
    private static final String HEAT_LEVEL_KEY = "arc_furnace.heat_level";
    private static final String BUFFER_LEVEL_KEY = "arc_furnace.buffer_level";
    private static final String BUFFER_TOTAL_LEVEL_KEY = "arc_furnace.buffer_total_level";
    private static final String HEAT_TOTAL_LEVEL_KEY = "arc_furnace.heat_total_level";
    private static final String SMELTING_PROGRESS_KEY = "arc_furnace.smelting_progress";
    private static final String SMELTING_TOTAL_TIME_KEY = "arc_furnace.smelting_total_time";
    private static final String INTERNAL_RESISTANCE_KEY = "arc_furnace.internal_resistance";

    private static final int TOTAL_SMELTING_TIME = 80;
    private static final int TOTAL_BUFFER_CAPASITY = 2000; // In mAh
    private static final int INTERNAL_RESISTANCE = 10; // In ohm

    private int heatLevel;
    private int heatTotalLevel = 7;
    private int smeltingProgress;
    private int smeltingTotalTime = TOTAL_SMELTING_TIME;
    private int bufferLevel;
    private int bufferTotalLevel = TOTAL_BUFFER_CAPASITY;
    private int internalResistance = INTERNAL_RESISTANCE;
    private final ContainerData data;

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
                    case 4 -> ArcFurnaceBlockEntity.this.bufferLevel;
                    case 5 -> ArcFurnaceBlockEntity.this.bufferTotalLevel;
                    case 6 -> ArcFurnaceBlockEntity.this.internalResistance;

                    default -> 0;
                };
            }

            @Override
            public void set(int i, int i1) {
                switch (i) {
                    case 0: ArcFurnaceBlockEntity.this.smeltingProgress = i;
                    case 1: ArcFurnaceBlockEntity.this.smeltingTotalTime = i;
                    case 2: ArcFurnaceBlockEntity.this.heatLevel = i;
                    case 3: ArcFurnaceBlockEntity.this.heatTotalLevel = i;
                    case 4: ArcFurnaceBlockEntity.this.bufferLevel = i;
                    case 5: ArcFurnaceBlockEntity.this.bufferTotalLevel = i;
                    case 6: ArcFurnaceBlockEntity.this.internalResistance = i;
                }
            }

            @Override
            public int getCount() {
                return 7;
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
        return Component.translatable("block.electrorealism.arc_furnace");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ArcFurnaceMenu(containerId, playerInventory, this, this.data);
    }

    @Override
    public void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
        super.loadAdditional(compoundTag, registries);
        itemHandler.deserializeNBT(registries, compoundTag.getCompound(INVENTORY_KEY));
        heatLevel = compoundTag.getInt(HEAT_LEVEL_KEY);
        heatTotalLevel = compoundTag.getInt(HEAT_TOTAL_LEVEL_KEY);
        smeltingProgress = compoundTag.getInt(SMELTING_PROGRESS_KEY);
        smeltingTotalTime = compoundTag.getInt(SMELTING_TOTAL_TIME_KEY);
        bufferLevel = compoundTag.getInt(BUFFER_LEVEL_KEY);
        bufferTotalLevel = compoundTag.getInt(BUFFER_TOTAL_LEVEL_KEY);
        internalResistance = compoundTag.getInt(INTERNAL_RESISTANCE_KEY);
    }
    @Override
    public void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
        compoundTag.put(INVENTORY_KEY, itemHandler.serializeNBT(registries));
        compoundTag.putInt(HEAT_LEVEL_KEY, heatLevel);
        compoundTag.putInt(HEAT_TOTAL_LEVEL_KEY, heatTotalLevel);
        compoundTag.putInt(SMELTING_PROGRESS_KEY, smeltingProgress);
        compoundTag.putInt(SMELTING_TOTAL_TIME_KEY, smeltingTotalTime);
        compoundTag.putInt(BUFFER_LEVEL_KEY, bufferLevel);
        compoundTag.putInt(BUFFER_TOTAL_LEVEL_KEY, bufferTotalLevel);
        compoundTag.putInt(INTERNAL_RESISTANCE_KEY, internalResistance);

        super.saveAdditional(compoundTag, registries);
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

    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        if (isPowered()) {
            heatUp();
        } else {
            coolDown();
        }

        if (hasRecipe() && isOutputSlotEmptyOrReceivable()) {
            if (isHeated() && hasBufferEnoughCharge()) {
                increaseSmeltingProgress();
                decreaseBufferCharge();
                if (hasSmeltingFinished()) {
                    smeltItem();
                    resetProgress();
                }
            } else {
                smeltingProgress = 0;
            }
        } else {
            smeltingProgress = 0;
        }
    }

    @Override
    public boolean isFaceAllowed(BlockState state, Direction faceAccessed) {
        return false;
    }

    private boolean hasBufferEnoughCharge() {
        return bufferLevel >= 100;
    }

    private void decreaseBufferCharge() {
        bufferLevel = bufferLevel - 3;
    }

    private boolean hasRecipe() {
        Optional<RecipeHolder<ArcFurnaceRecipe>> recipe = getCurrentRecipe();

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

    private Optional<RecipeHolder<ArcFurnaceRecipe>> getCurrentRecipe() {
        return this.level.getRecipeManager().getRecipeFor(ModRecipes.ARC_FURNACE_TYPE.get(), new ArcFurnaceRecipeInput(itemHandler.getStackInSlot(SLOT_INPUT)), level);
    }

    private boolean isOutputSlotEmptyOrReceivable() {
        return this.itemHandler.getStackInSlot(SLOT_OUTPUT).isEmpty() || this.itemHandler.getStackInSlot(SLOT_OUTPUT).getCount() < this.itemHandler.getStackInSlot(SLOT_OUTPUT).getMaxStackSize();
    }

    private void resetProgress() {
        this.smeltingProgress = 0;
        this.smeltingTotalTime = TOTAL_SMELTING_TIME;
    }

    private void smeltItem() {
        Optional<RecipeHolder<ArcFurnaceRecipe>> recipe = getCurrentRecipe();
        ItemStack output = recipe.get().value().getResultItem(null);

        itemHandler.extractItem(SLOT_INPUT, 1, false);

        itemHandler.setStackInSlot(SLOT_OUTPUT, new ItemStack(output.getItem(), itemHandler.getStackInSlot(SLOT_OUTPUT).getCount() + output.getCount()));
    }

    private boolean hasSmeltingFinished() {
        return this.smeltingProgress >= this.smeltingTotalTime;
    }

    private void increaseSmeltingProgress() {
        smeltingProgress++;
    }

    private void increaseHeatingProgress() {
        if (heatLevel < 7) {
            heatLevel++;
        }
    }

    // Timer for heating
    private int heatUpTimer = 0;

    private void heatUp() {
        if (heatUpTimer < 160) { // 8 seconds at 20 ticks/second
            heatUpTimer++;
            if (heatUpTimer % 20 == 0) {
                increaseHeatingProgress();
            }
        } else {
            // Stay fully heated after 8 seconds
            heatLevel = heatTotalLevel;
        }
    }

    @Override
    public int getResistance() {
        return internalResistance;
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

    private void coolDown() {
        if (heatLevel > 0) {
            heatLevel --; // Cool down slowly
        }
    }

    private boolean isHeated() {
        return this.heatLevel > 6;
    }

    private boolean isPowered() {
        return this.bufferLevel > 0;
    }
}