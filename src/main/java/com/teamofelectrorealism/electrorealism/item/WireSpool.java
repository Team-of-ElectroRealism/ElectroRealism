package com.teamofelectrorealism.electrorealism.item;

import com.teamofelectrorealism.electrorealism.datacomponents.ModDataComponents;
import com.teamofelectrorealism.electrorealism.datacomponents.WireConnectionData;
import com.teamofelectrorealism.electrorealism.power.IWireNode;
import com.teamofelectrorealism.electrorealism.power.WireConnectResult;
import com.teamofelectrorealism.electrorealism.power.WireType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

//todo
public class WireSpool extends Item {
    public WireSpool(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack itemStack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        BlockEntity blockEntity = context.getLevel().getBlockEntity(clickedPos);

        WireConnectionData connectionData = itemStack.get(ModDataComponents.WIRE_CONNECTION.value());

        // 3. Check if the block entity is a valid wire nodeBlockEntity. If not, exit.
        if (!(blockEntity instanceof IWireNode)) { // will also check if blockEntity is null
            return InteractionResult.PASS;
        }
        IWireNode nodeBlockEntity = (IWireNode) blockEntity;
        Item heldItem = itemStack.getItem();

        // 4. Check if this is the first or second click in the connection process.
        if (connectionData != null && connectionData.pos() != null /* Item has stored position data (second click) */) {
            WireConnectResult connectResult = null;
            BlockPos connectionPos = connectionData.pos();

            // 6a. Disconnect or connect wires based on the item type.
            if(isRemover(heldItem)) {
                connectResult = IWireNode.disconnect(context.getLevel(), clickedPos, connectionPos);
            } else {
                // 5a. Determine the type of wire being used.
                connectResult = IWireNode.connect(context.getLevel(), getPos(connectionData), getNode(connectionData), clickedPos, nodeBlockEntity.getAvailableNode(context.getClickLocation()), WireType.of(heldItem));
            }

            blockEntity.setChanged(); // 8a. Update the block entity's state.

            WireType connectionType = IWireNode.getTypeOfConnection(context.getLevel(), clickedPos, getPos(connectionData));
            // 9a. If the player is not in creative mode, handle item consumption and potential drops.
            if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
                if (connectResult == WireConnectResult.REMOVED) {
                    itemStack.shrink(1);
                    ItemStack stack = connectionType.getSourceDrop();
                    boolean shouldDrop = !context.getPlayer().addItem(stack);
                    if (shouldDrop) context.getPlayer().drop(stack, false);
                }
            }

            // 10a. Clear the item's NBT data and display a message to the player.
            itemStack.set(ModDataComponents.WIRE_CONNECTION.value(), null);
            context.getPlayer().displayClientMessage(connectResult.getMessage(), true);
        }

        else { // First click
            // 5b. If the item is a remover and the block has no connections, display a message and play a sound.
            if (context.getPlayer() == null) return InteractionResult.PASS;
            if (isRemover(heldItem)) {
                if (!nodeBlockEntity.hasAnyConnection()) {
                    context.getPlayer().displayClientMessage(WireConnectResult.NO_CONNECTION.getMessage(), true);
                    return InteractionResult.CONSUME;
                }
            }
            int index = nodeBlockEntity.getAvailableNode(context.getClickLocation());
            if (index < 0) {
                return InteractionResult.PASS;
            }
            if (!isRemover(heldItem)) {
                context.getPlayer().displayClientMessage(WireConnectResult.getConnect(nodeBlockEntity.isNodeInput(index), nodeBlockEntity.isNodeOutput(index)).getMessage(), true);
            }
            itemStack.set(ModDataComponents.WIRE_CONNECTION.value(), new WireConnectionData(nodeBlockEntity.getPos(), index)); // observerPacket? todo
        }
        return InteractionResult.CONSUME; // Indicate that the item was used
    }

    private static boolean isRemover(Item item) {
        return item == ModItems.SPOOL.get();
    }

    public static BlockPos getPos(WireConnectionData wireConnectionData) {
        if (wireConnectionData.pos() == null) {
            return null;
        }
        return wireConnectionData.pos();
    }

    public static int getNode(WireConnectionData wireConnectionData) {
        if (wireConnectionData.node() == -1) {
            return -1;
        }
        return wireConnectionData.node();
    }
}
