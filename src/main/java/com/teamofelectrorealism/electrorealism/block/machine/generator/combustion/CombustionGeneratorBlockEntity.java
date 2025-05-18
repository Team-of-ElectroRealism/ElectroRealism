package com.teamofelectrorealism.electrorealism.block.machine.generator.combustion;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.block.IPowerReceiver;
import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.block.connector.ConnectorType;
import com.teamofelectrorealism.electrorealism.block.machine.generator.AbstractGeneratorBlockEntity;
import com.teamofelectrorealism.electrorealism.network.INetworkMember;
import com.teamofelectrorealism.electrorealism.power.ConnectionPoint;
import com.teamofelectrorealism.electrorealism.power.IWireNode;
import com.teamofelectrorealism.electrorealism.power.WireType;
import com.teamofelectrorealism.electrorealism.screen.generator.CombustionGeneratorMenu;
import com.teamofelectrorealism.electrorealism.utils.FuelValues;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class CombustionGeneratorBlockEntity extends AbstractGeneratorBlockEntity implements MenuProvider, IWireNode, INetworkMember {
    public final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            if (!level.isClientSide()) {
                setChanged();
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    private UUID networkId = null;
    private static final int SLOT_FUEL = 0;

    private final ConnectionPoint[] connectionPoints = new ConnectionPoint[1];
    private final IWireNode[] nodeCache = new IWireNode[1];

    private static final String INVENTORY_KEY = "inventory";
    private static final String LIT_TIME_KEY = "combustion_generator.lit_time";
    private static final String LIT_DURATION_KEY = "combustion_generator.lit_duration";

    private int litTime;
    private int litDuration;
    private final ContainerData data;
    private final int voltage = 400;
    private boolean lastPropagatedLit  = false;

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

    @Override
    public int getVoltage() {
        return voltage;
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        boolean wasLit = state.getValue(CombustionGeneratorBlock.LIT);
        boolean isLit = isLit();

        if (isLit()) {
            litTime--;
        }


        if (!isLit && isFuel(itemHandler.getStackInSlot(SLOT_FUEL))) {
            litTime = getBurnDuration(itemHandler.getStackInSlot(SLOT_FUEL));
            litDuration = litTime;
            removeFuel();
            isLit = true;
        }

        if (!level.isClientSide()) {
            if (isLit != lastPropagatedLit) {
                lastPropagatedLit = isLit;
                ElectroRealism.NETWORK_MANAGER.propagateSignal(level, worldPosition);
                LOGGER.info("Generator at {} triggered propagateSignal", worldPosition);
            }
        }


        if (isLit) {
            transferVoltage(level, pos);
        }
        if (wasLit != isLit) {
            level.setBlockAndUpdate(pos, state.setValue(CombustionGeneratorBlock.LIT, isLit));
            setChanged(level, pos, state);
        }
    }

    @Override
    public boolean isFacePositiveTerminal(BlockState state, Direction faceAccessed) {
        Direction machineFacing = state.getValue(CombustionGeneratorBlock.FACING);
        Direction leftFace = machineFacing.getClockWise();

        return faceAccessed == leftFace;
    }

    @Override
    public boolean isFaceNegativeTerminal(BlockState state, Direction faceAccessed) {
        Direction machineFacing = state.getValue(CombustionGeneratorBlock.FACING);
        Direction rightFace = machineFacing.getCounterClockWise();

        return faceAccessed == rightFace;
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

    private boolean isLit() {
        return this.litTime > 0;
    }

    @Override
    protected void transferVoltage(Level level, BlockPos pos) {
        for (Direction facing: Direction.values()) {
            BlockPos neighborPos = pos.offset(facing.getNormal());
            BlockEntity blockEntity = level.getBlockEntity(neighborPos);
            if (blockEntity instanceof IPowerReceiver) {
                IPowerReceiver receiver = (IPowerReceiver) blockEntity;

                receiver.receiveVoltage(voltage);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.electrorealism.combustion_generator");
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

    @Override
    public void setPowered(boolean powered) {

    }

    @Override
    public void joinNetwork() {

    }

    @Override
    public UUID getNetworkId() {
        return networkId;
    }

    @Override
    public void setNetworkId(UUID id) {
        this.networkId = id;
    }

    @Override
    public ConnectorType getConnectorType() {
        return ConnectorType.Small;
    }

    @Override
    public int getMaxWireLength() {
        return 16;
    }

    @Override
    public int getConnectionPointCount() {
        return 1;
    }

    @Override
    public ConnectionPoint getConnectionPoint(int index) {
        return connectionPoints[index];
    }

    @Override
    public void setConnectionPoint(int pointIndex, int connectingPointIndex, WireType wireType, BlockPos targetPos) {
        connectionPoints[pointIndex] = new ConnectionPoint(this, pointIndex, connectingPointIndex, wireType, targetPos);
    }

    @Override
    public void removeConnectionPoint(int index, boolean dropWire) {
        connectionPoints[index] = null;
    }

    @Override
    public Vec3 getConnectionPointOffset(int node) {
        return new Vec3(0.5, 0.5, 0.5); // center of block or adjust as needed
    }

    @Override
    public @Nullable IWireNode getWireNode(int index) {
        return IWireNode.getWireNodeFrom(index, this, connectionPoints, nodeCache, level);
    }
}
