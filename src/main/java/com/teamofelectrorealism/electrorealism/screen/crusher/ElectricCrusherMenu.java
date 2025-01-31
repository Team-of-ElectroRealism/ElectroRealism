package com.teamofelectrorealism.electrorealism.screen.crusher;

import com.teamofelectrorealism.electrorealism.block.crusher.ElectricCrusherBlockEntity;
import com.teamofelectrorealism.electrorealism.screen.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ElectricCrusherMenu  extends AbstractContainerMenu {
    public final ElectricCrusherBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public ElectricCrusherMenu(int containerId , Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(3));
    }

    public ElectricCrusherMenu(int containerId, Inventory inv, BlockEntity blockEntity, ContainerData containerData) {
        super(ModMenuTypes.ELECTRIC_CRUSHER_MENU.get(), containerId)
        this.blockEntity = (ElectricCrusherBlockEntity) blockEntity;
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 1, 67, 17));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 2, 56, 53));
        this.addSlot(new SlotItemHandler(this.blockEntity.itemHandler, 3, 116, 35));

        addDataSlots(data);
    }

    public boolean isCrushing() {
        return data.get(0) > 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }
}
