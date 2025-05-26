package com.teamofpowersim.powersim.screen.crusher;

import com.teamofpowersim.powersim.block.ModBlocks;
import com.teamofpowersim.powersim.block.machine.consumer.crusher.ElectricCrusherBlockEntity;
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

public class ElectricCrusherMenu extends AbstractModMenu {
    public ElectricCrusherMenu(int containerId , Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(5));
    }

    public ElectricCrusherMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity, ContainerData containerData) {
        super(ModMenuTypes.ELECTRIC_CRUSHER_MENU.get(), containerId, playerInventory, blockEntity, containerData);
        this.addSlot(new SlotItemHandler(((ElectricCrusherBlockEntity) blockEntity).itemHandler, 0, 56, 35));
        this.addSlot(new SlotItemHandler(((ElectricCrusherBlockEntity) blockEntity).itemHandler, 1, 116, 35));
    }

    public float getPowerProgress() {
        int powerLevel = this.data.get(2);
        int powerTotalLevel = this.data.get(3);

        if (powerTotalLevel == 0) {
            System.out.println("Potential division by zero detected! powerTotalLevel in Crusher is 0. Returning 0 as power progress.");
            return 0f;
        }

        return (float) powerLevel / powerTotalLevel;
    }
    public float getCrushingProgress() {
        int crushingProgress = this.data.get(0);
        int crushingTotalTime = this.data.get(1);

        return crushingTotalTime != 0 && crushingProgress != 0 ? (float) crushingProgress / (float) crushingTotalTime : 0.0F;
    }

    public boolean isCrushing() {
        return data.get(0) > 0;
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
