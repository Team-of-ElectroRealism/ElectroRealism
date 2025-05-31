package com.teamofpowersim.powersim.block.machine.provider.combustion;

import com.mojang.logging.LogUtils;
import com.teamofpowersim.powersim.PowerSim;
import com.teamofpowersim.powersim.block.IActiveVoltageProvider;
import com.teamofpowersim.powersim.block.ModBlockEntityTypes;
import com.teamofpowersim.powersim.block.machine.provider.AbstractPowerProviderBlockEntity;
import com.teamofpowersim.powersim.screen.generator.CombustionGeneratorMenu;
import com.teamofpowersim.powersim.utils.FuelValues;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class CombustionGeneratorBlockEntity extends AbstractPowerProviderBlockEntity implements MenuProvider, IActiveVoltageProvider {
    private static final Logger LOGGER = LogUtils.getLogger();

    public final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            if (!level.isClientSide()) {
                setChanged();
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    private static final int SLOT_FUEL = 0;

    private static final String INVENTORY_KEY = "inventory";
    private static final String LIT_TIME_KEY = "combustion_generator.lit_time";
    private static final String LIT_DURATION_KEY = "combustion_generator.lit_duration";

    private int litTime;
    private int litDuration;
    private final ContainerData data;
    private final int nominalVoltage = 400;

    public CombustionGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.COMBUSTION_GENERATOR_BE.get(), pos, blockState);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> CombustionGeneratorBlockEntity.this.litTime;
                    case 1 -> CombustionGeneratorBlockEntity.this.litDuration;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0: CombustionGeneratorBlockEntity.this.litTime = value;
                    case 1: CombustionGeneratorBlockEntity.this.litDuration = value;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    /**
     * Returns the nominal voltage this generator produces when active.
     */
    @Override
    public int getNominalVoltage() {
        return this.nominalVoltage;
    }

    /**
     * Checks if the generator is currently active (lit and producing power).
     * This is used by AbstractPowerProviderBlockEntity.getVoltage() to determine
     * if 0V should be returned for the simulation.
     */
    @Override
    public boolean isActive() {
        return this.isLit();
    }

    private boolean isLit() {
        return this.litTime > 0;
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }

        boolean wasPreviouslyActive = isActive(); // Cache state BEFORE any changes this tick

        boolean currentBlockStateLit = state.getValue(CombustionGeneratorBlock.LIT);
        boolean currentInternalLit = isLit(); // isLit() is this.litTime > 0

        if (currentInternalLit) {
            litTime--;
        }

        // Refuel logic
        if (!currentInternalLit && isFuel(itemHandler.getStackInSlot(SLOT_FUEL))) {
            litTime = getBurnDuration(itemHandler.getStackInSlot(SLOT_FUEL));
            litDuration = litTime;
            removeFuel();
            currentInternalLit = true; // Update internal lit state
        }

        // If the LIT block state doesn't match our internal lit state, update block state.
        if (currentBlockStateLit != currentInternalLit) {
            level.setBlockAndUpdate(pos, state.setValue(CombustionGeneratorBlock.LIT, currentInternalLit));
            setChanged(); // Mark BE for saving
        }

        boolean isCurrentlyActive = isActive(); // Get state AFTER changes this tick

        // If the effective output state for the simulation has changed, mark the network dirty via NetworkManager.
        if (wasPreviouslyActive != isCurrentlyActive) {
            LOGGER.debug("Combustion Generator at {} active state changed: {} -> {}. Requesting network dirty status via NetworkManager.",
                    pos.toShortString(), wasPreviouslyActive, isCurrentlyActive);
            if (PowerSim.NETWORK_MANAGER != null) {
                // Use the new NetworkManager method.
                // We pass 'this' (the INetworkMember instance) to the manager.
                PowerSim.NETWORK_MANAGER.markNetworkDirty(this);
            } else {
                LOGGER.error("Combustion Generator at {} changed active state, but NetworkManager is NULL!", pos.toShortString());
            }
        }
    }

    @Override
    public boolean isFaceAllowed(BlockState state, Direction faceAccessed) {
        return true;
    }

    private void removeFuel() {
        if (itemHandler.getStackInSlot(SLOT_FUEL).hasCraftingRemainingItem())
            itemHandler.setStackInSlot(SLOT_FUEL, new ItemStack(itemHandler.getStackInSlot(SLOT_FUEL).getCraftingRemainingItem().getItem()));
        else if (itemHandler.getStackInSlot(SLOT_FUEL).getCount() > 1)
            itemHandler.getStackInSlot(SLOT_FUEL).shrink(1);
        else itemHandler.setStackInSlot(SLOT_FUEL, ItemStack.EMPTY);
    }

    private int getBurnDuration(ItemStack stackInSlot) {
        return FuelValues.getFuelValue(stackInSlot);
    }

    private boolean isFuel(ItemStack stackInSlot) {
        return getBurnDuration(stackInSlot) > 0;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.powersim.combustion_generator");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put(INVENTORY_KEY, itemHandler.serializeNBT(registries));
        tag.putInt(LIT_TIME_KEY, litTime);
        tag.putInt(LIT_DURATION_KEY, litDuration);

        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound(INVENTORY_KEY));
        litTime = tag.getInt(LIT_TIME_KEY);
        litDuration = tag.getInt(LIT_DURATION_KEY);
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

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CombustionGeneratorMenu(containerId, playerInventory, this, this.data);
    }
}
