package com.teamofelectrorealism.electrorealism.item;

import com.teamofelectrorealism.electrorealism.datacomponents.ModDataComponents;
import com.teamofelectrorealism.electrorealism.datacomponents.WireConnectionData;
import com.teamofelectrorealism.electrorealism.network.Connection;
import com.teamofelectrorealism.electrorealism.network.NetworkManager;
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
        ItemStack itemStackInHand = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        BlockEntity clickedBlockEntity = context.getLevel().getBlockEntity(clickedPos);

        WireConnectionData connectionData = itemStackInHand.get(ModDataComponents.WIRE_CONNECTION.value());

        if (!(clickedBlockEntity instanceof IWireNode clickedIWireNodeBlockEntity)) {
            return InteractionResult.PASS;
        }
        Item itemInHand = itemStackInHand.getItem();

        if (connectionData != null && connectionData.pos() != null) { // second click
            WireConnectResult connectResult;
            BlockPos targetPos = connectionData.pos();
            BlockEntity targetBlockEntity = context.getLevel().getBlockEntity(targetPos);

            if(isRemover(itemInHand)) {
                connectResult = IWireNode.disconnect(context.getLevel(), clickedPos, targetPos);
            } else {
                connectResult = IWireNode.connect(context.getLevel(), getPos(connectionData), getNode(connectionData), clickedPos, clickedIWireNodeBlockEntity.getAvailableNode(context.getClickLocation()), WireType.of(itemInHand));

                if (!NetworkManager.instances.containsKey(context.getLevel())) {
                    System.out.println("Called");
                    new NetworkManager(context.getLevel());
                }
                NetworkManager.instances.get(context.getLevel()).createConnection(
                        clickedIWireNodeBlockEntity.getLocalNode(connectionData.node()),
                        ((IWireNode) targetBlockEntity).getLocalNode(0)
                );
            }

            clickedBlockEntity.setChanged();

            WireType connectionType = IWireNode.getTypeOfConnection(context.getLevel(), clickedPos, getPos(connectionData));
            if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
                if (connectResult == WireConnectResult.REMOVED) {
                    itemStackInHand.shrink(1);
                    ItemStack stack = connectionType.getSourceDrop();
                    boolean shouldDrop = !context.getPlayer().addItem(stack);
                    if (shouldDrop) context.getPlayer().drop(stack, false);
                }
            }

            itemStackInHand.set(ModDataComponents.WIRE_CONNECTION.value(), null);
            context.getPlayer().displayClientMessage(connectResult.getMessage(), true);
        }

        else { // First click
            if (context.getPlayer() == null) return InteractionResult.PASS;
            if (isRemover(itemInHand)) {
                if (!clickedIWireNodeBlockEntity.hasAnyConnection()) {
                    context.getPlayer().displayClientMessage(WireConnectResult.NO_CONNECTION.getMessage(), true);
                    return InteractionResult.CONSUME;
                }
            }
            int index = clickedIWireNodeBlockEntity.getAvailableNode(context.getClickLocation());
            if (index < 0) {
                return InteractionResult.PASS;
            }
            if (!isRemover(itemInHand)) {
                context.getPlayer().displayClientMessage(WireConnectResult.getConnect(clickedIWireNodeBlockEntity.isNodeInput(index), clickedIWireNodeBlockEntity.isNodeOutput(index)).getMessage(), true);
            }
            itemStackInHand.set(ModDataComponents.WIRE_CONNECTION.value(), new WireConnectionData(clickedIWireNodeBlockEntity.getPos(), index)); // observerPacket? todo
        }
        return InteractionResult.CONSUME;
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
