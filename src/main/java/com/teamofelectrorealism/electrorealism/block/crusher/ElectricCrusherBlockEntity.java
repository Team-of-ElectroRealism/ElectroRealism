package com.teamofelectrorealism.electrorealism.block.crusher;

import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.screen.crusher.ElectricCrusherMenu;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class ElectricCrusherBlockEntity extends BlockEntity implements MenuProvider {
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
    private static final int SLOT_FUEL = 1;
    private static final int SLOT_OUTPUT = 2;

    private static final String INVENTORY_KEY = "inventory";
    private static final String POWER_LEVEL_KEY = "electric_crusher.power_level";
    private static final String CRUSHING_PROGRESS_KEY = "electric_crusher.crushing_progress";
    private static final String CRUSHING_TOTAL_TIME_KEY = "electric_crusher.crushing_total_time";

    private static final int TOTAL_CRUSHING_TIME = 200;

    private int powerLevel;
    private int crushingProgress;
    private int crushingTotalTime = TOTAL_CRUSHING_TIME;
    private final ContainerData data;

    public ElectricCrusherBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.ELECTRIC_CRUSHER_BE.get(), pos, blockState);
        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> ElectricCrusherMenu.this.crushingProgress;
                    case 1 -> ElectricCrusherMenu.this.crushingTotalTime;
                    case 2 -> ElectricCrusherMenu.this.powerLevel;
                    default -> 0;
                };
            }
            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0: ElectricCrusherMenu.this.crushingProgress = pValue;
                    case 1: ElectricCrusherMenu.this.crushingTotalTime = pValue;
                    case 2: ElectricCrusherMenu.this.powerLevel = pValue;
                };
            }
            @Override
            public int getCount() {
                return 3;
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
        return Component.translatable("block.electrorealism.electric_crusher");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ElectricCrusherMenu(containerId, playerInventory, this, this.data);
    }

    @Override
    public void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
        super.loadAdditional(compoundTag, registries);
        itemHandler.deserializeNBT(registries, compoundTag.getCompound(INVENTORY_KEY));
        powerLevel = compoundTag.getInt(POWER_LEVEL_KEY);
        crushingProgress = compoundTag.getInt(CRUSHING_PROGRESS_KEY);
        crushingTotalTime = compoundTag.getInt(CRUSHING_TOTAL_TIME_KEY);
    }
    @Override
    public void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
        compoundTag.put(INVENTORY_KEY, itemHandler.serializeNBT(registries));
        compoundTag.putInt(POWER_LEVEL_KEY, powerLevel);
        compoundTag.putInt(CRUSHING_PROGRESS_KEY, crushingProgress);
        compoundTag.putInt(CRUSHING_TOTAL_TIME_KEY, crushingTotalTime);

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

    }

    private boolean isPowered() {
        return this.powerLevel > 0;
    }
}
