package com.teamofelectrorealism.electrorealism.block.connector;

import com.teamofelectrorealism.electrorealism.rendering.WireNodeRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class ConnectorRenderer extends WireNodeRenderer<AbstractConnectorBlockEntity> {
    public ConnectorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }
}
