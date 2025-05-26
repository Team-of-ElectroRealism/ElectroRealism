package com.teamofpowersim.powersim.screen.crusher;

import com.teamofpowersim.powersim.block.ModBlocks;
import com.teamofpowersim.powersim.block.machine.consumer.crusher.ElectricCrusherBlockEntity;
import com.teamofpowersim.powersim.screen.AbstractModMenu;
import com.teamofpowersim.powersim.screen.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ElectricCrusherMenu extends AbstractModMenu {
    public ElectricCrusherMenu(int containerId , Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(4));
    }

    public ElectricCrusherMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity, ContainerData containerData) {
        super(ModMenuTypes.ELECTRIC_CRUSHER_MENU.get(), containerId, playerInventory, blockEntity, containerData);
        this.addSlot(new SlotItemHandler(((ElectricCrusherBlockEntity) blockEntity).itemHandler, 0, 56, 35));
        this.addSlot(new SlotItemHandler(((ElectricCrusherBlockEntity) blockEntity).itemHandler, 1, 116, 35));
    }

    /**
     * Returns 1.0f if powered, 0.0f if not.
     * Used by the screen to determine if the power icon should be fully shown or not at all.
     */
    public float getPowerDisplayStatus() {
        // isPowered is at data index 2, returns 0 or 1
        return this.data.get(2) == 1 ? 1.0f : 0.0f;
    }

    public float getCrushingProgress() {
        int crushingProgress = this.data.get(0);
        int crushingTotalTime = this.data.get(1);

        if (crushingTotalTime == 0) return 0.0F;
        return Math.min(1.0f, (float) crushingProgress / crushingTotalTime);
    }

    public boolean isCrushing() {
        // Crushing if progress > 0 AND powered
        return data.get(0) > 0 && (data.get(2) == 1);
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
        return ModBlocks.ELECTRIC_CRUSHER.get();
    }
}
