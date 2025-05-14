package com.teamofpowersim.powersim.block.connector;

import com.teamofpowersim.powersim.rendering.WireNodeRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class ConnectorRenderer extends WireNodeRenderer<AbstractConnectorBlockEntity> {
    public ConnectorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }
}
