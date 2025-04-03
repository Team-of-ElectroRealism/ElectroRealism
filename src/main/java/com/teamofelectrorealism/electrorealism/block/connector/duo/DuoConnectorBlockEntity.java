package com.teamofelectrorealism.electrorealism.block.connector.duo;

import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.block.connector.AbstractConnectorBlock;
import com.teamofelectrorealism.electrorealism.block.connector.AbstractConnectorBlockEntity;
import com.teamofelectrorealism.electrorealism.block.connector.ConnectorType;
import com.teamofelectrorealism.electrorealism.block.connector.TerminalType;
// Remove unused imports: CompoundTag, HolderLookup, Tag, INetworkMember, AbstractMachineBlockEntity
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity; // Import missing
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable; // Keep this one

public class DuoConnectorBlockEntity extends AbstractConnectorBlockEntity {
    private static final float OFFSET_HEIGHT = 5f; // Height from base block surface

    public DuoConnectorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.DUO_CONNECTOR_BE.get(), pos, blockState);
    }

    // Static tick method for the block's getTicker
    public static void serverTick(Level level, BlockPos pos, BlockState state, DuoConnectorBlockEntity be) {
        be.tick(level, pos, state); // Call instance tick method
    }

    // Instance tick method (if needed for future logic)
    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        // Currently does nothing, but keep for potential future use (e.g., network updates)
    }

    /**
     * Gets the terminal type for the specified node index (0 or 1)
     * by reading the corresponding BlockState property.
     *
     * @param index The node index (0 or 1).
     * @return The TerminalType for that node. Returns TerminalType.None if index is invalid or property missing.
     */
    @Nullable // Keep Nullable for consistency, though it should always return a value now
    @Override
    public TerminalType getTerminalType(int index) {
        BlockState state = this.getBlockState();
        try {
            if (index == 0 && state.hasProperty(DuoConnectorBlock.TERMINAL_TYPE_0)) {
                return state.getValue(DuoConnectorBlock.TERMINAL_TYPE_0);
            } else if (index == 1 && state.hasProperty(DuoConnectorBlock.TERMINAL_TYPE_1)) {
                return state.getValue(DuoConnectorBlock.TERMINAL_TYPE_1);
            }
        } catch (IllegalArgumentException e) {
            // This might happen if the blockstate somehow doesn't have the property,
            // though it should if createBlockStateDefinition is correct.
            System.err.println("Error getting TerminalType property for DuoConnector at " + worldPosition + ": " + e.getMessage());
        }
        // Fallback
        return TerminalType.None;
    }

    @Override
    public ConnectorType getConnectorType() {
        return ConnectorType.Duo;
    }

    @Override
    public int getMaxWireLength() {
        return 16; // Or your desired value
    }

    @Override
    public int getConnectionPointCount() {
        return 2; // Still two connection points
    }

    @Override
    public Vec3 getConnectionPointOffset(int node) {
        BlockState blockState = getBlockState();
        if (!(blockState.getBlock() instanceof AbstractConnectorBlock)) {
            return Vec3.ZERO;
        }
        Direction facing = blockState.getValue(AbstractConnectorBlock.FACING);

        float separation = 8/16f;
        float baseOffset = OFFSET_HEIGHT/16f; // Use the field defined in this class
        float center = 8/16f;

        Vec3 offsetNeg = Vec3.ZERO; // Offset corresponding to the "- separation / 2" side
        Vec3 offsetPos = Vec3.ZERO; // Offset corresponding to the "+ separation / 2" side

        // Calculate the base offsets based on facing, *before* considering node index
        switch (facing) {
            case DOWN: // Attached to ceiling, facing DOWN (Separation along X)
                offsetNeg = new Vec3(center - separation / 2, 1.0 - baseOffset, center);
                offsetPos = new Vec3(center + separation / 2, 1.0 - baseOffset, center);
                break;
            case UP: // Attached to floor, facing UP (Separation along X)
                offsetNeg = new Vec3(center - separation / 2, baseOffset, center);
                offsetPos = new Vec3(center + separation / 2, baseOffset, center);
                break;
            case NORTH: // Attached to South wall, facing NORTH (Separation along X)
                offsetNeg = new Vec3(center - separation / 2, center, 1.0 - baseOffset);
                offsetPos = new Vec3(center + separation / 2, center, 1.0 - baseOffset);
                break;
            case SOUTH: // Attached to North wall, facing SOUTH (Separation along X - BUT ROTATED 180 deg)
                // Base calculation still uses X, but the model's node 0 is now on the +X side
                offsetNeg = new Vec3(center - separation / 2, center, baseOffset); // World -X
                offsetPos = new Vec3(center + separation / 2, center, baseOffset); // World +X
                break;
            case WEST: // Attached to East wall, facing WEST (Separation along Z - BUT ROTATED 270 deg)
                // Base calculation uses Z, but the model's node 0 is now on the +Z side
                offsetNeg = new Vec3(1.0 - baseOffset, center, center - separation / 2); // World -Z
                offsetPos = new Vec3(1.0 - baseOffset, center, center + separation / 2); // World +Z
                break;
            case EAST: // Attached to West wall, facing EAST (Separation along Z)
                offsetNeg = new Vec3(baseOffset, center, center - separation / 2);
                offsetPos = new Vec3(baseOffset, center, center + separation / 2);
                break;
        }

        // --- NEW Logic: Assign correct offset based on node AND facing ---
        Vec3 finalOffset;
        boolean swapNeeded = (facing == Direction.SOUTH || facing == Direction.WEST);

        if (node == 0) {
            // Node 0 should normally be the 'negative' side, unless swapped
            finalOffset = swapNeeded ? offsetPos : offsetNeg;
        } else { // node == 1
            // Node 1 should normally be the 'positive' side, unless swapped
            finalOffset = swapNeeded ? offsetNeg : offsetPos;
        }


        // Return offset RELATIVE TO CENTER (0.5, 0.5, 0.5) for the wire renderer
        Vec3 centerVec = new Vec3(0.5, 0.5, 0.5);
        return finalOffset.subtract(centerVec);
    }

    // Keep onLoad if needed, but remove the determine call
    @Override
    public void onLoad() {
        super.onLoad();
    }


    /**
     * Determines which connection node (0 or 1) is closer to the click location
     * AND is available for a new connection.
     * Used for interaction logic and wire connection logic.
     *
     * @param clickLocation Absolute world coordinates of the click.
     * @return 0 or 1 indicating the closer available node, or -1 if the closer node is already occupied.
     */
    public int getAvailableNode(Vec3 clickLocation) { // Renamed parameter for clarity, keep it if you prefer the old name
        // Get the block center (block position + 0.5, 0.5, 0.5)
        Vec3 blockCenter = Vec3.atCenterOf(this.worldPosition);
        // Convert click location to local coordinates relative to the block center
        Vec3 localHit = clickLocation.subtract(blockCenter);

        // Retrieve connection point offsets (relative to block center)
        // These offsets are correctly adjusted for facing thanks to the previous fix
        Vec3 cp0 = this.getConnectionPointOffset(0);
        Vec3 cp1 = this.getConnectionPointOffset(1);

        // Determine the targeted node based on which offset is closer.
        int selectedNode = (localHit.distanceTo(cp0) < localHit.distanceTo(cp1)) ? 0 : 1;

        // --- RESTORED CHECK ---
        // Check if the selected node already has a connection stored.
        // Assumes getConnectionPoint(index) returns null if no wire is connected to that index.
        // Make sure AbstractConnectorBlockEntity.getConnectionPoint(int) exists and works this way.
        if (this.getConnectionPoint(selectedNode) != null) {
            // The closer node is already occupied, return -1 to indicate unavailability for a *new* connection.
            return -1;
        }

        // The closer node is available, return its index (0 or 1).
        return selectedNode;
    }
}