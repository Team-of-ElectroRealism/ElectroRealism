package com.teamofelectrorealism.electrorealism.block.arc_furnace;

import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.recipe.ModRecipes;
import com.teamofelectrorealism.electrorealism.recipe.arc_furnace.ArcFurnaceRecipe;
import com.teamofelectrorealism.electrorealism.recipe.arc_furnace.ArcFurnaceRecipeInput;
import com.teamofelectrorealism.electrorealism.screen.arc_furnace.ArcFurnaceMenu;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ArcFurnaceBlockEntity extends BlockEntity implements MenuProvider {
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
    private static final String HEAT_LEVEL_KEY = "arc_furnace.heat_level";
    private static final String HEAT_TOTAL_LEVEL_KEY = "arc_furnace.heat_total_level";
    private static final String SMELTING_PROGRESS_KEY = "arc_furnace.smelting_progress";
    private static final String SMELTING_TOTAL_TIME_KEY = "arc_furnace.smelting_total_time";

    private static final int TOTAL_SMELTING_TIME = 200;

    private int heatLevel;
    private int heatTotalLevel = 7;
    private int smeltingProgress;
    private int smeltingTotalTime = TOTAL_SMELTING_TIME;
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
                }
            }

            @Override
            public int getCount() {
                return 4;
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
    }
    @Override
    public void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
        compoundTag.put(INVENTORY_KEY, itemHandler.serializeNBT(registries));
        compoundTag.putInt(HEAT_LEVEL_KEY, heatLevel);
        compoundTag.putInt(HEAT_TOTAL_LEVEL_KEY, heatTotalLevel);
        compoundTag.putInt(SMELTING_PROGRESS_KEY, smeltingProgress);
        compoundTag.putInt(SMELTING_TOTAL_TIME_KEY, smeltingTotalTime);

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
        testHeating();

        if (hasRecipe() && isOutputSlotEmptyOrReceivable()) {
            if (isHeated()) {
                increaseSmeltingProgress();
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
        heatLevel++;
    }

    // Timer for heating
    private int heatUpTimer = 0;

    private void testHeating() {
        if (!itemHandler.getStackInSlot(SLOT_INPUT).isEmpty()) {
            // If heating up, increase the timer
            if (heatUpTimer < 160) { // 8 seconds at 20 ticks/second
                heatUpTimer++;
                heatLevel = (int) ((float) heatUpTimer / 160 * heatTotalLevel); // Smooth increase
            } else {
                // Stay fully heated after 8 seconds
                heatLevel = heatTotalLevel;
            }
        } else {
            // Cool down when no item is in slot
            if (heatLevel > 0) {
                heatLevel -= 1; // Cool down slowly
            }
            heatUpTimer = 0;
        }

        // Sync to client
        setChanged();
        if (!level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    private void testHeat() {
        heatLevel = 1;
    }

    private boolean isHeated() {
        return this.heatLevel > 6;
    }
}