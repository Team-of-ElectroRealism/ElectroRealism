package com.teamofelectrorealism.electrorealism.item;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.power.IWireNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

public class TestItem extends Item {
    public TestItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack itemStackInHand = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        BlockEntity clickedBlockEntity = context.getLevel().getBlockEntity(clickedPos);

        if (clickedBlockEntity instanceof IWireNode) {
            ElectroRealism.NETWORK_MANAGER.removeAllNetworks();
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
