package com.teamofelectrorealism.electrorealism.block.connector.small;

import com.teamofelectrorealism.electrorealism.block.ModBlockEntityTypes;
import com.teamofelectrorealism.electrorealism.block.connector.AbstractConnectorBlock;
import com.teamofelectrorealism.electrorealism.block.connector.AbstractConnectorBlockEntity;
import com.teamofelectrorealism.electrorealism.block.connector.ConnectorType;
import com.teamofelectrorealism.electrorealism.block.connector.TerminalType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class SmallConnectorBlockEntity extends AbstractConnectorBlockEntity {

    private final static float OFFSET_HEIGHT = 3f;
    public final static Vec3 OFFSET_DOWN = new Vec3(0f, OFFSET_HEIGHT/16f, 0f);
    public final static Vec3 OFFSET_UP = new Vec3(0f, -OFFSET_HEIGHT/16f, 0f);
    public final static Vec3 OFFSET_NORTH = new Vec3(0f, 0f, OFFSET_HEIGHT/16f);
    public final static Vec3 OFFSET_WEST = new Vec3(OFFSET_HEIGHT/16f, 0f, 0f);
    public final static Vec3 OFFSET_SOUTH = new Vec3(0f, 0f, -OFFSET_HEIGHT/16f);
    public final static Vec3 OFFSET_EAST = new Vec3(-OFFSET_HEIGHT/16f, 0f, 0f);

    public SmallConnectorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.SMALL_CONNECTOR_BE.get(), pos, blockState);
    }

    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        //System.out.println("Ticking!");
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
        return 4;
    }

    @Override
    public Level getLevelFromSelf() {
        return this.getLevel();
    }

    @Override
    public Vec3 getConnectionPointOffset(int node) {
        return switch (getBlockState().getValue(AbstractConnectorBlock.FACING)) {
            case DOWN -> OFFSET_DOWN;
            case UP -> OFFSET_UP;
            case NORTH -> OFFSET_NORTH;
            case WEST -> OFFSET_WEST;
            case SOUTH -> OFFSET_SOUTH;
            case EAST -> OFFSET_EAST;
        };
    }

    @Override
    public TerminalType getTerminalType(int index) { // Return non-nullable TerminalType
        // Read the type DIRECTLY from the BlockState property
        BlockState blockState = this.getBlockState();
        if (blockState.hasProperty(SmallConnectorBlock.TERMINAL_TYPE)) {
            // Ignore index for SmallConnector, it represents a single type dictated by its state
            return blockState.getValue(SmallConnectorBlock.TERMINAL_TYPE);
        }
        // Fallback if property is missing (shouldn't happen)
        return TerminalType.None;
    }
}
