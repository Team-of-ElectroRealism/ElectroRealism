package com.teamofpowersim.powersim.item;

import com.teamofpowersim.powersim.PowerSim;
import com.teamofpowersim.powersim.datacomponents.ModDataComponents;
import com.teamofpowersim.powersim.datacomponents.WireConnectionData;
import com.teamofpowersim.powersim.network.INetworkMember;
import com.teamofpowersim.powersim.network.NetworkManager;
import com.teamofpowersim.powersim.power.IWireNode;
import com.teamofpowersim.powersim.power.WireConnectResult;
import com.teamofpowersim.powersim.power.WireType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class WireSpool extends Item {
    public WireSpool(Properties properties) {
        super(properties);
    }

    /**
     * Handles the interaction when a wire spool item is used on a block.
     * It manages the connection and disconnection of wires between {@link IWireNode} instances.
     *
     * @param context The context of the item usage.
     * @return {@link InteractionResult#SUCCESS} if the action was successful,
     *         {@link InteractionResult#FAIL} if the action failed, {@link InteractionResult#PASS} otherwise.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack itemStackInHand = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        Level level = context.getLevel();
        BlockEntity clickedBlockEntity = level.getBlockEntity(clickedPos);

        WireConnectionData connectionData = itemStackInHand.get(ModDataComponents.WIRE_CONNECTION.value());

        if (!(clickedBlockEntity instanceof IWireNode clickedIWireNodeBlockEntity)) {
            return InteractionResult.PASS;
        }
        Item itemInHand = itemStackInHand.getItem();

        if (connectionData != null && connectionData.pos() != null) { // Second click
            WireConnectResult connectResult;
            BlockPos targetPos = connectionData.pos();
            BlockEntity targetBlockEntity = level.getBlockEntity(targetPos);

            if (!(targetBlockEntity instanceof IWireNode)) {
                itemStackInHand.set(ModDataComponents.WIRE_CONNECTION.value(), null);
                if (context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(WireConnectResult.INVALID.getMessage(), true);
                }
                return InteractionResult.FAIL;
            }

            if(isRemover(itemInHand)) {
                connectResult = IWireNode.disconnect(level, clickedPos, targetPos);

                if (!level.isClientSide() && connectResult == WireConnectResult.REMOVED) {
                    NetworkManager networkManager = PowerSim.NETWORK_MANAGER;
                    INetworkMember member1 = IWireNode.getNetworkMemberFromBlockEntity(clickedBlockEntity);
                    INetworkMember member2 = IWireNode.getNetworkMemberFromBlockEntity(targetBlockEntity);

                    networkManager.checkNetworkConnectivityAfterConnectionRemoval(member1, member2);
                }

            } else {
                connectResult = IWireNode.connect(level, getPos(connectionData), getNode(connectionData), clickedPos, clickedIWireNodeBlockEntity.getAvailableNode(context.getClickLocation()), WireType.of(itemInHand));
                if (connectResult.isConnected() || connectResult.isLinked()) {
                    if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
                        itemStackInHand.shrink(1);
                    }
                }
            }

            itemStackInHand.set(ModDataComponents.WIRE_CONNECTION.value(), null);
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(connectResult.getMessage(), true);
            }

        } else { // First click
            if (context.getPlayer() == null) return InteractionResult.PASS;

            int index;

            if (isRemover(itemInHand)) {
                if (!clickedIWireNodeBlockEntity.hasAnyConnection()) {
                    context.getPlayer().displayClientMessage(WireConnectResult.NO_CONNECTION.getMessage(), true);
                    return InteractionResult.FAIL;
                }
                // Storing -1 indicates it's the first click of a remove action.
                index = -1;
                context.getPlayer().displayClientMessage(WireConnectResult.getConnect(clickedIWireNodeBlockEntity.isConnectorInput(index), clickedIWireNodeBlockEntity.isConnectorOutput(index)).getMessage(), true);

            } else { // Connecting
                index = clickedIWireNodeBlockEntity.getAvailableNode(context.getClickLocation());
                if (index < 0) {
                    context.getPlayer().displayClientMessage(WireConnectResult.COUNT.getMessage(), true);
                    return InteractionResult.FAIL;
                }
                context.getPlayer().displayClientMessage(WireConnectResult.getConnect(clickedIWireNodeBlockEntity.isConnectorInput(index), clickedIWireNodeBlockEntity.isConnectorOutput(index)).getMessage(), true);
            }

            itemStackInHand.set(ModDataComponents.WIRE_CONNECTION.value(), new WireConnectionData(clickedIWireNodeBlockEntity.getPos(), index));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static boolean isRemover(Item item) {
        return item == ModItems.SPOOL.get();
    }

    /**
     * Retrieves the block position from the given wire connection data.
     *
     * @param wireConnectionData The wire connection data containing the block position.
     * @return The block position, or null if the position is not set.
     */
    public static BlockPos getPos(WireConnectionData wireConnectionData) {
        if (wireConnectionData.pos() == null) {
            return null;
        }
        return wireConnectionData.pos();
    }
    /**
     * Retrieves the node index from the given wire connection data.
     *
     * @param wireConnectionData The wire connection data containing the node index.
     * @return The node index, or -1 if the node is not set.
     */
    public static int getNode(WireConnectionData wireConnectionData) {
        if (wireConnectionData.node() == -1) {
            return -1;
        }
        return wireConnectionData.node();
    }
}
