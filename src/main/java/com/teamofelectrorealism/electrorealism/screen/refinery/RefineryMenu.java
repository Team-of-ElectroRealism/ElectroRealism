package com.teamofelectrorealism.electrorealism.screen.refinery;

import com.teamofelectrorealism.electrorealism.block.ModBlocks;
import com.teamofelectrorealism.electrorealism.block.machine.consumer.refinery.RefineryBlockEntity;
import com.teamofelectrorealism.electrorealism.screen.AbstractModMenu;
import com.teamofelectrorealism.electrorealism.screen.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class RefineryMenu extends AbstractModMenu {
    public RefineryMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(5));
    }

    public RefineryMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity, ContainerData containerData) {
        super(ModMenuTypes.REFINERY_MENU.get(), containerId, playerInventory, blockEntity, containerData);
        this.addSlot(new SlotItemHandler(((RefineryBlockEntity) blockEntity).itemHandler, 0, 56, 17));
        this.addSlot(new SlotItemHandler(((RefineryBlockEntity) blockEntity).itemHandler, 1, 116, 35));
    }

    @Override
    protected int getBlockEntityInventorySlotCount() {
        return 2;
    }

    @Override
    protected int getContainerDataCount() {
        return 5;
    }

    @Override
    protected Block getValidBlock() {
        return ModBlocks.REFINERY.get();
    }

    public float getPowerProgress() {
        int powerLevel = this.data.get(2);
        int powerTotalLevel = this.data.get(3);

        if (powerTotalLevel <= 0) {
            System.out.println("Potential division by zero detected!");
            return 0;
        }
        return Math.min(1.0f, (float) powerLevel / powerTotalLevel);
    }

    public float getRefiningProgress() {
        int refiningProgress = this.data.get(0);
        int refiningTotalProgress = this.data.get(1);

        if (refiningTotalProgress <= 0) {
            System.out.println("Potential division by zero detected!");
            return 0;
        }
        return Math.min(1.0f, (float) refiningProgress / refiningTotalProgress);
    }

    public boolean isRefining() {
        return data.get(0) > 0;
    }
}
