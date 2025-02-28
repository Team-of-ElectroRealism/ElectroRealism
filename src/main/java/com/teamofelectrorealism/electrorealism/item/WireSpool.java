package com.teamofelectrorealism.electrorealism.item;

import com.teamofelectrorealism.electrorealism.power.IWireNode;
import com.teamofelectrorealism.electrorealism.power.WireConnectResult;
import com.teamofelectrorealism.electrorealism.power.WireType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.stream.Stream;
//todo
public class WireSpool extends Item {
    public WireSpool(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack itemStack = context.getItemInHand();
        Stream<TagKey<Item>> tagStream = itemStack.getTags();

        if (tagStream.findAny().isPresent()) {
            // save tag or tags?
        } else {
            // create new tag or tags?
        }

        BlockPos clickedPos = context.getClickedPos();
        BlockEntity blockEntity = context.getLevel().getBlockEntity(clickedPos);
        if (!(blockEntity instanceof IWireNode)) { // will also check if blockEntity is null
            return InteractionResult.PASS;
        }
        IWireNode node = (IWireNode) blockEntity;
        Item heldItem = itemStack.getItem();

        if (true) { // tag or tags includes position
            WireConnectResult connectResult = null;

            // WireType connectionType = IWireNode.getTypeOfConnection(context.getLevel(), clickedPos, getPos(CompoundTag));

            blockEntity.setChanged();

            if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
                if (connectResult == WireConnectResult.REMOVED) {

                }
            }
        }


        return InteractionResult.FAIL; // not done
    }
}
