package com.teamofelectrorealism.electrorealism.block.connector.duo;

import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.block.connector.AbstractConnectorBlock;
import com.teamofelectrorealism.electrorealism.block.connector.AbstractConnectorBlockEntity;
import com.teamofelectrorealism.electrorealism.block.connector.ConnectorType;
import com.teamofelectrorealism.electrorealism.block.connector.TerminalType;
import com.teamofelectrorealism.electrorealism.block.machine.AbstractMachineBlockEntity;
import com.teamofelectrorealism.electrorealism.network.INetworkMember;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class DuoConnectorBlockEntity extends AbstractConnectorBlockEntity {

    private final static float OFFSET_HEIGHT = 3f;
    public final static Vec3 OFFSET_DOWN = new Vec3(0f, OFFSET_HEIGHT/16f, 0f);
    public final static Vec3 OFFSET_UP = new Vec3(0f, -OFFSET_HEIGHT/16f, 0f);
    public final static Vec3 OFFSET_NORTH = new Vec3(0f, 0f, OFFSET_HEIGHT/16f);
    public final static Vec3 OFFSET_WEST = new Vec3(OFFSET_HEIGHT/16f, 0f, 0f);
    public final static Vec3 OFFSET_SOUTH = new Vec3(0f, 0f, -OFFSET_HEIGHT/16f);
    public final static Vec3 OFFSET_EAST = new Vec3(-OFFSET_HEIGHT/16f, 0f, 0f);

    private static final String TERMINAL_TYPE_INDEX_0_KEY = "terminal_type_0";
    private static final String TERMINAL_TYPE_INDEX_1_KEY = "terminal_type_1";

    @Nullable
    private TerminalType terminalTypeIndex0 = null;
    @Nullable
    private TerminalType terminalTypeIndex1 = null;

    public DuoConnectorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.DUO_CONNECTOR_BE.get(), pos, blockState);
    }

    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        //System.out.println("Ticking!");
    }

    /**
     * Gets the terminal type at the specified index.
     * If the terminal type is not yet determined, it attempts to determine it.
     * @param index The index of the terminal type to get (0 or 1).
     * @return The terminal type at the specified index, or null if the index is invalid or the type is not determined.
     */
    @Nullable
    @Override
    public TerminalType getTerminalType(int index) {
        if ((index == 0 && terminalTypeIndex0 == null) || (index == 1 && terminalTypeIndex1 == null)) {
            if (this.level != null && !this.level.isClientSide) {
                determineAttachedTerminalTypes();
            }
        }

        if (index == 0) {
            return this.terminalTypeIndex0;
        } else if (index == 1) {
            return this.terminalTypeIndex1;
        } else {
            return null;
        }
    }

    /**
     * Determines the terminal types of the attached machine, if any.
     * Sets the terminalTypeIndex0 and terminalTypeIndex1 accordingly.
     */
    private void determineAttachedTerminalTypes() {
        if (this.level == null || this.level.isClientSide) return;

        INetworkMember adjacentMember = findNetworkMember();
        TerminalType determinedType0 = null;
        TerminalType determinedType1 = null;

        if (adjacentMember instanceof AbstractMachineBlockEntity machineBE) {
            BlockState machineState = machineBE.getBlockState();
            BlockState connectorState = this.getBlockState();
            if (!(connectorState.getBlock() instanceof AbstractConnectorBlock)) return;

            Direction directionFromMachine = connectorState.getValue(AbstractConnectorBlock.FACING);


        }

        if (this.terminalTypeIndex0 != determinedType0 || this.terminalTypeIndex1 != determinedType1) {
            this.terminalTypeIndex0 = determinedType0;
            this.terminalTypeIndex1 = determinedType1;
            setChanged();
        }
    }

    @Override
    public ConnectorType getConnectorType() {
        return ConnectorType.Duo;
    }

    @Override
    public int getMaxWireLength() {
        return 16;
    }

    @Override
    public int getConnectionPointCount() {
        return 2;
    }

    @Override
    public Vec3 getConnectionPointOffset(int node) {
        // Return different offsets for node 0 and node 1 based on FACING
        // These offsets are relative to the block's center (0.5, 0.5, 0.5)
        // Wires should render connecting to these points.
        BlockState blockState = getBlockState();
        // Ensure the blockstate has the FACING property before accessing it
        if (!(blockState.getBlock() instanceof AbstractConnectorBlock)) {
            return Vec3.ZERO; // Safety check
        }
        Direction facing = blockState.getValue(AbstractConnectorBlock.FACING);

        float separation = 8/16f;
        float baseOffset = 5/16f;
        float center = 8/16f;

        Vec3 offset0 = Vec3.ZERO;
        Vec3 offset1 = Vec3.ZERO;

        switch (facing) {
            case DOWN: // Originally on ceiling, facing down. Inverted to floor.
                offset0 = new Vec3(center - separation / 2, baseOffset, center);
                offset1 = new Vec3(center + separation / 2, baseOffset, center);
                break;
            case UP: // Originally on floor, facing up. Inverted to ceiling.
                offset0 = new Vec3(center - separation / 2, 1.0 - baseOffset, center);
                offset1 = new Vec3(center + separation / 2, 1.0 - baseOffset, center);
                break;
            case NORTH: // Originally on South wall (z = baseOffset), facing North. Inverted to North wall.
                offset0 = new Vec3(center - separation / 2, center, 1.0 - baseOffset); // z is now near 1 edge
                offset1 = new Vec3(center + separation / 2, center, 1.0 - baseOffset);
                break;
            case SOUTH: // Originally on North wall (z = 1.0 - baseOffset), facing South. Inverted to South wall.
                offset0 = new Vec3(center - separation / 2, center, baseOffset); // z is now near 0 edge
                offset1 = new Vec3(center + separation / 2, center, baseOffset);
                break;
            case WEST: // Originally on East wall (x = baseOffset), facing West. Inverted to West wall.
                offset0 = new Vec3(1.0 - baseOffset, center, center - separation / 2); // x is now near 1 edge
                offset1 = new Vec3(1.0 - baseOffset, center, center + separation / 2);
                break;
            case EAST: // Originally on West wall (x = 1.0 - baseOffset), facing East. Inverted to East wall.
                offset0 = new Vec3(baseOffset, center, center - separation / 2); // x is now near 0 edge
                offset1 = new Vec3(baseOffset, center, center + separation / 2);
                break;
        }

        // --- Return offset RELATIVE TO CENTER (0.5, 0.5, 0.5) ---
        // The WireNodeRenderer adds the block position and (0.5, 0.5, 0.5)
        // So subtract (0.5, 0.5, 0.5) from the absolute offsets calculated above.
        Vec3 centerVec = new Vec3(0.5, 0.5, 0.5);
        return (node == 0) ? offset0.subtract(centerVec) : offset1.subtract(centerVec);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide) {
            determineAttachedTerminalTypes();
        }
    }

    public int getAvailableNode(Vec3 clickLocation) {
        // Get the block center (block position + 0.5, 0.5, 0.5)
        Vec3 blockCenter = Vec3.atCenterOf(this.worldPosition);
        // Convert click location to local coordinates relative to the block center
        Vec3 localHit = clickLocation.subtract(blockCenter);

        // Retrieve connection point offsets (relative to block center)
        Vec3 cp0 = this.getConnectionPointOffset(0);
        Vec3 cp1 = this.getConnectionPointOffset(1);

        // Determine the targeted node based on which offset is closer.
        int selectedNode = (localHit.distanceTo(cp0) < localHit.distanceTo(cp1)) ? 0 : 1;

        // Only allow a new connection if the selected node is not already occupied.
        if (getConnectionPoint(selectedNode) != null) {
            return -1;
        }
        return selectedNode;
    }


    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.terminalTypeIndex0 != null) {
            tag.putString(TERMINAL_TYPE_INDEX_0_KEY, this.terminalTypeIndex0.getSerializedName());
        }
        if (this.terminalTypeIndex1 != null) {
            tag.putString(TERMINAL_TYPE_INDEX_1_KEY, this.terminalTypeIndex1.getSerializedName());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TERMINAL_TYPE_INDEX_0_KEY, Tag.TAG_STRING)) {
            this.terminalTypeIndex0 = TerminalType.fromName(tag.getString(TERMINAL_TYPE_INDEX_0_KEY));
        } else {
            this.terminalTypeIndex0 = null;
        }

        if (tag.contains(TERMINAL_TYPE_INDEX_1_KEY, Tag.TAG_STRING)) {
            this.terminalTypeIndex1 = TerminalType.fromName(tag.getString(TERMINAL_TYPE_INDEX_1_KEY));
        } else {
            this.terminalTypeIndex1 = null;
        }
    }
}
