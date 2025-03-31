package com.teamofelectrorealism.electrorealism.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.item.ModItems;
import com.teamofelectrorealism.electrorealism.network.NetworkManager;
import com.teamofelectrorealism.electrorealism.rendering.HighlightNetworks;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (!mc.player.getMainHandItem().getItem().equals(ModItems.TEST_ITEM.get())) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        DeltaTracker partialTick = event.getPartialTick();

        // Get the MultiBufferSource from the Minecraft instance.
        MultiBufferSource buffer = mc.renderBuffers().bufferSource();

        NetworkManager networkManager = ElectroRealism.NETWORK_MANAGER;

        HighlightNetworks.highlightNetwork(networkManager.getNetworksDataForClient());
        HighlightNetworks.renderHighlights(poseStack, buffer, partialTick);
    }
}
