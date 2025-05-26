package com.teamofpowersim.powersim.screen.arc_furnace;

import com.teamofpowersim.powersim.block.ModBlocks;
import com.teamofpowersim.powersim.block.machine.consumer.arc_furnace.ArcFurnaceBlockEntity;
import com.teamofpowersim.powersim.screen.AbstractModMenu;
import com.teamofpowersim.powersim.screen.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ArcFurnaceMenu extends AbstractModMenu {
    public ArcFurnaceMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(7));
    }


    public ArcFurnaceMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity, ContainerData containerData) {
        super(ModMenuTypes.ARC_FURNACE_MENU.get(), containerId, playerInventory, blockEntity, containerData);
        this.addSlot(new SlotItemHandler(((ArcFurnaceBlockEntity) blockEntity).itemHandler, 0, 56, 35));
        this.addSlot(new SlotItemHandler(((ArcFurnaceBlockEntity) blockEntity).itemHandler, 1, 116, 35));
    }

    public int getHeatProgress() {
        int heatLevel = this.data.get(2);

        return heatLevel;
    }

    public float getPowerProgress() {
        int powerLevel = this.data.get(4);
        int powerTotalLevel = this.data.get(5);

        return powerTotalLevel != 0 && powerLevel != 0 ? (float) powerLevel / (float) powerTotalLevel : 0.0F;
    }

    public float getSmeltingProgress() {
        int smeltingProgress = this.data.get(0);
        int smeltingTotalTime = this.data.get(1);

        return smeltingTotalTime != 0 && smeltingProgress != 0 ? (float) smeltingProgress / (float) smeltingTotalTime : 0.0F;
    }

    public boolean isSmelting() {return data.get(0) > 0;}

    @Override
    protected int getBlockEntityInventorySlotCount() {
        return 2;
    }

    @Override
    protected int getContainerDataCount() {
        return 7;
    }

    @Override
    protected Block getValidBlock() {
        return ModBlocks.ARC_FURNACE.get();
    }
}
