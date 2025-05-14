package com.teamofpowersim.powersim.item;

import com.teamofpowersim.powersim.PowerSim;
import com.teamofpowersim.powersim.network.INetworkMember;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.UUID;

public class TestItem extends Item {
    public TestItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack itemStackInHand = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        BlockEntity clickedBlockEntity = context.getLevel().getBlockEntity(clickedPos);

        if (clickedBlockEntity instanceof INetworkMember networkMember && !context.getLevel().isClientSide()) {
            UUID networkId = networkMember.getNetworkId();
            PowerSim.NETWORK_MANAGER.getNetworkIds();
            PowerSim.NETWORK_MANAGER.printMembersInNetwork(networkId);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
