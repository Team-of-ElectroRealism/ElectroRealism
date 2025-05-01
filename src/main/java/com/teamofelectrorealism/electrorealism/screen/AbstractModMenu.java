package com.teamofelectrorealism.electrorealism.screen;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractModMenu extends AbstractContainerMenu {
    protected final BlockEntity blockEntity;
    protected final Level level;
    protected final ContainerData data;

    protected AbstractModMenu(@Nullable MenuType<?> menuType, int containerId, Inventory playerInventory, BlockEntity blockEntity, ContainerData containerData) {
        super(menuType, containerId);

        checkContainerSize(playerInventory, getBlockEntityInventorySlotCount());
        checkContainerDataCount(containerData, getContainerDataCount());

        this.blockEntity = blockEntity;
        this.level = playerInventory.player.level();
        this.data = containerData;

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addDataSlots(containerData);
    }

    /**
     * @return The number of slots belonging to the Tile Entity inventory.
     */
    protected abstract int getBlockEntityInventorySlotCount();

    /**
     * @return The expected number of integers in the ContainerData.
     */
    protected abstract int getContainerDataCount();

    /**
     * @return The Block associated with this menu, used for the stillValid check.
     */
    protected abstract Block getValidBlock();


    // CREDIT GOES TO: diesieben07 | https://github.com/diesieben07/SevenCommons
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    /**
     * Handles shift-clicking items between the player's inventory and the block entity's inventory.
     *
     * @param playerIn The player performing the shift-click.
     * @param pIndex   The index of the slot clicked.
     * @return An ItemStack representing the remaining items after the transfer, or ItemStack.EMPTY if none.
     */
    @Override
    public ItemStack quickMoveStack(Player playerIn, int pIndex) {
        Slot sourceSlot = slots.get(pIndex);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        int teInventorySlotCount = getBlockEntityInventorySlotCount();

        // Check if the slot clicked is one of the vanilla container slots
        if (pIndex >= VANILLA_FIRST_SLOT_INDEX && pIndex < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // This is a vanilla container slot so merge the stack into the tile inventory
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX + teInventorySlotCount, false)) {
                return ItemStack.EMPTY;
            }
        } else if (pIndex >= TE_INVENTORY_FIRST_SLOT_INDEX && pIndex < TE_INVENTORY_FIRST_SLOT_INDEX + teInventorySlotCount) {
            // This is a TE slot so merge the stack into the player's inventory
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            System.err.println("Invalid slotIndex:" + pIndex + " for block entity menu."); // Use System.err for errors
            return ItemStack.EMPTY;
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, getValidBlock());
    }

    protected void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < PLAYER_INVENTORY_ROW_COUNT; ++i) {
            for (int l = 0; l < PLAYER_INVENTORY_COLUMN_COUNT; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * PLAYER_INVENTORY_COLUMN_COUNT + HOTBAR_SLOT_COUNT, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    protected void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < HOTBAR_SLOT_COUNT; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}
