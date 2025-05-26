package com.teamofpowersim.powersim.screen.arc_furnace;

import com.teamofpowersim.powersim.block.ModBlocks;
import com.teamofpowersim.powersim.block.machine.consumer.arc_furnace.ArcFurnaceBlockEntity;
import com.teamofpowersim.powersim.screen.AbstractModMenu;
import com.teamofpowersim.powersim.screen.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ArcFurnaceMenu extends AbstractModMenu {
    public ArcFurnaceMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(6));
    }

    public ArcFurnaceMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity, ContainerData containerData) {
        super(ModMenuTypes.ARC_FURNACE_MENU.get(), containerId, playerInventory, blockEntity, containerData);
        this.addSlot(new SlotItemHandler(((ArcFurnaceBlockEntity) blockEntity).itemHandler, 0, 56, 35));
        this.addSlot(new SlotItemHandler(((ArcFurnaceBlockEntity) blockEntity).itemHandler, 1, 116, 35));
    }

    public int getHeatLevel() { // Changed from getHeatProgress to return raw level
        return this.data.get(2);
    }

    public int getMaxHeatLevel() { // Added getter for max heat
        return this.data.get(3);
    }

    /**
     * Returns 1.0f if powered, 0.0f if not.
     * Used by the screen to determine if the power icon should be fully shown or not at all.
     */
    public float getPowerDisplayStatus() {
        // isPowered is at data index 4, returns 0 or 1
        return this.data.get(4) == 1 ? 1.0f : 0.0f;
    }

    public float getSmeltingProgress() {
        int smeltingProgress = this.data.get(0);
        int smeltingTotalTime = this.data.get(1);

        if (smeltingTotalTime == 0) return 0.0F;
        return Math.min(1.0f, (float) smeltingProgress / smeltingTotalTime);
    }

    public boolean isSmelting() {
        return data.get(0) > 0 && (data.get(4) == 1);
    }

    @Override
    protected int getBlockEntityInventorySlotCount() {
        return 2;
    }

    @Override
    protected int getContainerDataCount() {
        return 6;
    }

    @Override
    protected Block getValidBlock() {
        return ModBlocks.ARC_FURNACE.get();
    }
}
