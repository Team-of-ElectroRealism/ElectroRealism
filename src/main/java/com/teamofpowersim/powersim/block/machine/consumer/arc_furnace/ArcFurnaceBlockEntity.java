package com.teamofpowersim.powersim.block.machine.consumer.arc_furnace;

import com.mojang.logging.LogUtils;
import com.teamofpowersim.powersim.api.ElectricalAPI;
import com.teamofpowersim.powersim.block.ModBlockEntityTypes;
import com.teamofpowersim.powersim.block.machine.consumer.AbstractPowerConsumerBlockEntity;
import com.teamofpowersim.powersim.recipe.ModRecipes;
import com.teamofpowersim.powersim.recipe.arc_furnace.ArcFurnaceRecipe;
import com.teamofpowersim.powersim.recipe.arc_furnace.ArcFurnaceRecipeInput;
import com.teamofpowersim.powersim.screen.arc_furnace.ArcFurnaceMenu;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;

public class ArcFurnaceBlockEntity extends AbstractPowerConsumerBlockEntity implements MenuProvider {
    private static final Logger LOGGER = LogUtils.getLogger();

    public final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            if (!level.isClientSide()) {
                setChanged();
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2);
            }
        }
    };

    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;

    private static final String INVENTORY_KEY = "inventory";
    private static final String HEAT_LEVEL_KEY = "arc_furnace.heat_level";
    private static final String BUFFER_LEVEL_KEY = "arc_furnace.buffer_level";
    private static final String BUFFER_TOTAL_LEVEL_KEY = "arc_furnace.buffer_total_level";
    private static final String HEAT_TOTAL_LEVEL_KEY = "arc_furnace.heat_total_level";
    private static final String SMELTING_PROGRESS_KEY = "arc_furnace.smelting_progress";
    private static final String SMELTING_TOTAL_TIME_KEY = "arc_furnace.smelting_total_time";
    private static final String INTERNAL_RESISTANCE_KEY = "arc_furnace.internal_resistance";

    // --- Electrical Configuration ---
    private static final double MIN_OPERATING_VOLTAGE = 100.0; // Example: Needs 100V to even consider working
    private static final double NOMINAL_OPERATING_CURRENT = 30.0; // Example: Ideal current for full speed
    private static final double MAX_SAFE_CURRENT = 50.0;      // Example: Machine might break or wires melt

    private static final int TOTAL_SMELTING_TIME = 40;
    private static final int TOTAL_BUFFER_CAPASITY = 2000; // In mAh
    private static final int INTERNAL_RESISTANCE = 10; // In ohm

    private int heatLevel;
    private int heatTotalLevel = 7;
    private int smeltingProgress;
    private int smeltingTotalTime = TOTAL_SMELTING_TIME;
    private int bufferLevel;
    private int bufferTotalLevel = TOTAL_BUFFER_CAPASITY;
    private int internalResistance = INTERNAL_RESISTANCE;
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
                    case 4 -> ArcFurnaceBlockEntity.this.bufferLevel;
                    case 5 -> ArcFurnaceBlockEntity.this.bufferTotalLevel;
                    case 6 -> ArcFurnaceBlockEntity.this.internalResistance;

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
                    case 4: ArcFurnaceBlockEntity.this.bufferLevel = i;
                    case 5: ArcFurnaceBlockEntity.this.bufferTotalLevel = i;
                    case 6: ArcFurnaceBlockEntity.this.internalResistance = i;
                }
            }

            @Override
            public int getCount() {
                return 7;
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
        return Component.translatable("block.powersim.arc_furnace");
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
        bufferLevel = compoundTag.getInt(BUFFER_LEVEL_KEY);
        bufferTotalLevel = compoundTag.getInt(BUFFER_TOTAL_LEVEL_KEY);
        internalResistance = compoundTag.getInt(INTERNAL_RESISTANCE_KEY);
    }
    @Override
    public void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
        compoundTag.put(INVENTORY_KEY, itemHandler.serializeNBT(registries));
        compoundTag.putInt(HEAT_LEVEL_KEY, heatLevel);
        compoundTag.putInt(HEAT_TOTAL_LEVEL_KEY, heatTotalLevel);
        compoundTag.putInt(SMELTING_PROGRESS_KEY, smeltingProgress);
        compoundTag.putInt(SMELTING_TOTAL_TIME_KEY, smeltingTotalTime);
        compoundTag.putInt(BUFFER_LEVEL_KEY, bufferLevel);
        compoundTag.putInt(BUFFER_TOTAL_LEVEL_KEY, bufferTotalLevel);
        compoundTag.putInt(INTERNAL_RESISTANCE_KEY, internalResistance);

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

    @Override
    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        if (level.isClientSide()) {
            return;
        }

        // Get simulated values from AbstractMachineBlockEntity
        double actualVoltage = getSimVoltage(); // This is V across the ArcFurnace (R1)
        double actualCurrent = getSimCurrent(); // This is I through the ArcFurnace (R1)

        // --- 1. Check for Over-Current / Catastrophic Failure ---
        if (Math.abs(actualCurrent) > MAX_SAFE_CURRENT) {
            LOGGER.warn("Arc Furnace at {} OVERCURRENT! I: {:.2f}A > {:.2f}A. Destroying.", blockPos, actualCurrent, MAX_SAFE_CURRENT);
            level.destroyBlock(blockPos, true); // Drop items
            // You could also play an explosion sound, create fire, etc.
            return; // Stop further processing
        }

        // --- 2. Determine if the furnace has enough power to operate ---
        // For simplicity, let's say it needs minimum voltage and some current.
        // The current direction matters (positive current for positive voltage usually means consuming power)
        boolean hasSufficientPower = (Math.abs(actualVoltage) >= MIN_OPERATING_VOLTAGE) &&
                (Math.signum(actualVoltage) == Math.signum(actualCurrent) && Math.abs(actualCurrent) > 0.01); // Consuming power

        if (hasSufficientPower) {
            // --- 3. Charge the buffer / Accumulate Energy ---
            // Power P = V * I. Energy = P * t.
            // Let's say 1 tick = 1/20th of a second.
            // We can define bufferTotalLevel in terms of Joules or Watt-seconds.
            // For now, let's make a simpler charge rate based on current.
            // If current is around NOMINAL_OPERATING_CURRENT, it charges/heats fast.
            // (This replaces your old `receiveVoltage` and `ElectricalAPI`)

            double powerInputWatts = Math.abs(actualVoltage * actualCurrent); // P = VI
            // Let's define bufferTotalLevel as, say, 2000 "energy units"
            // And let's say at nominal current and voltage, it fills the buffer in, e.g., 10 seconds (200 ticks)
            // Nominal power = MIN_OPERATING_VOLTAGE * NOMINAL_OPERATING_CURRENT (e.g., 100V * 30A = 3000W)
            // Charge rate could be proportional to powerInputWatts.
            // Example: int chargeIncrease = (int) (powerInputWatts / 150.0); // Scaled to fill buffer over time
            // A simpler approach for now: if powered, increment buffer.
            if (bufferLevel < bufferTotalLevel) {
                bufferLevel = Math.min(bufferTotalLevel, bufferLevel + (int) (Math.abs(actualCurrent) / NOMINAL_OPERATING_CURRENT * 10)); // Charge faster with more current, up to a point
                if (bufferLevel > bufferTotalLevel) bufferLevel = bufferTotalLevel;
            }

            // --- 4. Heat Up ---
            // Heating speed could also depend on actualCurrent or powerInputWatts
            if (isBufferSufficientlyCharged()) { // Only heat if buffer has some charge
                heatUp(powerInputWatts); // Pass power to heatUp method
            } else {
                // If buffer depleted, start cooling slightly even if main power is on
                // This prevents instant reheat if buffer is just above threshold
                coolDown();
            }

        } else { // Insufficient power from simulation
            // Discharge buffer if not externally powered
            if (bufferLevel > 0) {
                bufferLevel = Math.max(0, bufferLevel - 5); // Gradual discharge
            }
            coolDown();
        }

        // --- 5. Smelting Logic (uses heatLevel and bufferLevel) ---
        // This part of your logic can mostly stay, as it depends on internal heat and buffer.
        if (hasRecipe() && isOutputSlotEmptyOrReceivable()) {
            if (isHeated() && isBufferSufficientlyChargedForSmelting()) { // Renamed for clarity
                increaseSmeltingProgress();
                // Smelting consumes buffered energy
                bufferLevel = Math.max(0, bufferLevel - 3); // Keep your existing consumption rate per progress tick
                if (hasSmeltingFinished()) {
                    smeltItem();
                    resetProgress();
                }
            } else {
                if (smeltingProgress > 0) smeltingProgress = 0; // Reset if conditions are no longer met
            }
        } else {
            if (smeltingProgress > 0) smeltingProgress = 0; // Reset if no recipe or output blocked
        }

        // Update block state for LIT property if it exists
        // (This should be handled by your AbstractPowerConsumerBlock or here if specific)
        BlockState currentState = getBlockState();
        if (currentState.getBlock() instanceof ArcFurnaceBlock && currentState.hasProperty(ArcFurnaceBlock.LIT)) {
            boolean shouldBeLit = isHeated() && hasSufficientPower; // Or just isHeated()
            if (currentState.getValue(ArcFurnaceBlock.LIT) != shouldBeLit) {
                level.setBlock(blockPos, currentState.setValue(ArcFurnaceBlock.LIT, shouldBeLit), 3);
            }
        }
        setChanged(); // Important to save changes
    }

    private boolean isBufferSufficientlyCharged() {
        // Threshold for the buffer to be considered "charged enough" to allow heating/smelting
        return bufferLevel > bufferTotalLevel * 0.1; // e.g., needs at least 10% charge
    }

    private boolean isBufferSufficientlyChargedForSmelting() {
        // Specific threshold for smelting, might be higher than just for heating
        return bufferLevel >= 3; // Your original smelting consumption
    }

    @Override
    public boolean isFaceAllowed(BlockState state, Direction faceAccessed) {
        Direction facing = state.getValue(ArcFurnaceBlock.FACING);
        return faceAccessed != facing && faceAccessed != facing.getOpposite();
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

    // Timer for heating and cooling, to make it gradual
    private int heatActivityTimer = 0;
    private static final int HEAT_TICK_INTERVAL = 20; // Check/change heat every second

    private void heatUp(double powerWatts) {
        // More power = faster heating
        // This is a simple model. A better one might consider heat capacity.
        heatActivityTimer++;
        if (heatActivityTimer >= HEAT_TICK_INTERVAL) {
            heatActivityTimer = 0;
            if (heatLevel < heatTotalLevel) {
                int heatIncrease = 1; // Base increase
                if (powerWatts > (MIN_OPERATING_VOLTAGE * NOMINAL_OPERATING_CURRENT * 0.75)) { // If significantly powered
                    heatIncrease = 2;
                }
                heatLevel = Math.min(heatTotalLevel, heatLevel + heatIncrease);
                LOGGER.debug("Arc Furnace at {} heating up. Heat: {}/{}", worldPosition, heatLevel, heatTotalLevel);
            }
        }
    }

    private void coolDown() {
        heatActivityTimer++;
        if (heatActivityTimer >= HEAT_TICK_INTERVAL * 2) { // Cool down slower than heat up
            heatActivityTimer = 0;
            if (heatLevel > 0) {
                heatLevel--;
                LOGGER.debug("Arc Furnace at {} cooling down. Heat: {}/{}", worldPosition, heatLevel, heatTotalLevel);
            }
        }
    }


    @Override
    public int getResistance() {
        return internalResistance;
    }

    @Override
    public void receiveVoltage(int voltage) {
        int totalResistance = this.internalResistance;

        int chargeIncrease = ElectricalAPI.getChargeIncreaseMah(voltage, totalResistance, (double) 1 / 20);

        setBufferCharge(bufferLevel + chargeIncrease);
    }

    @Override
    public int getBufferCharge() {
        return bufferLevel;
    }

    @Override
    public void setBufferCharge(int charge) {
        this.bufferLevel = Math.max(0, Math.min(charge, bufferTotalLevel));
    }

    private boolean isHeated() {
        // Condition for being "hot enough" to smelt
        return this.heatLevel >= heatTotalLevel -1; // e.g., needs to be almost max heat
    }
}